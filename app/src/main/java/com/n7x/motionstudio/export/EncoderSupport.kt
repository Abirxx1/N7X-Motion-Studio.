package com.n7x.motionstudio.export

import android.media.MediaCodecList

/** Asks the phone's hardware what it can really encode. */
object EncoderSupport {
    private fun encoders(mime: String) = try {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun hasEncoder(mime: String): Boolean = encoders(mime).isNotEmpty()

    fun supportsSize(mime: String, width: Int, height: Int): Boolean = encoders(mime).any { info ->
        try {
            info.getCapabilitiesForType(mime).videoCapabilities?.isSizeSupported(width, height) == true
        } catch (e: Exception) {
            false
        }
    }

    fun supportsFps(mime: String, width: Int, height: Int, fps: Int): Boolean = encoders(mime).any { info ->
        try {
            info.getCapabilitiesForType(mime).videoCapabilities
                ?.areSizeAndRateSupported(width, height, fps.toDouble()) == true
        } catch (e: Exception) {
            false
        }
    }
}
