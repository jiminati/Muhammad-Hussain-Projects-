package com.vlektra.batterydoctor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlektra.batterydoctor.bms.BmsSource
import com.vlektra.batterydoctor.bms.DalyBleSource
import com.vlektra.batterydoctor.bms.SimulatedBms
import com.vlektra.batterydoctor.diagnosis.DiagnosisResult
import com.vlektra.batterydoctor.diagnosis.RuleEngine
import com.vlektra.batterydoctor.model.BmsParameters
import com.vlektra.batterydoctor.model.DiscoveredPack
import com.vlektra.batterydoctor.model.HistoryEvent
import com.vlektra.batterydoctor.model.JobCard
import com.vlektra.batterydoctor.model.PackSnapshot
import kotlinx.coroutines.launch

enum class Screen { SPLASH, CONNECT, HOME, ALARMS, ENGINEER, PARAMETERS, REPORT, ABOUT }

class AppState : ViewModel() {

    // ---- navigation ----
    var screen by mutableStateOf(Screen.SPLASH)
        private set

    /** Engineering mode shows the raw numbers. Base mode hides them. */
    var engineeringMode by mutableStateOf(false)

    /** The connection chime. Off for anyone working somewhere quiet. */
    var soundOn by mutableStateOf(true)

    fun splashDone() {
        if (screen == Screen.SPLASH) screen = Screen.CONNECT
    }

    fun go(to: Screen) {
        screen = to
    }

    // ---- source ----
    var useSimulator by mutableStateOf(true)
        private set

    private var simulated: BmsSource = SimulatedBms()
    private var live: BmsSource = DalyBleSource()
    val source: BmsSource get() = if (useSimulator) simulated else live

    fun setSimulator(on: Boolean) {
        if (on == useSimulator) return
        viewModelScope.launch { source.disconnect() }
        useSimulator = on
        reset()
    }

    // ---- scan ----
    var found by mutableStateOf<List<DiscoveredPack>>(emptyList())
        private set
    var scanning by mutableStateOf(false)
        private set

    fun startScan() {
        scanning = true
        found = emptyList()
        error = null
        viewModelScope.launch {
            try {
                source.scan().collect { found = it }
            } catch (e: Exception) {
                error = e.message
            } finally {
                scanning = false
            }
        }
    }

    // ---- connection and data ----
    var connected by mutableStateOf<DiscoveredPack?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var busyLabel by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)

    var snapshot by mutableStateOf<PackSnapshot?>(null)
        private set
    var history by mutableStateOf<List<HistoryEvent>>(emptyList())
        private set
    var parameters by mutableStateOf<BmsParameters?>(null)
        private set
    var diagnosis by mutableStateOf<DiagnosisResult?>(null)
        private set

    var job by mutableStateOf(JobCard())
    var rangeComplaint by mutableStateOf(false)

    private fun reset() {
        connected = null
        snapshot = null
        history = emptyList()
        parameters = null
        diagnosis = null
        found = emptyList()
        error = null
        screen = Screen.CONNECT
    }

    fun connectAndRead(pack: DiscoveredPack) {
        busy = true
        busyLabel = "Connecting to ${pack.name}"
        error = null
        viewModelScope.launch {
            val c = source.connect(pack.address)
            if (c.isFailure) {
                error = c.exceptionOrNull()?.message ?: "Could not connect"
                busy = false
                return@launch
            }
            connected = pack
            job = job.copy(
                batteryId = pack.name,
                chassisNumber = pack.knownChassis ?: job.chassisNumber
            )

            busyLabel = "Reading live data"
            source.readSnapshot()
                .onSuccess { snapshot = it }
                .onFailure { error = it.message }

            busyLabel = "Reading history log"
            source.readHistory()
                .onSuccess { history = it }
                .onFailure { /* history is optional; keep going */ }

            busyLabel = "Reading parameters"
            source.readParameters()
                .onSuccess { parameters = it }
                .onFailure { /* parameters are optional; keep going */ }

            recompute()
            busy = false
            busyLabel = ""
            if (snapshot != null) screen = Screen.HOME
        }
    }

    fun refresh() {
        val pack = connected ?: return
        busy = true
        busyLabel = "Re-reading ${pack.name}"
        viewModelScope.launch {
            source.readSnapshot()
                .onSuccess { snapshot = it }
                .onFailure { error = it.message }
            recompute()
            busy = false
            busyLabel = ""
        }
    }

    fun recompute() {
        val s = snapshot ?: return
        diagnosis = RuleEngine.diagnose(
            snapshot = s,
            history = history,
            parameters = parameters,
            batchHint = connected?.knownBatch,
            rangeComplaint = rangeComplaint
        )
    }

    fun disconnect() {
        viewModelScope.launch {
            source.disconnect()
            reset()
        }
    }
}
