package com.vlektra.batterydoctor.bms

import com.vlektra.batterydoctor.model.BmsParameters
import com.vlektra.batterydoctor.model.DiscoveredPack
import com.vlektra.batterydoctor.model.HistoryEvent
import com.vlektra.batterydoctor.model.PackSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

/**
 * The real DALY BMS over Bluetooth Low Energy.
 *
 * STATE OF THIS FILE
 * ------------------
 * The transport layer below (scan filter, GATT service and characteristics,
 * Modbus-RTU framing and CRC) is the published behaviour of the DALY
 * Bluetooth module used on the JHB-R24TK boards, and is believed correct.
 *
 * What is NOT yet known, and therefore NOT implemented:
 *   - the register addresses for live data and their scaling factors
 *   - the command sequence that walks the stored history log
 *   - the register addresses for the settable parameters
 *
 * Those are filled in from either the DALY protocol document or a decoded
 * capture of the official app. Until then every read here fails loudly with
 * [ProtocolNotAvailable] rather than guessing: a wrong register read returns
 * nonsense, and a wrong register WRITE could change a protection threshold on
 * a live pack. Guessing is not acceptable on battery hardware.
 */
class DalyBleSource : BmsSource {

    override val sourceLabel = "DALY BMS over Bluetooth"
    override val isLive = true

    companion object {
        /** Nordic UART-style service the DALY BLE module exposes. */
        val SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")

        /** App writes commands here. */
        val WRITE_CHAR_UUID: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")

        /** BMS notifies replies here. */
        val NOTIFY_CHAR_UUID: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")

        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        /** Advertised name prefixes seen on Vlektra packs. */
        val NAME_PREFIXES = listOf("JHB-", "DL-")

        /** Modbus slave address the BMS answers on. */
        const val SLAVE_ADDRESS: Int = 0xD2
    }

    // ------------------------------------------------------------------
    // Modbus RTU framing — independent of the register map, so it is
    // already correct and unit-testable today.
    // ------------------------------------------------------------------

    /** Standard Modbus CRC-16, low byte first on the wire. */
    fun crc16(data: ByteArray): Int {
        var crc = 0xFFFF
        for (b in data) {
            crc = crc xor (b.toInt() and 0xFF)
            repeat(8) {
                crc = if (crc and 1 != 0) (crc shr 1) xor 0xA001 else crc shr 1
            }
        }
        return crc and 0xFFFF
    }

    /** Build a "read holding registers" frame: addr, 0x03, start, count, CRC. */
    fun buildReadFrame(startRegister: Int, registerCount: Int): ByteArray {
        val body = byteArrayOf(
            SLAVE_ADDRESS.toByte(),
            0x03,
            ((startRegister shr 8) and 0xFF).toByte(),
            (startRegister and 0xFF).toByte(),
            ((registerCount shr 8) and 0xFF).toByte(),
            (registerCount and 0xFF).toByte()
        )
        val crc = crc16(body)
        return body + byteArrayOf((crc and 0xFF).toByte(), ((crc shr 8) and 0xFF).toByte())
    }

    /** Build a "write single register" frame: addr, 0x06, register, value, CRC. */
    fun buildWriteFrame(register: Int, value: Int): ByteArray {
        val body = byteArrayOf(
            SLAVE_ADDRESS.toByte(),
            0x06,
            ((register shr 8) and 0xFF).toByte(),
            (register and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
        val crc = crc16(body)
        return body + byteArrayOf((crc and 0xFF).toByte(), ((crc shr 8) and 0xFF).toByte())
    }

    /** True when a received frame's trailing CRC matches its contents. */
    fun frameIsValid(frame: ByteArray): Boolean {
        if (frame.size < 4) return false
        val body = frame.copyOfRange(0, frame.size - 2)
        val got = (frame[frame.size - 2].toInt() and 0xFF) or
            ((frame[frame.size - 1].toInt() and 0xFF) shl 8)
        return crc16(body) == got
    }

    // ------------------------------------------------------------------
    // Transport — wired up once the register map is known.
    // ------------------------------------------------------------------

    private val notImplemented = ProtocolNotAvailable(
        "The DALY register map is not confirmed yet. Reading a live pack is " +
            "disabled until the protocol document arrives, so the app cannot " +
            "show you a wrong number. Use a demo pack in the meantime."
    )

    override fun scan(): Flow<List<DiscoveredPack>> = flow {
        emit(emptyList())
        throw notImplemented
    }

    override suspend fun connect(address: String): Result<Unit> =
        Result.failure(notImplemented)

    override suspend fun disconnect() = Unit

    override suspend fun readSnapshot(): Result<PackSnapshot> =
        Result.failure(notImplemented)

    override suspend fun readHistory(): Result<List<HistoryEvent>> =
        Result.failure(notImplemented)

    override suspend fun readParameters(): Result<BmsParameters> =
        Result.failure(notImplemented)

    override suspend fun writeParameter(change: ParameterChange): Result<Unit> =
        Result.failure(notImplemented)
}
