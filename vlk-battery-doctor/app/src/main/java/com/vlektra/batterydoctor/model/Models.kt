package com.vlektra.batterydoctor.model

/** One reading of everything the BMS reports right now. */
data class PackSnapshot(
    val deviceName: String,
    val macAddress: String,
    val batteryCode: String,
    val bmsSerial: String,
    val firmware: String,
    val packVoltage: Double,        // V
    val current: Double,            // A, positive = charging
    val socPercent: Double,
    val remainingAh: Double,
    val ratedAh: Double,
    val cycles: Int,
    val cellsMv: List<Int>,         // one entry per series cell
    val tempsC: List<Int>,          // NTC sensors
    val mosTempC: Int,
    val chargeMosOn: Boolean,
    val dischargeMosOn: Boolean,
    val balancingActive: Boolean,
    val totalUsage: String
) {
    val cellCount: Int get() = cellsMv.size
    val minCellMv: Int get() = cellsMv.minOrNull() ?: 0
    val maxCellMv: Int get() = cellsMv.maxOrNull() ?: 0
    val deltaMv: Int get() = maxCellMv - minCellMv
    val lowestCellNumber: Int get() = cellsMv.indexOf(minCellMv) + 1
    val highestCellNumber: Int get() = cellsMv.indexOf(maxCellMv) + 1
    val averageMv: Double get() = if (cellsMv.isEmpty()) 0.0 else cellsMv.average()

    val medianMv: Double
        get() {
            if (cellsMv.isEmpty()) return 0.0
            val s = cellsMv.sorted()
            val m = s.size / 2
            return if (s.size % 2 == 0) (s[m - 1] + s[m]) / 2.0 else s[m].toDouble()
        }

    /** True when the pack is being charged or discharged hard enough to reveal a weak cell. */
    val underLoad: Boolean get() = kotlin.math.abs(current) >= 2.0

    val maxTempC: Int get() = tempsC.maxOrNull() ?: 0
    val minTempC: Int get() = tempsC.minOrNull() ?: 0
    val tempSpreadC: Int get() = maxTempC - minTempC
}

/** One line of the BMS history log. */
data class HistoryEvent(
    val name: String,
    val timestamp: String,
    val level: Int = 0,
    val category: EventCategory = EventCategory.OTHER
)

enum class EventCategory {
    POWER,              // Power On, BMS Sleep — normal housekeeping
    CELL_UNDERVOLTAGE,
    CELL_OVERVOLTAGE,
    VOLTAGE_SENSE,      // Voltage Collect Faulty, Voltage Line Offline
    TEMP_SENSE,         // Temp Collect Faulty
    TEMP_DIFFERENCE,    // ErrDeltaT
    SHORT_CIRCUIT,
    OVERCURRENT,
    CHARGE_TEMP_HIGH,
    CHARGE_TEMP_LOW,
    SOC_LOW,
    OTHER
}

/** The settable BMS parameters the app reads and (in the engineer tier) writes. */
data class BmsParameters(
    val capacityLearning: Boolean,
    val activeBalancing: Boolean,
    val balanceStartMv: Int,
    val balanceStopMv: Int,
    val balanceStartDiffMv: Int,
    val balanceCurrentA: Double,
    val cellOverVoltageMv: Int,
    val cellUnderVoltageMv: Int,
    val packOverVoltageV: Double,
    val packUnderVoltageV: Double,
    val voltageDiffProtectMv: Int,
    val chargeOverCurrentA: Double,
    val dischargeOverCurrentA: Double,
    val shortCircuitTriggerA: Int,
    val chargeHighTempC: Int,
    val chargeLowTempC: Int,
    val dischargeHighTempC: Int,
    val dischargeLowTempC: Int,
    val tempDifferenceC: Int,
    val ntcDisconnectProtectionDisabled: Boolean,
    val soc0Mv: Int,
    val soc100Mv: Int,
    val ratedCapacityAh: Double,
    val cellStrings: Int,
    val sleepTimeS: Int
)

/** Which tier of user may change a given parameter. */
enum class AccessTier { TECHNICIAN, ENGINEER, LOCKED }

/** Identity of the pack and the job, used on the report. */
data class JobCard(
    val batteryId: String = "",
    val chassisNumber: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val model: String = "",
    val city: String = "",
    val technician: String = "",
    val complaint: String = ""
)

/** A pack found over Bluetooth. */
data class DiscoveredPack(
    val name: String,
    val address: String,
    val rssi: Int,
    val knownChassis: String? = null,
    val knownBatch: String? = null
)
