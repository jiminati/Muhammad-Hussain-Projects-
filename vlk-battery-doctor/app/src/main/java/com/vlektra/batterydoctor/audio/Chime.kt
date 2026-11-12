package com.vlektra.batterydoctor.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.vlektra.batterydoctor.R

/**
 * The sound of a battery latching on.
 *
 * Struck glass, two notes a fifth apart, to match the surfaces. It is routed
 * as a sonification cue rather than media, so it follows the phone's own
 * silent switch — a technician in a showroom should not have to think about
 * it.
 *
 * A pair of haptic ticks lands with the two strikes. Felt and heard together,
 * the moment reads as a connection made even with the phone at arm's length.
 */
class Chime(context: Context) {

    private val appContext = context.applicationContext

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var soundId: Int = 0
    private var loaded = false
    /** Set when a connection beats the decoder; played the moment loading ends. */
    private var pendingPlay = false

    init {
        pool.setOnLoadCompleteListener { _, _, status ->
            loaded = status == 0
            if (loaded && pendingPlay) {
                pendingPlay = false
                playSound()
            }
        }
        soundId = try {
            pool.load(appContext, R.raw.connect_chime, 1)
        } catch (e: Exception) {
            0
        }
    }

    /** Call when a pack has connected and its first reading is in. */
    fun connected(withHaptics: Boolean = true) {
        if (loaded) playSound() else pendingPlay = true
        if (withHaptics) tick()
    }

    private fun playSound() {
        if (soundId == 0) return
        try {
            pool.play(soundId, 0.9f, 0.9f, 1, 0, 1.0f)
        } catch (e: Exception) {
            // A missing audio route is not worth interrupting a diagnosis for.
        }
    }

    /** Two short ticks, timed to the two strikes in the chime. */
    private fun tick() {
        val vibrator = vibrator() ?: return
        if (!vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 12, 113, 16)
                val amplitudes = intArrayOf(0, 90, 0, 150)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 12, 113, 16), -1)
            }
        } catch (e: Exception) {
            // Haptics are a nicety; never let them break the flow.
        }
    }

    private fun vibrator(): Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        null
    }

    fun release() {
        try {
            pool.release()
        } catch (e: Exception) {
            // already gone
        }
    }
}
