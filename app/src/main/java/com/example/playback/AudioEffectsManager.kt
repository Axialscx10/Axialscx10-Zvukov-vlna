package com.example.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import androidx.media3.common.C
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Manages the Android hardware Equalizer, BassBoost, Virtualizer (3D Spatial),
 * and LoudnessEnhancer attached to ExoPlayer's audio session ID.
 *
 * CRITICAL CAR BLUETOOTH FIX (v31.0):
 * On Android (Samsung SoundAlive, Xiaomi MiSound, Pixel/Motorola/OnePlus `libbundlewrapper.so`),
 * attaching ANY hardware `AudioEffect` (including `Equalizer`) to an `audioSessionId` routes the
 * `AudioTrack` through the OS `EffectChain` mixer instead of the direct A2DP `FastMixer`.
 * When Bluetooth A2DP resumes from Pause (`AVDTP_START`), `libbundlewrapper.so` engages its internal
 * biquad limiter / headroom attenuation (-6 dB to -9 dB) on the first Play until the user pauses and
 * plays a second time.
 *
 * Therefore, whenever `bluetoothVolumeStabilizerEnabled` is ON (default) OR a Bluetooth audio output
 * is active, ALL hardware `AudioEffect` instances (`Equalizer`, `BassBoost`, `Virtualizer`,
 * `LoudnessEnhancer`) are completely detached and released from `audioSessionId`, guaranteeing a 100%
 * pure, un-attenuated native PCM stream at full 1.0f volume on the very first resume after Pause.
 */
object AudioEffectsManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    private var masterEnabled: Boolean = true
    private var bandLevels: List<Float> = List(10) { 0f }
    private var preampDb: Float = 0f
    private var subBassBoostDb: Float = 2.0f
    private var spatialEnabled: Boolean = false
    private var autoGainEnabled: Boolean = true
    private var replayGainEnabled: Boolean = false
    private var replayGainPreampDb: Float = 0f
    private var bluetoothVolumeStabilizerEnabled: Boolean = true
    private var bluetoothOutputConnected: Boolean = false

    private var playerVolumeListener: ((Float) -> Unit)? = null

    @Synchronized
    fun setPlayerVolumeListener(listener: ((Float) -> Unit)?) {
        playerVolumeListener = listener
        listener?.invoke(computePlayerVolume())
    }

    @Synchronized
    fun isBluetoothVolumeStabilizerEnabled(): Boolean = bluetoothVolumeStabilizerEnabled

    /**
     * Returns true when hardware DSP effects must be bypassed to prevent Bluetooth A2DP
     * pause/resume volume drops in cars.
     */
    @Synchronized
    fun shouldBypassHardwareDsp(): Boolean =
        bluetoothVolumeStabilizerEnabled || bluetoothOutputConnected

    @Synchronized
    fun setBluetoothOutputConnected(connected: Boolean) {
        if (bluetoothOutputConnected == connected) return
        bluetoothOutputConnected = connected
        syncDspAttachment()
        notifyPlayerVolume()
    }

    @Synchronized
    fun setBluetoothVolumeStabilizerEnabled(enabled: Boolean) {
        bluetoothVolumeStabilizerEnabled = enabled
        syncDspAttachment()
        notifyPlayerVolume()
    }

    @Synchronized
    private fun syncDspAttachment() {
        if (shouldBypassHardwareDsp() || !masterEnabled) {
            // Completely release ALL AudioEffects (including Equalizer) so AudioFlinger routes
            // the stream directly through FastMixer with zero libbundlewrapper.so attenuation!
            releaseEffects()
        } else if (currentAudioSessionId != C.AUDIO_SESSION_ID_UNSET && currentAudioSessionId != 0) {
            ensureAllEffects(currentAudioSessionId)
            applyEqualizerSettings()
            applyBassBoostSettings()
            applyVirtualizerSettings()
            applyLoudnessSettings()
        }
    }

    /**
     * Computes digital volume for ExoPlayer (0.20f .. 1.0f).
     * When Bluetooth Volume Stabilizer is ON or Bluetooth output is connected, digital volume
     * is ALWAYS locked to 1.0f (100%).
     */
    @Synchronized
    fun computePlayerVolume(): Float {
        if (!masterEnabled || shouldBypassHardwareDsp()) return 1.0f
        val totalPreampDb = effectivePreampOffsetDb().coerceIn(-12f, 0f)
        if (totalPreampDb >= -0.1f) return 1.0f
        return 10.0f.pow(totalPreampDb / 20.0f).coerceIn(0.20f, 1.0f)
    }

    private fun notifyPlayerVolume() {
        val vol = computePlayerVolume()
        playerVolumeListener?.invoke(vol)
    }

    @Synchronized
    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return
        val sessionChanged = currentAudioSessionId != audioSessionId
        currentAudioSessionId = audioSessionId

        if (shouldBypassHardwareDsp() || !masterEnabled) {
            releaseEffects()
            notifyPlayerVolume()
            return
        }

        if (sessionChanged) {
            releaseEffects()
        }
        ensureAllEffects(audioSessionId)
        applyEqualizerSettings()
        applyBassBoostSettings()
        applyVirtualizerSettings()
        applyLoudnessSettings()
        notifyPlayerVolume()
    }

    @Synchronized
    fun onPlaybackResumed() {
        if (shouldBypassHardwareDsp() || !masterEnabled) {
            releaseEffects()
            notifyPlayerVolume()
            return
        }
        applyEqualizerSettings()
        applyBassBoostSettings()
        applyVirtualizerSettings()
        applyLoudnessSettings()
        notifyPlayerVolume()
    }

    private fun ensureAllEffects(audioSessionId: Int) {
        if (shouldBypassHardwareDsp() || !masterEnabled) return
        if (equalizer == null) {
            try {
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = masterEnabled
                }
            } catch (_: Throwable) {
                equalizer = null
            }
        }
        if (bassBoost == null) {
            try {
                bassBoost = BassBoost(0, audioSessionId).apply {
                    enabled = masterEnabled && (subBassBoostDb > 0.1f)
                }
            } catch (_: Throwable) {
                bassBoost = null
            }
        }
        if (virtualizer == null) {
            try {
                virtualizer = Virtualizer(0, audioSessionId).apply {
                    enabled = masterEnabled && spatialEnabled
                }
            } catch (_: Throwable) {
                virtualizer = null
            }
        }
        if (loudnessEnhancer == null) {
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            } catch (_: Throwable) {
                loudnessEnhancer = null
            }
        }
    }

    @Synchronized
    fun setMasterEnabled(enabled: Boolean) {
        masterEnabled = enabled
        syncDspAttachment()
        notifyPlayerVolume()
    }

    @Synchronized
    fun setBands(bands: List<Float>, preamp: Float) {
        bandLevels = bands
        preampDb = preamp
        if (!shouldBypassHardwareDsp() && masterEnabled) {
            applyEqualizerSettings()
            applyLoudnessSettings()
        }
        notifyPlayerVolume()
    }

    @Synchronized
    fun setSubBassBoost(db: Float) {
        subBassBoostDb = db
        if (!shouldBypassHardwareDsp() && masterEnabled) {
            applyBassBoostSettings()
            applyEqualizerSettings()
        }
    }

    @Synchronized
    fun setSpatialEnabled(enabled: Boolean) {
        spatialEnabled = enabled
        if (!shouldBypassHardwareDsp() && masterEnabled) {
            applyVirtualizerSettings()
            applyEqualizerSettings()
        }
    }

    @Synchronized
    fun setAutoGainEnabled(enabled: Boolean) {
        autoGainEnabled = enabled
        if (!shouldBypassHardwareDsp() && masterEnabled) {
            applyEqualizerSettings()
            applyLoudnessSettings()
        }
        notifyPlayerVolume()
    }

    @Synchronized
    fun setReplayGain(enabled: Boolean, rgPreampDb: Float) {
        replayGainEnabled = enabled
        replayGainPreampDb = rgPreampDb
        if (!shouldBypassHardwareDsp() && masterEnabled) {
            applyEqualizerSettings()
            applyLoudnessSettings()
        }
        notifyPlayerVolume()
    }

    private fun effectivePreampOffsetDb(): Float {
        val maxBoost = (bandLevels.maxOrNull() ?: 0f).coerceAtLeast(0f)
        val autoGainReduction = if (autoGainEnabled && maxBoost > 3.5f) {
            -((maxBoost - 3.5f) * 0.55f)
        } else {
            0f
        }
        val rgOffset = if (replayGainEnabled) replayGainPreampDb else 0f
        return preampDb + autoGainReduction + rgOffset
    }

    private fun applyEqualizerSettings() {
        if (shouldBypassHardwareDsp()) return
        val eq = equalizer ?: return
        try {
            eq.enabled = masterEnabled
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()

            val targetFreqs = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

            val rawHwDb = FloatArray(numBands)
            for (i in 0 until numBands) {
                val hwCenterFreqHz = eq.getCenterFreq(i.toShort()) / 1000
                var closestIdx = 0
                var minDiff = Int.MAX_VALUE
                for ((idx, f) in targetFreqs.withIndex()) {
                    val diff = abs(f - hwCenterFreqHz)
                    if (diff < minDiff) {
                        minDiff = diff
                        closestIdx = idx
                    }
                }
                val subExtra = if (closestIdx <= 1 && subBassBoostDb > 0.1f) {
                    subBassBoostDb * (if (closestIdx == 0) 0.35f else 0.18f)
                } else 0f
                val spatialExtra = if (spatialEnabled && closestIdx >= 8) 0.8f else 0f
                val db = bandLevels.getOrElse(closestIdx) { 0f } + subExtra + spatialExtra
                rawHwDb[i] = db
            }

            val headroomOffsetDb = effectivePreampOffsetDb().coerceAtMost(0f)

            for (i in 0 until numBands) {
                val dbVal = rawHwDb[i] + headroomOffsetDb
                val millibels = (dbVal * 100f).roundToInt().coerceIn(minMb, maxMb).toShort()
                eq.setBandLevel(i.toShort(), millibels)
            }
        } catch (_: Throwable) {}
    }

    private fun applyBassBoostSettings() {
        if (shouldBypassHardwareDsp()) return
        val bb = bassBoost ?: return
        try {
            val useHwBass = masterEnabled && (subBassBoostDb > 0.1f)
            bb.enabled = useHwBass
            if (useHwBass && bb.strengthSupported) {
                val strength = ((subBassBoostDb / 10f) * 1000).roundToInt().coerceIn(0, 1000).toShort()
                bb.setStrength(strength)
            }
        } catch (_: Throwable) {}
    }

    private fun applyVirtualizerSettings() {
        if (shouldBypassHardwareDsp()) return
        val virt = virtualizer ?: return
        try {
            val useHwVirt = masterEnabled && spatialEnabled
            virt.enabled = useHwVirt
            if (useHwVirt && virt.strengthSupported) {
                virt.setStrength(650.toShort())
            }
        } catch (_: Throwable) {}
    }

    private fun applyLoudnessSettings() {
        if (shouldBypassHardwareDsp()) return
        val le = loudnessEnhancer ?: return
        try {
            val effPreamp = effectivePreampOffsetDb()
            if (masterEnabled && effPreamp > 0.2f) {
                le.enabled = true
                val targetMb = (effPreamp * 100f).roundToInt().coerceIn(0, 900)
                le.setTargetGain(targetMb)
            } else {
                le.setTargetGain(0)
                le.enabled = false
            }
        } catch (_: Throwable) {}
    }

    @Synchronized
    fun releaseEffects() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (_: Throwable) {}
        equalizer = null

        try {
            bassBoost?.enabled = false
            bassBoost?.release()
        } catch (_: Throwable) {}
        bassBoost = null

        try {
            virtualizer?.enabled = false
            virtualizer?.release()
        } catch (_: Throwable) {}
        virtualizer = null

        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (_: Throwable) {}
        loudnessEnhancer = null
    }
}
