package com.n7x.motionstudio.export

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.SingleColorLut
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.google.common.collect.ImmutableList
import com.n7x.motionstudio.animation.Keyframes
import com.n7x.motionstudio.animation.TextAnimations
import com.n7x.motionstudio.data.AudioEntity
import com.n7x.motionstudio.data.MediaClipEntity
import com.n7x.motionstudio.data.OverlayEntity
import com.n7x.motionstudio.data.endMs
import com.n7x.motionstudio.data.lengthMs
import com.n7x.motionstudio.data.timelineMs
import com.n7x.motionstudio.effects.ColorOps
import com.n7x.motionstudio.effects.CubeLut
import java.io.File
import java.util.Locale
import kotlin.math.ceil

/**
 * Turns the project (clips, text, stickers, music) into a Media3 Composition.
 *
 * IMPORTANT (fixed in 0.8.2): keyframes, transitions and text/sticker overlays used to be
 * attached as Composition-level effects (Composition.Builder.setEffects). Media3 1.4.1 does not
 * reliably apply composition-level video effects - this is a known limitation (see the TODO in
 * Media3's own Composition.java about "composition level effects are only applied consistently
 * between the different VideoGraphs", bug b/302695659, and
 * https://github.com/androidx/media/issues/1687 where a Media3 engineer confirms composition
 * level effects are not supported yet and says to add the effect to every EditedMediaItem
 * instead). That mismatch is the most likely reason transitions/text/keyframes showed in the
 * preview but not in the exported file.
 *
 * Fix: the same MotionTransformation/MotionColor/OverlayEffect objects are now attached to every
 * clip's own EditedMediaItem (the "tail" list below), instead of to the Composition. All clips
 * still sit in one EditedMediaItemSequence, and per https://github.com/androidx/media/issues/1658
 * the presentationTimeUs an item's effect receives is continuous across that whole sequence (not
 * reset to 0 per item), so TimelineMotion's existing whole-timeline-millisecond math keeps working
 * unchanged.
 *
 * This fix is UNTESTED until a real export is checked on the phone.
 */
class ExportEngine(private val context: Context) {

    class Prepared(val composition: Composition, val tempFiles: List<File>, val plan: String)

    fun prepare(
        settings: ExportSettings,
        clips: List<MediaClipEntity>,
        overlays: List<OverlayEntity>,
        audios: List<AudioEntity>,
        workDir: File
    ): Prepared {
        val outW = settings.width
        val outH = settings.height
        val temp = mutableListOf<File>()
        val totalMs = clips.sumOf { it.timelineMs() }

        // Keyframe/transition motion and text/sticker overlays, evaluated on the whole timeline.
        // Built once and attached to EVERY clip's own effects list (see class doc above) instead
        // of to the Composition, because composition-level effects are not reliable in Media3
        // 1.4.1.
        val motion = TimelineMotion(clips)
        val overlayList = overlays.mapNotNull { overlayFor(it, outW, outH, totalMs) }
        val perClipTail = mutableListOf<Effect>()
        if (motion.needed) {
            perClipTail.add(MotionTransformation(motion, outW.toFloat() / outH.toFloat()))
            perClipTail.add(MotionColor(motion))
        }
        if (overlayList.isNotEmpty()) {
            perClipTail.add(OverlayEffect(ImmutableList.copyOf(overlayList)))
        }

        val sequences = mutableListOf<EditedMediaItemSequence>()
        sequences.add(EditedMediaItemSequence(clips.map { videoItem(it, settings, outW, outH, perClipTail) }))
        for (audio in audios) {
            audioSequence(audio, settings, totalMs, workDir, temp)?.let { sequences.add(it) }
        }

        val builder = Composition.Builder(sequences)
        return Prepared(builder.build(), temp, describe(settings, clips, overlays, audios))
    }

    /** A short description of what is exported. It is copied into error reports. */
    private fun describe(
        settings: ExportSettings,
        clips: List<MediaClipEntity>,
        overlays: List<OverlayEntity>,
        audios: List<AudioEntity>
    ): String = buildString {
        appendLine(
            "Output ${settings.width}x${settings.height} " +
                "${if (settings.h265) "H.265" else "H.264"} " +
                "fps=${if (settings.fps == 0) "source" else settings.fps} " +
                "bitrate=${String.format(Locale.US, "%.1f", settings.videoMbps())}Mbps safeMode=${settings.safeMode}"
        )
        clips.forEachIndexed { i, c ->
            appendLine(
                "clip $i: ${if (c.isVideo) "video" else "photo"} trim=${c.trimStartMs}-${c.trimEndMs}ms " +
                    "speed=${c.speed} volume=${c.volume} look=${c.filterId} lut=${c.lutPath.isNotEmpty()} " +
                    "transition=${c.transitionType}/${c.transitionMs}ms keyframes=${Keyframes.parse(c.keyframes).size}"
            )
        }
        audios.forEachIndexed { i, a ->
            appendLine(
                "audio $i: start=${a.startMs}ms trim=${a.trimStartMs}-${a.trimEndMs}ms " +
                    "source=${a.sourceMs}ms volume=${a.volume} file=${File(a.path).name}"
            )
        }
        appendLine("text/sticker layers: ${overlays.size} (effects attached per-clip, see export engine note)")
    }

    private fun videoItem(
        clip: MediaClipEntity,
        s: ExportSettings,
        w: Int,
        h: Int,
        perClipTail: List<Effect>
    ): EditedMediaItem {
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(File(clip.path)))
            .apply {
                if (clip.isVideo) {
                    setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(clip.trimStartMs)
                            .setEndPositionMs(clip.trimEndMs)
                            .build()
                    )
                }
            }
            .build()

        val video = mutableListOf<Effect>()
        val audio = mutableListOf<AudioProcessor>()
        val changedSpeed = clip.isVideo && clip.speed != 1f

        if (changedSpeed) {
            video.add(SpeedChangeEffect(clip.speed))
            if (!s.safeMode) {
                val sonic = SonicAudioProcessor()
                sonic.setSpeed(clip.speed)
                audio.add(sonic)
            }
        }
        ColorOps.build(clip.filterId, clip.brightness, clip.contrast, clip.saturation)?.let {
            video.add(ColorMatrixEffect(it))
        }
        if (clip.isVideo && clip.lutPath.isNotEmpty()) {
            try {
                video.add(SingleColorLut.createFromCube(CubeLut.parse(File(clip.lutPath))))
            } catch (e: Exception) {
                // a broken LUT is skipped
            }
        }
        video.add(Presentation.createForWidthAndHeight(w, h, Presentation.LAYOUT_SCALE_TO_FIT))
        if (s.fps > 0 && !s.safeMode) {
            video.add(FrameDropEffect.createDefaultFrameDropEffect(s.fps.toFloat()))
        }
        // keyframes/transitions/text run last, after the frame already matches the output
        // resolution and aspect ratio (Presentation above), on every clip's own item.
        video.addAll(perClipTail)
        if (clip.isVideo && clip.volume != 1f && !s.safeMode) audio.add(volumeProcessor(clip.volume))

        val item = EditedMediaItem.Builder(mediaItem).setEffects(Effects(audio, video))
        // safe mode: a clip with changed speed loses its sound instead of risking a failed export
        if (s.safeMode && changedSpeed) item.setRemoveAudio(true)
        if (!clip.isVideo) {
            item.setDurationUs((clip.trimEndMs - clip.trimStartMs) * 1000L).setFrameRate(30)
        }
        return item.build()
    }

    private fun audioSequence(
        audio: AudioEntity,
        s: ExportSettings,
        totalMs: Long,
        workDir: File,
        temp: MutableList<File>
    ): EditedMediaItemSequence? {
        val room = totalMs - audio.startMs
        if (room < 200L) return null
        val lengthMs = minOf(audio.lengthMs, room)
        val items = mutableListOf<EditedMediaItem>()
        if (audio.startMs > 0L) {
            val wav = File(workDir, "silence_${audio.id}.wav")
            SilenceWav.write(wav, audio.startMs)
            temp.add(wav)
            items.add(EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(wav))).build())
        }
        val clip = MediaItem.Builder()
            .setUri(Uri.fromFile(File(audio.path)))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(audio.trimStartMs)
                    .setEndPositionMs(audio.trimStartMs + lengthMs)
                    .build()
            )
            .build()
        val processors = if (s.safeMode || audio.volume == 1f) {
            emptyList<AudioProcessor>()
        } else {
            listOf(volumeProcessor(audio.volume))
        }
        items.add(
            EditedMediaItem.Builder(clip)
                .setRemoveVideo(true)
                .setEffects(Effects(processors, emptyList<Effect>()))
                .build()
        )
        return EditedMediaItemSequence(items)
    }

    private fun overlayFor(layer: OverlayEntity, w: Int, h: Int, totalMs: Long): TextureOverlay? {
        val full = OverlayRenderer.render(layer, w, h) ?: return null
        val startUs = layer.startMs * 1000L
        val endUs = minOf(layer.endMs, totalMs) * 1000L
        if (startUs >= endUs) return null
        return AnimatedOverlay(layer, w, h, full, startUs, endUs)
    }

    private fun volumeProcessor(volume: Float): AudioProcessor {
        val processor = ChannelMixingAudioProcessor()
        processor.putChannelMixingMatrix(ChannelMixingMatrix(1, 1, floatArrayOf(volume)))
        processor.putChannelMixingMatrix(ChannelMixingMatrix(2, 2, floatArrayOf(volume, 0f, 0f, volume)))
        return processor
    }

    /** The same 4x5 colour matrix the preview uses, as a Media3 colour effect. */
    private class ColorMatrixEffect(m: FloatArray) : RgbMatrix {
        private val glMatrix = FloatArray(16).also { g ->
            for (row in 0..2) {
                for (col in 0..2) g[col * 4 + row] = m[row * 5 + col]
                g[3 * 4 + row] = m[row * 5 + 4] / 255f
            }
            g[3 * 4 + 3] = 1f
        }

        override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = glMatrix
    }

    /**
     * A text or sticker layer with its start, stop and overall animation.
     * The animation values come from TextAnimations, the same code the preview uses.
     */
    private class AnimatedOverlay(
        private val layer: OverlayEntity,
        private val outW: Int,
        private val outH: Int,
        private val fullBitmap: Bitmap,
        private val startUs: Long,
        private val endUs: Long
    ) : BitmapOverlay() {
        private var current: Bitmap = fullBitmap
        private var shownCount = -1

        private fun animation(presentationTimeUs: Long) =
            TextAnimations.evaluate(layer, (presentationTimeUs - startUs) / 1000L)

        override fun getBitmap(presentationTimeUs: Long): Bitmap {
            val a = animation(presentationTimeUs)
            if (layer.kind == "text" && a.reveal < 1f) {
                val count = ceil(layer.text.length * a.reveal).toInt().coerceIn(0, layer.text.length)
                if (count != shownCount) {
                    current = OverlayRenderer.render(layer, outW, outH, count) ?: fullBitmap
                    shownCount = count
                }
            } else if (shownCount != -1) {
                current = fullBitmap
                shownCount = -1
            }
            return current
        }

        override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
            val visible = presentationTimeUs in startUs until endUs
            val a = animation(presentationTimeUs)
            return OverlaySettings.Builder()
                .setBackgroundFrameAnchor(2f * (layer.x + a.dx), -2f * (layer.y + a.dy))
                .setRotationDegrees(-(layer.rotation + a.rotation))
                .setScale(a.scale, a.scale)
                .setAlphaScale(if (visible) (layer.opacity * a.alpha).coerceIn(0f, 1f) else 0f)
                .build()
        }
    }
}
