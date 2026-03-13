package eu.kanade.translation.model

import kotlinx.serialization.Serializable

@Serializable
data class PageTranslation(
    var blocks: MutableList<TranslationBlock> = mutableListOf(),
    var imgWidth: Float = 0f,
    var imgHeight: Float = 0f,
) {
    companion object {
        val EMPTY = PageTranslation()
    }
}

@Serializable
data class TranslationBlock(
    var text: String,
    var translation: String = "",
    var width: Float,
    var height: Float,
    var x: Float,
    var y: Float,
    var symHeight: Float,
    var symWidth: Float,
    val angle: Float,

) {
    /** Padding for the white background rectangle — slightly larger than text to ensure full coverage. */
    fun backgroundPadding(): Pair<Float, Float> {
        val padX = symWidth * 2.5f
        val padY = symHeight * 1.5f
        return Pair(padX, padY)
    }
 
    /** Padding for the text content area. */
    fun textPadding(): Pair<Float, Float> {
        val padX = symWidth * 2f
        val padY = symHeight
        return Pair(padX, padY)
    }
}
