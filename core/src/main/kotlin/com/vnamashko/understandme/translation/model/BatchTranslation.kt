package com.vnamashko.understandme.translation.model

sealed interface BatchTranslation {
    data class Translated(val translations: Map<String, String>) : BatchTranslation
    data object ModelMissing : BatchTranslation
}
