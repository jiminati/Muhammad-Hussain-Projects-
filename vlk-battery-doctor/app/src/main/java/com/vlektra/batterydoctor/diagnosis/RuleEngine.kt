package com.vlektra.batterydoctor.diagnosis

import com.vlektra.batterydoctor.model.BmsParameters
import com.vlektra.batterydoctor.model.EventCategory
import com.vlektra.batterydoctor.model.HistoryEvent
import com.vlektra.batterydoctor.model.PackSnapshot
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Turns BMS data into a verdict and a list of actions.
 *
 * Every threshold here comes from Vlektra field experience on 24S LFP packs
 * with DALY JHB-R24TK boards. They are gathered in [Thresholds] so they can
 * be corrected in one place as more field data arrives.
 */
object RuleEngine {

    object Thresholds {
        /** A cell this far below the pack median has lost its connection, not its charge. */
        const val COLLAPSE_MV = 300

        /** Cell positions where Batch 6 packs break their busbars. */
        val BUSBAR_CELLS = setOf(7, 18)

        /** Spread above this will not come back with balancing alone. */
        const val IMBALANCE_SERIOUS_MV = 100

        /** Spread above this is worth a second look next service. */
        const val IMBALANCE_WATCH_MV = 50

        /** Below this per cell, LFP starts taking permanent damage. */
        const val DEEP_DISCHARGE_MV = 2800

        /** Repeated sampling faults mean the harness, not a loose plug. */
        const val SENSE_FAULTS_SYSTEMIC = 3

        /** This many under-voltage cut-offs is a usage pattern, not an incident. */
        const val UNDERVOLTAGE_HABITUAL = 10

        /** SOC disagreeing with the cell voltages by more than this is a lost gauge. */
        const val SOC_MISMATCH_PCT = 25.0

        /** Balancing start voltage Vlektra standardised on. */
        const val VLK_BALANCE_START_MV = 2900
    }

    /**
     * Resting open-circuit voltage to state of charge for one LFP cell.
     *
     * The curve is deliberately flat between roughly 20% and 80% because LFP
     * chemistry is flat there. This is used only to catch a gauge that has
     * lost its reference entirely, never to quote a precise SOC.
     */
    private val OCV_CURVE = listOf(
        2700 to 0.0, 2900 to 2.0, 3000 to 5.0, 3100 to 10.0,
        3200 to 20.0, 3250 to 30.0, 3280 to 40.0, 3300 to 50.0,
        3320 to 60.0, 3330 to 68.0, 3340 to 80.0, 3350 to 90.0,
        3400 to 97.0, 3450 to 100.0
    )

    fun socFromMillivolts(mv: Double): Double {
        if (mv <= OCV_CURVE.first().first) return 0.0
        if (mv >= OCV_CURVE.last().first) return 100.0
        for (i in 1 until OCV_CURVE.size) {
            val (hiMv, hiSoc) = OCV_CURVE[i]
            if (mv <= hiMv) {
                val (loMv, loSoc) = OCV_CURVE[i - 1]
                return loSoc + (mv - loMv) * (hiSoc - loSoc) / (hiMv - loMv)
            }
        }
        return 100.0
    }

    /**
     * @param batchHint "Batch 6", "Batch 7" or null when the batch is unknown.
     * @param rangeComplaint true when the customer brought the bike in for low range.
     */
    fun diagnose(
        snapshot: PackSnapshot,
        history: List<HistoryEvent>,
        parameters: BmsParameters?,
        batchHint: String? = null,
        rangeComplaint: Boolean = false
    ): DiagnosisResult {

        val findings = mutableListOf<Finding>()
        val isBatch6 = batchHint?.contains("6") == true

        findings += cellFindings(snapshot, isBatch6)
        findings += socFinding(snapshot)
        findings += historyFindings(history)
        findings += parameterFindings(parameters)

        var worst = findings.maxByOrNull { it.severity.rank }?.severity ?: Severity.OK

        if (rangeComplaint && worst == Severity.OK) {
            findings += Finding(
                severity = Severity.OK,
                title = "Low range complaint, no fault found",
                evidence = "Cells balanced, history clean, configuration correct",
                reasoning = "On a healthy pack a range complaint is almost always " +
                    "charging habit, load or tyre pressure rather than the battery.",
                actionUrdu = "Battery theek hai. Customer ko samjhayein: range 100% se 0% " +
                    "tak naapi jaati hai. Agar woh 30% par charge laga dete hain to range " +
                    "kam lagegi. Tyre pressure aur weight bhi check karein.",
                actionEnglish = "Pack is healthy. Explain that quoted range is measured " +
                    "100% to 0%; also check tyre pressure and load."
            )
            worst = findings.maxByOrNull { it.severity.rank }?.severity ?: Severity.OK
        }

        val sorted = findings.sortedByDescending { it.severity.rank }
        return DiagnosisResult(
            worst = worst,
            findings = sorted,
            headline = sorted.firstOrNull { it.severity == worst }?.title ?: "No faults found"
        )
    }

    // ------------------------------------------------------------------
    // Cells
    // ------------------------------------------------------------------

    private fun cellFindings(s: PackSnapshot, isBatch6: Boolean): List<Finding> {
        if (s.cellsMv.isEmpty()) return emptyList()
        val out = mutableListOf<Finding>()

        val sag = (s.medianMv - s.minCellMv).roundToInt()
        val low = s.lowestCellNumber

        when {
            sag >= Thresholds.COLLAPSE_MV -> {
                val busbar = low in Thresholds.BUSBAR_CELLS || isBatch6
                if (busbar) {
                    out += Finding(
                        severity = Severity.ENGINEER,
                        title = "Broken busbar suspected at cell $low",
                        evidence = "Cell $low at ${s.minCellMv} mV, $sag mV below the pack " +
                            "median of ${s.medianMv.roundToInt()} mV",
                        reasoning = "A collapse this large at cell 7 or 18 on a Batch 6 pack " +
                            "is the busbar-break signature, not a weak cell. The cell is " +
                            "healthy; its connection is not.",
                        actionUrdu = "Battery repair line par bhejein. Cell $low ka busbar tuta " +
                            "hua lag raha hai — laser weld se theek hoga. Warranty claim ke liye " +
                            "photo aur ye report save karein.",
                        actionEnglish = "Send to the busbar repair line. Flag as a warranty " +
                            "candidate and attach this report."
                    )
                } else {
                    out += Finding(
                        severity = Severity.WORKSHOP,
                        title = "Cell $low has collapsed",
                        evidence = "Cell $low at ${s.minCellMv} mV, $sag mV below the median",
                        reasoning = "A single cell this far below the rest will not recover " +
                            "by balancing. Either the connection is bad or the cell is finished.",
                        actionUrdu = "Battery workshop bhejein. Cell $low bohot neeche hai — " +
                            "pehle connection aur busbar check karein, agar theek hai to cell " +
                            "replace karni hogi.",
                        actionEnglish = "Workshop: check the connection at cell $low first, " +
                            "then plan a cell replacement."
                    )
                }
            }

            s.deltaMv >= Thresholds.IMBALANCE_SERIOUS_MV -> {
                out += Finding(
                    severity = Severity.WORKSHOP,
                    title = "Cells out of balance",
                    evidence = "Spread ${s.deltaMv} mV — cell $low at ${s.minCellMv} mV, " +
                        "cell ${s.highestCellNumber} at ${s.maxCellMv} mV",
                    reasoning = if (s.underLoad)
                        "Measured under ${abs(s.current).roundToInt()} A of load, which is " +
                            "where real imbalance shows up."
                    else
                        "Measured at rest. A pack that looks acceptable at rest can still " +
                            "spread badly under load, so this may be understated.",
                    actionUrdu = "Raat bhar active balancing par lagayein (Balancing → Active " +
                        "Balancing ON, start voltage ${Thresholds.VLK_BALANCE_START_MV} mV). " +
                        "Subah dobara check karein. Agar farq phir bhi " +
                        "${Thresholds.IMBALANCE_SERIOUS_MV} mV se zyada hai to battery replace " +
                        "karni hogi.",
                    actionEnglish = "Overnight active balancing at " +
                        "${Thresholds.VLK_BALANCE_START_MV} mV, then re-read. Still above " +
                        "${Thresholds.IMBALANCE_SERIOUS_MV} mV means replacement."
                )
            }

            s.deltaMv >= Thresholds.IMBALANCE_WATCH_MV -> {
                out += Finding(
                    severity = Severity.WATCH,
                    title = "Small imbalance building",
                    evidence = "Spread ${s.deltaMv} mV, lowest is cell $low",
                    reasoning = "Still inside the safe range, but worth a second reading " +
                        "at the next service to see whether it is growing.",
                    actionUrdu = "Abhi koi kaam nahi. Agle service par cell $low dobara check karein.",
                    actionEnglish = "No action now. Re-read cell $low at the next service."
                )
            }

            else -> {
                out += Finding(
                    severity = Severity.OK,
                    title = "Cells are well balanced",
                    evidence = "Spread only ${s.deltaMv} mV across ${s.cellCount} cells",
                    reasoning = "Nothing wrong with the cell group at this state of charge.",
                    actionUrdu = "Cells theek hain.",
                    actionEnglish = "Cell group is healthy."
                )
            }
        }

        if (s.minCellMv < Thresholds.DEEP_DISCHARGE_MV) {
            out += Finding(
                severity = Severity.ENGINEER,
                title = "Pack is deeply discharged",
                evidence = "Lowest cell ${s.minCellMv} mV, below the " +
                    "${Thresholds.DEEP_DISCHARGE_MV} mV floor",
                reasoning = "Below 2.8 V an LFP cell begins taking permanent damage, and a " +
                    "normal-rate charge makes it worse.",
                actionUrdu = "Battery ko abhi normal charge mat karein. Engineer ko batayein — " +
                    "pehle kam current par slow charge chahiye hoga.",
                actionEnglish = "Do not charge at normal rate. Engineer to supervise a " +
                    "low-current recovery charge."
            )
        }

        if (s.tempSpreadC >= 15) {
            out += Finding(
                severity = Severity.WORKSHOP,
                title = "Temperature spread across the pack",
                evidence = "Sensors read ${s.tempsC.joinToString(", ") { "$it °C" }} — " +
                    "spread ${s.tempSpreadC} °C",
                reasoning = "A real ${s.tempSpreadC} °C spread means uneven current sharing; " +
                    "a false one means a sensor has come loose.",
                actionUrdu = "Chaaron NTC sensor check karein — koi dheela ya tuta to nahi. " +
                    "Agar sensor theek hain to current sharing ka masla hai.",
                actionEnglish = "Check all four NTCs for a loose sensor. If they are sound, " +
                    "investigate uneven current sharing."
            )
        }

        return out
    }

    // ------------------------------------------------------------------
    // State of charge sanity
    // ------------------------------------------------------------------

    private fun socFinding(s: PackSnapshot): List<Finding> {
        if (s.cellsMv.isEmpty() || s.underLoad) return emptyList()

        val estimated = socFromMillivolts(s.averageMv)
        if (abs(estimated - s.socPercent) < Thresholds.SOC_MISMATCH_PCT) return emptyList()

        return listOf(
            Finding(
                severity = Severity.WATCH,
                title = "SOC reading does not match the cell voltages",
                evidence = "BMS reports ${"%.1f".format(s.socPercent)}% at " +
                    "${"%.1f".format(s.packVoltage)} V, but ${s.averageMv.roundToInt()} mV per " +
                    "cell works out to roughly ${estimated.roundToInt()}%",
                reasoning = "The gauge has lost its reference point. The pack itself holds " +
                    "charge; only the percentage is wrong, which is what the customer sees.",
                actionUrdu = "Battery ko 100% tak full charge karein, phir pura chalayein. BMS " +
                    "khud SOC theek kar lega. Capacity Learning OFF rehni chahiye.",
                actionEnglish = "Full charge to 100%, then one full run. The BMS re-references " +
                    "itself. Keep Capacity Learning off."
            )
        )
    }

    // ------------------------------------------------------------------
    // History log
    // ------------------------------------------------------------------

    private fun historyFindings(history: List<HistoryEvent>): List<Finding> {
        if (history.isEmpty()) return emptyList()
        val out = mutableListOf<Finding>()

        fun count(c: EventCategory) = history.count { it.category == c }
        fun latest(c: EventCategory) = history.firstOrNull { it.category == c }?.timestamp

        val shorts = count(EventCategory.SHORT_CIRCUIT)
        if (shorts > 0) {
            out += Finding(
                severity = Severity.ENGINEER,
                title = "Short circuit protection has fired",
                evidence = "$shorts event(s)" + (latest(EventCategory.SHORT_CIRCUIT)
                    ?.let { ", most recent $it" } ?: ""),
                reasoning = "The BMS saw current above its short-circuit trigger. Either the " +
                    "wiring is chafing somewhere or the load is faulty. This is not something " +
                    "to hand back without finding the cause.",
                actionUrdu = "Battery khud na kholein. Engineer ko bhejein — cable, XT90 " +
                    "connector aur controller wiring check karni hai.",
                actionEnglish = "Engineer to inspect cabling, the XT90 connector and controller " +
                    "wiring before this pack goes back out."
            )
        }

        val sense = count(EventCategory.VOLTAGE_SENSE)
        when {
            sense >= Thresholds.SENSE_FAULTS_SYSTEMIC -> out += Finding(
                severity = Severity.ENGINEER,
                title = "Voltage sense harness is failing",
                evidence = "$sense voltage sampling faults in the log" +
                    (latest(EventCategory.VOLTAGE_SENSE)?.let { ", most recent $it" } ?: ""),
                reasoning = "Repeated sampling faults point at the sense wires or the BMS " +
                    "front end, not at the cells. A pack can read perfectly balanced and " +
                    "still be unsafe to return with this fault present.",
                actionUrdu = "Engineer ko bhejein — sense wire harness ya BMS board ka masla " +
                    "hai. Cells theek ho sakti hain lekin battery wapas na dein.",
                actionEnglish = "Engineer: inspect the sense harness and BMS front end. The " +
                    "cells may be fine; the measurement path is not."
            )

            sense > 0 -> out += Finding(
                severity = Severity.WORKSHOP,
                title = "Voltage sense fault logged",
                evidence = "$sense event(s)" +
                    (latest(EventCategory.VOLTAGE_SENSE)?.let { ", at $it" } ?: ""),
                reasoning = "A single event can be a loose connector rather than a failing " +
                    "harness, but it should still be looked at.",
                actionUrdu = "Sense wire connector BMS par dobara theek se lagayein aur " +
                    "connector pins check karein.",
                actionEnglish = "Re-seat the sense connector at the BMS and inspect the pins."
            )
        }

        val tempSense = count(EventCategory.TEMP_SENSE)
        val deltaT = count(EventCategory.TEMP_DIFFERENCE)
        if (tempSense > 0 || deltaT > 0) {
            out += Finding(
                severity = Severity.WORKSHOP,
                title = "Temperature sensor trouble",
                evidence = "$tempSense temperature sampling fault(s), $deltaT temperature " +
                    "difference alarm(s)",
                reasoning = "An NTC is reading wrong or has come loose. On these boards NTC " +
                    "disconnection protection is disabled, so a dead sensor will not stop the " +
                    "pack — it just stops protecting it.",
                actionUrdu = "Chaaron NTC sensor check karein — koi dheela ya tuta to nahi. " +
                    "Cell par sahi tarah paste/tape karein.",
                actionEnglish = "Check all four NTCs for a loose or broken sensor and re-fix " +
                    "them to the cells."
            )
        }

        val deep = count(EventCategory.CELL_UNDERVOLTAGE)
        when {
            deep >= Thresholds.UNDERVOLTAGE_HABITUAL -> out += Finding(
                severity = Severity.WORKSHOP,
                title = "Pack has been run flat repeatedly",
                evidence = "$deep cell under-voltage events in the log",
                reasoning = "Repeated level-3 cut-offs wear the weakest cell faster than the " +
                    "rest, which is how an even pack turns into an uneven one.",
                actionUrdu = "Customer ko batayein ke battery bilkul zero tak na chalayein — " +
                    "15-20% par charge laga dein.",
                actionEnglish = "Advise the customer to recharge at 15-20% rather than running " +
                    "to cut-off."
            )

            deep > 0 -> out += Finding(
                severity = Severity.WATCH,
                title = "Some under-voltage cut-offs logged",
                evidence = "$deep event(s)",
                reasoning = "Normal if the bike was run to empty a few times.",
                actionUrdu = "Koi kaam nahi. Customer ko normal charging habit samjha dein.",
                actionEnglish = "No action. Mention normal charging habits to the customer."
            )
        }

        if (count(EventCategory.CHARGE_TEMP_HIGH) > 0) {
            out += Finding(
                severity = Severity.WATCH,
                title = "Charged while hot",
                evidence = "${count(EventCategory.CHARGE_TEMP_HIGH)} charge " +
                    "high-temperature event(s)",
                reasoning = "Charging above the high-temperature limit trips protection and " +
                    "shortens cell life.",
                actionUrdu = "Customer ko batayein: dhoop mein ya chalne ke foran baad charge " +
                    "na karein. Thandi jagah par 30 minute rakh kar charge karein.",
                actionEnglish = "Advise: let the pack cool for 30 minutes before charging, and " +
                    "charge out of direct sun."
            )
        }

        if (count(EventCategory.CHARGE_TEMP_LOW) > 0) {
            out += Finding(
                severity = Severity.WATCH,
                title = "Charged in the cold",
                evidence = "${count(EventCategory.CHARGE_TEMP_LOW)} charge " +
                    "low-temperature event(s)",
                reasoning = "Charging LFP below 0 °C plates lithium and causes permanent " +
                    "capacity loss.",
                actionUrdu = "Sardiyon mein battery ko kamre ke andar laa kar charge karein.",
                actionEnglish = "Advise indoor charging in winter."
            )
        }

        if (count(EventCategory.OVERCURRENT) > 0) {
            out += Finding(
                severity = Severity.WORKSHOP,
                title = "Overcurrent protection has tripped",
                evidence = "${count(EventCategory.OVERCURRENT)} event(s)",
                reasoning = "Either the rider is loading the bike beyond its rating or the " +
                    "controller is drawing more than it should.",
                actionUrdu = "Controller aur motor wiring check karein. Customer se poochein " +
                    "kitna weight ya load uthate hain.",
                actionEnglish = "Check controller and motor wiring; ask the customer about load."
            )
        }

        return out
    }

    // ------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------

    private fun parameterFindings(p: BmsParameters?): List<Finding> {
        if (p == null) return emptyList()
        val out = mutableListOf<Finding>()

        if (p.capacityLearning) {
            out += Finding(
                severity = Severity.WORKSHOP,
                title = "Capacity Learning is switched on",
                evidence = "Basic Settings → Capacity Learning Switch = ON",
                reasoning = "This is the single most common cause of false low-range " +
                    "complaints on Vlektra packs. It shrinks the usable window the BMS " +
                    "reports without anything being wrong with the cells.",
                actionUrdu = "Settings → Basic Settings → Capacity Learning Switch → OFF " +
                    "karein. Phir battery ek baar full charge karein.",
                actionEnglish = "Turn Capacity Learning off, then do one full charge."
            )
        }

        if (!p.activeBalancing) {
            out += Finding(
                severity = Severity.WORKSHOP,
                title = "Active balancing is switched off",
                evidence = "Balancing → Active Balancing Switch = OFF",
                reasoning = "With balancing off the weakest cell drifts further every cycle, " +
                    "which ends as a range complaint a few months later.",
                actionUrdu = "Settings → Balancing → Active Balancing Switch → ON karein, " +
                    "start voltage ${Thresholds.VLK_BALANCE_START_MV} mV. Phir raat bhar " +
                    "balance hone dein.",
                actionEnglish = "Enable active balancing at ${Thresholds.VLK_BALANCE_START_MV} " +
                    "mV and let it run overnight."
            )
        }

        if (p.ntcDisconnectProtectionDisabled) {
            out += Finding(
                severity = Severity.WATCH,
                title = "NTC disconnection protection is disabled",
                evidence = "Temperature → Disable NTC Disconnection Protection = ON",
                reasoning = "A failed temperature sensor will not stop the pack. That is a " +
                    "deliberate setting on these boards, but it means temperature faults in " +
                    "the log deserve more attention, not less.",
                actionUrdu = "Ye setting jaan boojh kar lagayi gayi hai. NTC sensor manually " +
                    "check karte rahein.",
                actionEnglish = "Intended setting — but check NTCs manually at every service."
            )
        }

        return out
    }
}
