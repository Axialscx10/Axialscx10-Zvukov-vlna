package com.example.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import androidx.media3.common.C
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Manages the Android hardware Equalizer, BassBoost, Virtualizer (3D Spatial),
 * and LoudnessEnhancer attached to ExoPlayer's audio session ID.
 *
 * Design notes:
 * - Hardware EQ, BassBoost, and Spatial stay active on speakers, wired headphones, AND Bluetooth/Car
 *   outputs unless the user explicitly turns on the Emergency Bluetooth Bypass switch.
 * - Preamp / Auto-Gain headroom / ReplayGain negative gain is applied ONCE digitally via
 *   `player.volume` (0.20f..1.0f), while positive gain uses `LoudnessEnhancer`, preventing double
 *   attenuation across the EQ and player volume.
 * - All 10 UI bands (31 Hz..16 kHz) are averaged/interpolated in log-frequency space onto however
 *   many physical bands the device DSP provides (typically 5).
 */
object AudioEffectsManager {
    private val UI_TARGET_FREQS_HZ = floatArrayOf(
        31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f
    )

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
    private var replayGainMode: String = "Album Gain (Zachová dynamiku)"
    private var replayGainPreampDb: Float = 0f
    private var activeTrackRgDb: Float? = null
    private var activeAlbumRgDb: Float? = null

    // Emergency Car/BT DSP bypass is OFF by default so EQ & Bass work over Bluetooth normally.
    private var bluetoothVolumeStabilizerEnabled: Boolean = false

    private var playerVolumeListener: ((Float) -> Unit)? = null

    @Synchronized
    fun setPlayerVolumeListener(listener: ((Float) -> Unit)?) {
        playerVolumeListener = listener
        listener?.invoke(computePlayerVolume())
    }

    @Synchronized
    fun isBluetoothVolumeStabilizerEnabled(): Boolean = bluetoothVolumeStabilizerEnabled

    /**
     * Returns true ONLY when the user explicitly enabled the emergency hardware-DSP bypass
     * (and when stabilizing volume, also avoids audio focus ducking).
     */
    @Synchronized
    fun shouldBypassHardwareDsp(): Boolean = bluetoothVolumeStabilizerEnabled

    @Synchronized
    fun setBluetoothVolumeStabilizerEnabled(enabled: Boolean) {
        if (bluetoothVolumeStabilizerEnabled == enabled) return
        bluetoothVolumeStabilizerEnabled = enabled
        syncDspAttachment()
        notifyPlayerVolume()
    }

    @Synchronized
    private fun syncDspAttachment() {
        if (shouldBypassHardwareDsp() || !masterEnabled) {
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
     * Negative preamp / auto-gain headroom / ReplayGain attenuation is applied here ONCE.
     */
    @Synchronized
    fun computePlayerVolume(): Float {
        if (shouldBypassHardwareDsp()) return 1.0f
        if (!masterEnabled && !replayGainEnabled) return 1.0f
        val offsetDb = if (masterEnabled) {
            effectivePreampOffsetDb()
        } else {
            effectiveReplayGainDb()
        }.coerceIn(-12f, 0f)
        if (offsetDb >= -0.05f) return 1.0f
        return 10.0f.pow(offsetDb / 20.0f).coerceIn(0.20f, 1.0f)
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
        if (equalizer == null && currentAudioSessionId != C.AUDIO_SESSION_ID_UNSET && currentAudioSessionId != 0) {
            ensureAllEffects(currentAudioSessionId)
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
                @Suppress("DEPRECATION")
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
    fun setReplayGain(enabled: Boolean, rgPreampDb: Float, mode: String = replayGainMode) {
        replayGainEnabled = enabled
        replayGainPreampDb = rgPreampDb
        replayGainMode = mode
        if (!shouldBypassHardwareDsp() && masterEnabled) {
            applyLoudnessSettings()
        }
        notifyPlayerVolume()
    }

    @Synchronized
    fun setActiveTrackReplayGain(trackDb: Float?, albumDb: Float?) {
        activeTrackRgDb = trackDb
        activeAlbumRgDb = albumDb
        if (replayGainEnabled) {
            if (!shouldBypassHardwareDsp() && masterEnabled) {
                applyLoudnessSettings()
            }
            notifyPlayerVolume()
        }
    }

    private fun effectiveReplayGainDb(): Float {
        if (!replayGainEnabled) return 0f
        val isTrackMode = replayGainMode.contains("Track", ignoreCase = true) ||
            replayGainMode.contains("Skladba", ignoreCase = true)
        val tagGain = if (isTrackMode) {
            activeTrackRgDb ?: activeAlbumRgDb ?: 0f
        } else {
            activeAlbumRgDb ?: activeTrackRgDb ?: 0f
        }
        return (tagGain + replayGainPreampDb).coerceIn(-12f, 6f)
    }

    internal fun effectivePreampOffsetDb(): Float {
        val maxBoost = (bandLevels.maxOrNull() ?: 0f).coerceAtLeast(0f)
        val autoGainReduction = if (autoGainEnabled && maxBoost > 3.5f) {
            -((maxBoost - 3.5f) * 0.55f)
        } else {
            0f
        }
        return preampDb + autoGainReduction + effectiveReplayGainDb()
    }

    /**
     * Maps the 10 UI bands (31..16000 Hz) onto [hwCenterFreqsHz] using log-frequency Voronoi
     * buckets (or linear log2 interpolation if hardware has more bands) so every UI slider affects output.
     */
    internal fun mapUiBandsToHwBands(
        uiBands: List<Float>,
        hwCenterFreqsHz: FloatArray,
        subBassDb: Float,
        spatial: Boolean
    ): FloatArray {
        val numHw = hwCenterFreqsHz.size
        val result = FloatArray(numHw)
        if (numHw == 0) return result

        val effectiveUi = FloatArray(UI_TARGET_FREQS_HZ.size) { idx ->
            val subExtra = if (idx <= 1 && subBassDb > 0.1f) {
                subBassDb * (if (idx == 0) 0.35f else 0.18f)
            } else 0f
            val spatialExtra = if (spatial && idx >= 8) 0.8f else 0f
            uiBands.getOrElse(idx) { 0f } + subExtra + spatialExtra
        }

        if (numHw == 1) {
            result[0] = effectiveUi.average().toFloat()
            return result
        }

        for (i in 0 until numHw) {
            val lowBound = if (i == 0) 0f else sqrt(hwCenterFreqsHz[i - 1] * hwCenterFreqsHz[i])
            val highBound = if (i == numHw - 1) Float.MAX_VALUE else sqrt(hwCenterFreqsHz[i] * hwCenterFreqsHz[i + 1])

            var sum = 0f
            var count = 0
            for (uIdx in UI_TARGET_FREQS_HZ.indices) {
                val f = UI_TARGET_FREQS_HZ[uIdx]
                val inBucket = if (i == numHw - 1) f >= lowBound else (f >= lowBound && f < highBound)
                if (inBucket) {
                    sum += effectiveUi[uIdx]
                    count++
                }
            }
            result[i] = if (count > 0) {
                sum / count
            } else {
                // Linear interpolation in log2-frequency space
                interpolateLog2(hwCenterFreqsHz[i], effectiveUi)
            }
        }
        return result
    }

    private fun interpolateLog2(freqHz: Float, uiValues: FloatArray): Float {
        val safeF = freqHz.coerceAtLeast(1f)
        if (safeF <= UI_TARGET_FREQS_HZ.first()) return uiValues.first()
        if (safeF >= UI_TARGET_FREQS_HZ.last()) return uiValues.last()
        val logF = ln(safeF.toDouble())
        for (k in 0 until UI_TARGET_FREQS_HZ.size - 1) {
            val f0 = UI_TARGET_FREQS_HZ[k]
            val f1 = UI_TARGET_FREQS_HZ[k + 1]
            if (safeF in f0..f1) {
                val l0 = ln(f0.toDouble())
                val l1 = ln(f1.toDouble())
                val frac = ((logF - l0) / (l1 - l0)).toFloat().coerceIn(0f, 1f)
                return uiValues[k] * (1f - frac) + uiValues[k + 1] * frac
            }
        }
        return uiValues.last()
    }

    private fun applyEqualizerSettings() {
        if (shouldBypassHardwareDsp()) return
        val eq = equalizer ?: return
        try {
            eq.enabled = masterEnabled
            val numBands = eq.numberOfBands.toInt()
            if (numBands <= 0) return
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()

            val hwFreqs = FloatArray(numBands) { i ->
                (eq.getCenterFreq(i.toShort()) / 1000f).coerceAtLeast(20f)
            }
            val mappedDb = mapUiBandsToHwBands(bandLevels, hwFreqs, subBassBoostDb, spatialEnabled)

            for (i in 0 until numBands) {
                val millibels = (mappedDb[i] * 100f).roundToInt().coerceIn(minMb, maxMb).toShort()
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
            @Suppress("DEPRECATION")
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

    /** Test helper to reset mutable state between unit tests. */
    @Synchronized
    internal fun resetForTest() {
        releaseEffects()
        currentAudioSessionId = C.AUDIO_SESSION_ID_UNSET
        masterEnabled = true
        bandLevels = List(10) { 0f }
        preampDb = 0f
        subBassBoostDb = 0f
        spatialEnabled = false
        autoGainEnabled = false
        replayGainEnabled = false
        replayGainMode = "Album Gain (Zachová dynamiku)"
        replayGainPreampDb = 0f
        activeTrackRgDb = null
        activeAlbumRgDb = null
        bluetoothVolumeStabilizerEnabled = false
        playerVolumeListener = null
    }
}
