package com.vlektra.batterydoctor.bms

import com.vlektra.batterydoctor.model.BmsParameters
import com.vlektra.batterydoctor.model.DiscoveredPack
import com.vlektra.batterydoctor.model.HistoryEvent
import com.vlektra.batterydoctor.model.PackSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Everything the app needs from a battery, behind one interface.
 *
 * Two implementations exist:
 *  - [SimulatedBms]  : real Vlektra field data, no hardware needed. Used for
 *                      building, demonstrating and testing the rules.
 *  - [DalyBleSource] : the real thing over Bluetooth. Its read methods are
 *                      stubbed until the DALY protocol is confirmed.
 *
 * Nothing above this interface knows which one it is talking to, so swapping
 * the simulator for real Bluetooth changes no screen and no rule.
 */
interface BmsSource {

    /** Human name of this source, shown in the About screen. */
    val sourceLabel: String

    /** True when this source talks to real hardware. */
    val isLive: Boolean

    /** Packs currently visible. Emits again as the scan finds more. */
    fun scan(): Flow<List<DiscoveredPack>>

    suspend fun connect(address: String): Result<Unit>

    suspend fun disconnect()

    /** A fresh reading of the live data. */
    suspend fun readSnapshot(): Result<PackSnapshot>

    /** The stored history log, newest first. */
    suspend fun readHistory(): Result<List<HistoryEvent>>

    /** The current parameter set. */
    suspend fun readParameters(): Result<BmsParameters>

    /**
     * Change one parameter.
     *
     * Every call is expected to be gated by [com.vlektra.batterydoctor.model.AccessTier]
     * in the UI, and every call is written to the audit log before it is sent.
     */
    suspend fun writeParameter(change: ParameterChange): Result<Unit>
}

/** One parameter write, recorded before it is sent so the audit trail is complete. */
data class ParameterChange(
    val key: String,
    val label: String,
    val oldValue: String,
    val newValue: String,
    val byUser: String
)

/** Raised when a read or write is attempted before the protocol is wired up. */
class ProtocolNotAvailable(message: String) : Exception(message)
