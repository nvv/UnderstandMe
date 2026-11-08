package com.vnamashko.understandme.translation.model

sealed interface TranslationResult {
    data class Translated(val text: String) : TranslationResult
    data object ModelMissing : TranslationResult
    data object Failed : TranslationResult
}
