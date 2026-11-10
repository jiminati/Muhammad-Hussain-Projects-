package com.vlektra.batterydoctor.bms

import com.vlektra.batterydoctor.model.BmsParameters
import com.vlektra.batterydoctor.model.DiscoveredPack
import com.vlektra.batterydoctor.model.EventCategory
import com.vlektra.batterydoctor.model.FaultCatalog
import com.vlektra.batterydoctor.model.HistoryEvent
import com.vlektra.batterydoctor.model.PackSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A stand-in for the real BMS, carrying three packs recorded from Vlektra's
 * own fleet. It exists so the whole app — screens, rules, reports — can be
 * built, demonstrated and tested before the Bluetooth protocol is confirmed,
 * and so the rules can be regression-tested against known packs afterwards.
 */
class SimulatedBms : BmsSource {

    override val sourceLabel = "Demo packs (no Bluetooth)"
    override val isLive = false

    private var connectedTo: String? = null

    override fun scan(): Flow<List<DiscoveredPack>> = flow {
        emit(emptyList())
        delay(400)
        emit(packs.take(1).map { it.discovered })
        delay(500)
        emit(packs.take(2).map { it.discovered })
        delay(600)
        emit(packs.map { it.discovered })
    }

    override suspend fun connect(address: String): Result<Unit> {
        delay(700)
        val found = packs.any { it.discovered.address == address }
        return if (found) {
            connectedTo = address
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("No demo pack with address $address"))
        }
    }

    override suspend fun disconnect() {
        connectedTo = null
    }

    private fun current(): DemoPack? = packs.firstOrNull { it.discovered.address == connectedTo }

    override suspend fun readSnapshot(): Result<PackSnapshot> {
        delay(500)
        val p = current() ?: return Result.failure(IllegalStateException("Not connected"))
        return Result.success(p.snapshot)
    }

    override suspend fun readHistory(): Result<List<HistoryEvent>> {
        delay(900)
        val p = current() ?: return Result.failure(IllegalStateException("Not connected"))
        return Result.success(p.history)
    }

    override suspend fun readParameters(): Result<BmsParameters> {
        delay(600)
        val p = current() ?: return Result.failure(IllegalStateException("Not connected"))
        return Result.success(p.parameters)
    }

    override suspend fun writeParameter(change: ParameterChange): Result<Unit> {
        delay(400)
        return Result.failure(
            ProtocolNotAvailable(
                "Demo mode does not write to a battery. Connect a real pack to apply changes."
            )
        )
    }

    private data class DemoPack(
        val discovered: DiscoveredPack,
        val snapshot: PackSnapshot,
        val history: List<HistoryEvent>,
        val parameters: BmsParameters
    )

    private fun ev(name: String, ts: String) = HistoryEvent(
        name = name,
        timestamp = ts,
        level = FaultCatalog.levelOf(name),
        category = FaultCatalog.categorise(name)
    )

    /** Parameters as actually set on Vlektra's 6th/7th batch JHB-R24TK boards. */
    private val vlkStandardParams = BmsParameters(
        capacityLearning = false,
        activeBalancing = true,
        balanceStartMv = 3350,
        balanceStopMv = 3800,
        balanceStartDiffMv = 20,
        balanceCurrentA = 1.0,
        cellOverVoltageMv = 3600,
        cellUnderVoltageMv = 2800,
        packOverVoltageV = 86.4,
        packUnderVoltageV = 67.2,
        voltageDiffProtectMv = 800,
        chargeOverCurrentA = 150.0,
        dischargeOverCurrentA = 150.0,
        shortCircuitTriggerA = 240,
        chargeHighTempC = 55,
        chargeLowTempC = -10,
        dischargeHighTempC = 60,
        dischargeLowTempC = -20,
        tempDifferenceC = 15,
        ntcDisconnectProtectionDisabled = true,
        soc0Mv = 2700,
        soc100Mv = 3445,
        ratedCapacityAh = 36.0,
        cellStrings = 24,
        sleepTimeS = 3600
    )

    private val packs: List<DemoPack> = listOf(

        // ------------------------------------------------------------------
        // 1. Sense-harness pack. Real readings from JHB-D219120135F7:
        //    cells near-perfectly balanced, yet SOC stuck at 0% at 80.0 V,
        //    with repeated voltage and temperature sampling faults.
        // ------------------------------------------------------------------
        DemoPack(
            discovered = DiscoveredPack(
                name = "JHB-D219120135F7",
                address = "D2:19:12:01:35:F7",
                rssi = -58,
                knownChassis = null,
                knownBatch = "Unknown"
            ),
            snapshot = PackSnapshot(
                deviceName = "JHB-D219120135F7",
                macAddress = "D2:19:12:01:35:F7",
                batteryCode = "R24TK1A-24S80A",
                bmsSerial = "224KL160600127",
                firmware = "70_260128_MS50 / jhbS_70_250827",
                packVoltage = 80.0,
                current = 0.0,
                socPercent = 0.0,
                remainingAh = 0.0,
                ratedAh = 36.0,
                cycles = 15,
                cellsMv = listOf(
                    3336, 3335, 3335, 3335, 3335, 3335, 3336, 3336,
                    3335, 3335, 3335, 3335, 3335, 3335, 3336, 3335,
                    3335, 3334, 3335, 3335, 3335, 3335, 3335, 3335
                ),
                tempsC = listOf(32, 30, 30, 25),
                mosTempC = 31,
                chargeMosOn = true,
                dischargeMosOn = true,
                balancingActive = false,
                totalUsage = "21 d 5 h 25 m"
            ),
            history = listOf(
                ev("ErrDeltaT", "2026-11-10 15:09:22"),
                ev("ErrDeltaT", "2026-11-10 15:05:34"),
                ev("Temp Collect Faulty", "2026-11-09 15:41:47"),
                ev("Temp Collect Faulty", "2026-11-09 15:41:26"),
                ev("ErrSOC Low:3 Level", "2026-11-09 15:41:23"),
                ev("Power On", "2026-11-09 15:41:22"),
                ev("BMS Sleep", "2026-11-09 15:41:21"),
                ev("Voltage Line Offline", "2026-11-09 14:36:49"),
                ev("Voltage Collect Faulty", "2026-11-09 14:36:49"),
                ev("ErrSOC Low:3 Level", "2026-11-09 14:36:49"),
                ev("Power On", "2026-11-09 14:36:48"),
                ev("Power On", "2026-11-07 16:34:57"),
                ev("BMS Sleep", "2026-11-07 12:05:23"),
                ev("Power On", "2026-11-07 12:04:55"),
                ev("BMS Sleep", "2026-11-07 06:04:42"),
                ev("Power On", "2026-11-07 06:04:14"),
                ev("ErrCellV Low", "2026-11-03 20:25:54"),
                ev("Voltage Line Offline", "2026-11-03 20:25:53"),
                ev("Voltage Collect Faulty", "2026-11-03 20:25:53"),
                ev("ErrCellV Low", "2026-11-03 20:25:50"),
                ev("ErrCellV Low:2 Level", "2026-11-03 20:25:46"),
                ev("ErrCellV Low:3 Level", "2026-11-03 20:25:46"),
                ev("ErrCellV Low:3 Level", "2026-11-03 20:25:45"),
                ev("ErrCellV Low:2 Level", "2026-11-03 20:24:10"),
                ev("ErrCellV Low:3 Level", "2026-11-03 20:24:09"),
                ev("Short Circuit Protect", "2026-11-03 20:19:54"),
                ev("Short Circuit Protect", "2026-11-03 20:17:02"),
                ev("ErrCellV Low:2 Level", "2026-11-03 20:17:00"),
                ev("ErrCellV Low:3 Level", "2026-11-03 20:16:59"),
                ev("ErrCellV Low:3 Level", "2026-11-03 20:15:26"),
                ev("ErrCellV Low:2 Level", "2026-11-03 20:15:27"),
                ev("ErrCellV Low:3 Level", "2026-11-03 20:14:46"),
                ev("ErrCellV Low:2 Level", "2026-11-03 20:14:47"),
                ev("ErrCellV Low:3 Level", "2026-11-03 20:11:54"),
                ev("ErrCellV Low", "2026-11-03 20:11:50")
            ),
            parameters = vlkStandardParams
        ),

        // ------------------------------------------------------------------
        // 2. Batch 6 busbar break at cell 7 — the Jhang failure, 7 of 16 bikes.
        //    Cell 7 collapses several hundred mV below the pack under load.
        // ------------------------------------------------------------------
        DemoPack(
            discovered = DiscoveredPack(
                name = "JHB-D2191200E3A1",
                address = "D2:19:12:00:E3:A1",
                rssi = -52,
                knownChassis = "VLK14334",
                knownBatch = "Batch 6 (260126)"
            ),
            snapshot = PackSnapshot(
                deviceName = "JHB-D2191200E3A1",
                macAddress = "D2:19:12:00:E3:A1",
                batteryCode = "R24TK1A-24S100A",
                bmsSerial = "224KL160600388",
                firmware = "70_260526_MS51 / jhbS_70_250827",
                packVoltage = 76.9,
                current = -18.4,
                socPercent = 41.0,
                remainingAh = 14.8,
                ratedAh = 36.0,
                cycles = 214,
                cellsMv = listOf(
                    3218, 3215, 3221, 3216, 3219, 3214, 2834, 3217,
                    3220, 3215, 3218, 3216, 3213, 3219, 3222, 3214,
                    3217, 3209, 3216, 3218, 3215, 3220, 3214, 3217
                ),
                tempsC = listOf(38, 36, 37, 33),
                mosTempC = 44,
                chargeMosOn = true,
                dischargeMosOn = true,
                balancingActive = true,
                totalUsage = "243 d 11 h 02 m"
            ),
            history = listOf(
                ev("ErrCellV Low:3 Level", "2026-11-09 18:42:11"),
                ev("ErrCellV Low:2 Level", "2026-11-09 18:42:08"),
                ev("ErrCellV Low:3 Level", "2026-11-08 19:15:44"),
                ev("ErrCellV Low:3 Level", "2026-11-08 19:12:03"),
                ev("ErrCellV Low:2 Level", "2026-11-08 19:11:58"),
                ev("ErrCellV Low:3 Level", "2026-11-07 17:55:21"),
                ev("ErrCellV Low:3 Level", "2026-11-07 17:48:09"),
                ev("ErrCellV Low:2 Level", "2026-11-07 17:48:02"),
                ev("Power On", "2026-11-07 08:12:40"),
                ev("ErrCellV Low:3 Level", "2026-11-06 18:30:55"),
                ev("ErrCellV Low:3 Level", "2026-11-06 18:22:17"),
                ev("ErrCellV Low:2 Level", "2026-11-06 18:22:10"),
                ev("ErrDeltaV", "2026-11-06 18:21:44"),
                ev("BMS Sleep", "2026-11-06 02:10:08"),
                ev("Power On", "2026-11-05 07:58:31"),
                ev("ErrCellV Low:3 Level", "2026-11-04 19:02:48"),
                ev("ErrCellV Low:2 Level", "2026-11-04 19:02:41")
            ),
            parameters = vlkStandardParams
        ),

        // ------------------------------------------------------------------
        // 3. A healthy Batch 7 pack whose owner complains about range, with
        //    Capacity Learning switched on — the commonest false complaint.
        // ------------------------------------------------------------------
        DemoPack(
            discovered = DiscoveredPack(
                name = "JHB-D21912007C44",
                address = "D2:19:12:00:7C:44",
                rssi = -74,
                knownChassis = "VLK200226",
                knownBatch = "Batch 7 (260530)"
            ),
            snapshot = PackSnapshot(
                deviceName = "JHB-D21912007C44",
                macAddress = "D2:19:12:00:7C:44",
                batteryCode = "R24TK1A-24S100A",
                bmsSerial = "224KL160600944",
                firmware = "70_260526_MS51 / jhbS_70_250827",
                packVoltage = 79.4,
                current = -11.2,
                socPercent = 58.0,
                remainingAh = 20.9,
                ratedAh = 36.0,
                cycles = 96,
                cellsMv = listOf(
                    3309, 3311, 3308, 3310, 3312, 3307, 3309, 3311,
                    3310, 3308, 3313, 3309, 3307, 3310, 3312, 3308,
                    3311, 3306, 3309, 3310, 3308, 3312, 3309, 3310
                ),
                tempsC = listOf(34, 33, 33, 30),
                mosTempC = 39,
                chargeMosOn = true,
                dischargeMosOn = true,
                balancingActive = false,
                totalUsage = "118 d 03 h 47 m"
            ),
            history = listOf(
                ev("Power On", "2026-11-10 08:14:02"),
                ev("BMS Sleep", "2026-11-10 02:13:55"),
                ev("Power On", "2026-11-09 08:09:31"),
                ev("BMS Sleep", "2026-11-09 02:08:44"),
                ev("ErrSOC Low:1 Level", "2026-11-08 19:40:12"),
                ev("Power On", "2026-11-08 08:02:19")
            ),
            parameters = vlkStandardParams.copy(
                capacityLearning = true,
                activeBalancing = false
            )
        )
    )
}
