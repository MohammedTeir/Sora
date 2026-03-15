package eu.kanade.translation.recognizer

import com.google.mlkit.nl.translate.TranslateLanguage
import tachiyomi.core.common.preference.Preference

enum class TextRecognizerLanguage(var code: String, val label: String) {
    ARABIC(TranslateLanguage.ARABIC, "Arabic"),
    CHINESE(TranslateLanguage.CHINESE, "Chinese (trad/sim)"),
    JAPANESE(TranslateLanguage.JAPANESE, "Japanese"),
    KOREAN(TranslateLanguage.KOREAN, "Korean"),
    ENGLISH(TranslateLanguage.ENGLISH, "English"),
    ;

    companion object {
        fun fromPref(pref: Preference<String>): TextRecognizerLanguage {
            val name = pref.get()
            var lang = entries.firstOrNull { it.name.equals(name, true) }
            if (lang == null) {
                pref.set(ARABIC.name)
                return ARABIC
            }
            return lang
        }
    }
}
