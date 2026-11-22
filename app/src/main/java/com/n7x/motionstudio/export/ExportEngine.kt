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
import com.n7x.motionstudio.data.AudioEntity
import com.n7x.motionstudio.data.MediaClipEntity
import com.n7x.motionstudio.data.OverlayEntity
import com.n7x.motionstudio.data.endMs
import com.n7x.motionstudio.data.lengthMs
import com.n7x.motionstudio.data.timelineMs
import com.n7x.motionstudio.effects.ColorOps
import com.n7x.motionstudio.effects.CubeLut
import java.io.File

/**
 * Turns the project (clips, text, stickers, music) into a Media3 Composition.
 * Not included yet: keyframe animation, transitions and text animations.
 */
class ExportEngine(private val context: Context) {

    class Prepared(val composition: Composition, val tempFiles: List<File>)

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

        val sequences = mutableListOf<EditedMediaItemSequence>()
        sequences.add(EditedMediaItemSequence(clips.map { videoItem(it, settings, outW, outH) }))
        for (audio in audios) {
            audioSequence(audio, totalMs, workDir, temp)?.let { sequences.add(it) }
        }

        val overlayList = overlays.mapNotNull { overlayFor(it, outW, outH, totalMs) }
        val builder = Composition.Builder(sequences)
        if (overlayList.isNotEmpty()) {
            builder.setEffects(
                Effects(emptyList(), listOf<Effect>(OverlayEffect(ImmutableList.copyOf(overlayList))))
            )
        }
        return Prepared(builder.build(), temp)
    }

    private fun videoItem(clip: MediaClipEntity, s: ExportSettings, w: Int, h: Int): EditedMediaItem {
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

        if (clip.isVideo && clip.speed != 1f) {
            video.add(SpeedChangeEffect(clip.speed))
            val sonic = SonicAudioProcessor()
            sonic.setSpeed(clip.speed)
            audio.add(sonic)
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
        if (s.fps > 0) video.add(FrameDropEffect.createDefaultFrameDropEffect(s.fps.toFloat()))
        if (clip.isVideo && clip.volume != 1f) audio.add(volumeProcessor(clip.volume))

        val item = EditedMediaItem.Builder(mediaItem).setEffects(Effects(audio, video))
        if (!clip.isVideo) {
            item.setDurationUs((clip.trimEndMs - clip.trimStartMs) * 1000L).setFrameRate(30)
        }
        return item.build()
    }

    private fun audioSequence(
        audio: AudioEntity,
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
        items.add(
            EditedMediaItem.Builder(clip)
                .setRemoveVideo(true)
                .setEffects(Effects(listOf<AudioProcessor>(volumeProcessor(audio.volume)), emptyList<Effect>()))
                .build()
        )
        return EditedMediaItemSequence(items)
    }

    private fun overlayFor(layer: OverlayEntity, w: Int, h: Int, totalMs: Long): TextureOverlay? {
        val bitmap = OverlayRenderer.render(layer, w, h) ?: return null
        val startUs = layer.startMs * 1000L
        val endUs = minOf(layer.endMs, totalMs) * 1000L
        if (startUs >= endUs) return null
        return LayerOverlay(bitmap, startUs, endUs, 2f * layer.x, -2f * layer.y, -layer.rotation, layer.opacity)
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

    /** A text or sticker bitmap that is visible between startUs and endUs. */
    private class LayerOverlay(
        private val bitmap: Bitmap,
        private val startUs: Long,
        private val endUs: Long,
        private val anchorX: Float,
        private val anchorY: Float,
        private val rotation: Float,
        private val opacity: Float
    ) : BitmapOverlay() {
        override fun getBitmap(presentationTimeUs: Long): Bitmap = bitmap

        override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
            val visible = presentationTimeUs in startUs until endUs
            return OverlaySettings.Builder()
                .setBackgroundFrameAnchor(anchorX, anchorY)
                .setRotationDegrees(rotation)
                .setAlphaScale(if (visible) opacity else 0f)
                .build()
        }
    }
}
