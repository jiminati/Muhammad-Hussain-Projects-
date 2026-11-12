package com.vlektra.batterydoctor.report

import com.vlektra.batterydoctor.diagnosis.DiagnosisResult
import com.vlektra.batterydoctor.diagnosis.RuleEngine
import com.vlektra.batterydoctor.diagnosis.Severity
import com.vlektra.batterydoctor.model.BmsParameters
import com.vlektra.batterydoctor.model.HistoryEvent
import com.vlektra.batterydoctor.model.JobCard
import com.vlektra.batterydoctor.model.PackSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Builds the text a technician sends from the field. */
object ReportBuilder {

    private fun now(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK).format(Date())

    private fun dash(label: String, value: String?): String =
        if (value.isNullOrBlank()) "$label: —" else "$label: $value"

    /**
     * The short message a technician sends on WhatsApp from the roadside.
     * Built to be readable on a phone without scrolling sideways.
     */
    fun whatsAppMessage(
        job: JobCard,
        snapshot: PackSnapshot,
        diagnosis: DiagnosisResult
    ): String {
        val b = StringBuilder()
        b.appendLine("*VLK BATTERY DOCTOR*")
        b.appendLine(now())
        b.appendLine()
        b.appendLine("*${diagnosis.worst.verdict.uppercase()}*")
        b.appendLine(diagnosis.headline)
        b.appendLine()
        if (job.city.isNotBlank()) b.appendLine("City: ${job.city}")
        if (job.model.isNotBlank()) b.appendLine("Model: ${job.model}")
        if (job.customerName.isNotBlank()) b.appendLine("Customer: ${job.customerName}")
        if (job.chassisNumber.isNotBlank()) b.appendLine("Chassis: ${job.chassisNumber}")
        b.appendLine("Battery: ${job.batteryId.ifBlank { snapshot.deviceName }}")
        b.appendLine()
        b.appendLine("*Status:* ${job.status.label}")
        b.appendLine()
        b.appendLine(
            "Pack ${"%.1f".format(snapshot.packVoltage)} V · " +
                "SOC ${"%.0f".format(snapshot.socPercent)}% · " +
                "spread ${snapshot.deltaMv} mV · " +
                "${snapshot.cycles} cycles"
        )
        if (snapshot.deltaMv >= 50) {
            b.appendLine(
                "Low cell #${snapshot.lowestCellNumber} at ${snapshot.minCellMv} mV"
            )
        }
        b.appendLine()
        val acts = diagnosis.actionable
        if (acts.isEmpty()) {
            b.appendLine("No action needed.")
        } else {
            b.appendLine("*Action:*")
            acts.forEachIndexed { i, f ->
                b.appendLine("${i + 1}. ${f.actionEnglish}")
            }
        }
        if (job.technician.isNotBlank()) {
            b.appendLine()
            b.appendLine("Checked by ${job.technician}")
        }
        return b.toString().trimEnd()
    }

    /** The full record for the complaint log and the warranty file. */
    fun fullReport(
        job: JobCard,
        snapshot: PackSnapshot,
        history: List<HistoryEvent>,
        parameters: BmsParameters?,
        diagnosis: DiagnosisResult
    ): String {
        val b = StringBuilder()
        val rule = "-".repeat(46)

        b.appendLine("VLK BATTERY DOCTOR — DIAGNOSIS REPORT")
        b.appendLine(rule)
        b.appendLine(dash("Date", now()))
        b.appendLine(dash("Battery ID", job.batteryId.ifBlank { snapshot.deviceName }))
        b.appendLine(dash("Chassis", job.chassisNumber))
        b.appendLine(dash("Model", job.model))
        b.appendLine(dash("Customer", job.customerName))
        b.appendLine(dash("Phone", job.customerPhone))
        b.appendLine(dash("City", job.city))
        b.appendLine(dash("Technician", job.technician))
        b.appendLine(dash("Complaint", job.complaint))
        b.appendLine(dash("Status", job.status.label))
        b.appendLine()

        b.appendLine("VERDICT")
        b.appendLine(rule)
        b.appendLine("${diagnosis.worst.verdict} [${diagnosis.worst.badge}]")
        b.appendLine(diagnosis.headline)
        b.appendLine("Outcome: ${job.status.label}")
        b.appendLine()

        b.appendLine("PACK IDENTITY")
        b.appendLine(rule)
        b.appendLine(dash("BMS name", snapshot.deviceName))
        b.appendLine(dash("Battery code", snapshot.batteryCode))
        b.appendLine(dash("BMS serial", snapshot.bmsSerial))
        b.appendLine(dash("Firmware", snapshot.firmware))
        b.appendLine(dash("Total usage", snapshot.totalUsage))
        b.appendLine()

        b.appendLine("READINGS")
        b.appendLine(rule)
        b.appendLine("Pack voltage      ${"%.2f".format(snapshot.packVoltage)} V")
        b.appendLine("Current           ${"%.1f".format(snapshot.current)} A")
        b.appendLine("SOC (BMS)         ${"%.1f".format(snapshot.socPercent)} %")
        b.appendLine(
            "SOC (from OCV)    ${RuleEngine.socFromMillivolts(snapshot.averageMv).roundToInt()} %"
        )
        b.appendLine("Remaining         ${"%.1f".format(snapshot.remainingAh)} Ah of " +
            "${"%.0f".format(snapshot.ratedAh)} Ah")
        b.appendLine("Cycles            ${snapshot.cycles}")
        b.appendLine("Cell spread       ${snapshot.deltaMv} mV")
        b.appendLine("Lowest cell       #${snapshot.lowestCellNumber} at ${snapshot.minCellMv} mV")
        b.appendLine("Highest cell      #${snapshot.highestCellNumber} at ${snapshot.maxCellMv} mV")
        b.appendLine("Temperatures      ${snapshot.tempsC.joinToString(", ") { "$it" }} °C " +
            "(MOS ${snapshot.mosTempC} °C)")
        b.appendLine()

        b.appendLine("CELL VOLTAGES (mV)")
        b.appendLine(rule)
        snapshot.cellsMv.chunked(6).forEachIndexed { row, chunk ->
            val first = row * 6 + 1
            val labels = chunk.mapIndexed { i, mv ->
                val n = first + i
                val mark = when (n) {
                    snapshot.lowestCellNumber -> "v"
                    snapshot.highestCellNumber -> "^"
                    else -> " "
                }
                "%2d:%d%s".format(n, mv, mark)
            }
            b.appendLine(labels.joinToString("  "))
        }
        b.appendLine()

        b.appendLine("FINDINGS")
        b.appendLine(rule)
        diagnosis.findings.forEachIndexed { i, f ->
            b.appendLine("${i + 1}. [${f.severity.badge}] ${f.title}")
            b.appendLine("   Evidence : ${f.evidence}")
            b.appendLine("   Reason   : ${f.reasoning}")
            b.appendLine()
        }

        b.appendLine("ACTIONS")
        b.appendLine(rule)
        val acts = diagnosis.actionable.ifEmpty { diagnosis.findings }
        acts.forEachIndexed { i, f ->
            b.appendLine("${i + 1}. ${f.actionEnglish}")
            b.appendLine("   ${f.actionUrdu}")
            b.appendLine()
        }

        if (parameters != null) {
            b.appendLine("CONFIGURATION AT TIME OF CHECK")
            b.appendLine(rule)
            b.appendLine("Capacity Learning     ${onOff(parameters.capacityLearning)}")
            b.appendLine("Active Balancing      ${onOff(parameters.activeBalancing)}")
            b.appendLine("Balance start         ${parameters.balanceStartMv} mV " +
                "(diff ${parameters.balanceStartDiffMv} mV)")
            b.appendLine("Cell OV / UV          ${parameters.cellOverVoltageMv} / " +
                "${parameters.cellUnderVoltageMv} mV")
            b.appendLine("Pack OV / UV          ${parameters.packOverVoltageV} / " +
                "${parameters.packUnderVoltageV} V")
            b.appendLine("Charge / discharge OC ${parameters.chargeOverCurrentA} / " +
                "${parameters.dischargeOverCurrentA} A")
            b.appendLine("Short circuit trigger ${parameters.shortCircuitTriggerA} A")
            b.appendLine("NTC disc. protection  " +
                "${if (parameters.ntcDisconnectProtectionDisabled) "DISABLED" else "enabled"}")
            b.appendLine()
        }

        if (history.isNotEmpty()) {
            b.appendLine("HISTORY LOG (${history.size} entries, newest first)")
            b.appendLine(rule)
            history.take(40).forEach {
                b.appendLine("${it.timestamp}  ${it.name}")
            }
            if (history.size > 40) b.appendLine("... ${history.size - 40} more")
            b.appendLine()
        }

        b.appendLine(rule)
        b.appendLine("VLK Battery Doctor · Vlektra R&D · developed by Sen")
        return b.toString().trimEnd()
    }

    private fun onOff(v: Boolean) = if (v) "ON" else "OFF"

    /** Filename for the shared report. */
    fun fileName(job: JobCard, snapshot: PackSnapshot): String {
        val id = job.chassisNumber.ifBlank { job.batteryId.ifBlank { snapshot.deviceName } }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.UK).format(Date())
        val safe = id.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return "VLK_${safe}_$stamp.txt"
    }

    /** Short one-line summary for a list row. */
    fun oneLine(diagnosis: DiagnosisResult, snapshot: PackSnapshot): String =
        "${diagnosis.worst.verdict} · ${snapshot.deviceName} · spread ${snapshot.deltaMv} mV"

    /** Colour-free severity word for anywhere colour cannot be used. */
    fun severityWord(s: Severity): String = s.badge
}
