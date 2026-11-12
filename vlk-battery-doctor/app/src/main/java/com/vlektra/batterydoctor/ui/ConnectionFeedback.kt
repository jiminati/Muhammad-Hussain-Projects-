package com.vlektra.batterydoctor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.vlektra.batterydoctor.AppState
import com.vlektra.batterydoctor.audio.Chime

/**
 * Sounds the chime the first time each pack connects.
 *
 * Keyed on the pack's address rather than on "connected or not", so coming
 * back to a screen or rotating the phone does not re-trigger it, while
 * moving to a different battery does.
 */
@Composable
fun ConnectionFeedback(state: AppState) {
    val context = LocalContext.current
    val chime = remember { Chime(context) }

    DisposableEffect(Unit) {
        onDispose { chime.release() }
    }

    var lastAnnounced by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.connected?.address, state.snapshot != null) {
        val address = state.connected?.address
        // Wait for the first reading, so the sound marks a usable connection
        // rather than a handshake that might still fail.
        if (address != null && state.snapshot != null && address != lastAnnounced) {
            lastAnnounced = address
            if (state.soundOn) chime.connected()
        }
        if (address == null) lastAnnounced = null
    }
}
