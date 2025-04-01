package com.vnamashko.understandme.utils

fun getLanguageTag(code: String) = "$code-${languageToCountry[code]}"

val languageToCountry = mapOf(
    "en" to "US", // English -> United States
    "es" to "ES", // Spanish -> Spain
    "fr" to "FR", // French -> France
    "de" to "DE", // German -> Germany
    "it" to "IT", // Italian -> Italy
    "pt" to "BR", // Portuguese -> Brazil
    "zh" to "CN", // Chinese -> China
    "ja" to "JP", // Japanese -> Japan
    "ko" to "KR", // Korean -> South Korea
    "ar" to "SA", // Arabic -> Saudi Arabia
    "ru" to "RU", // Russian -> Russia
    "hi" to "IN", // Hindi -> India
    "bn" to "BD", // Bengali -> Bangladesh
    "pa" to "IN", // Punjabi -> India
    "mr" to "IN", // Marathi -> India
    "ta" to "IN", // Tamil -> India
    "te" to "IN", // Telugu -> India
    "ml" to "IN", // Malayalam -> India
    "gu" to "IN", // Gujarati -> India
    "kn" to "IN", // Kannada -> India
    "ur" to "PK", // Urdu -> Pakistan
    "vi" to "VN", // Vietnamese -> Vietnam
    "pl" to "PL", // Polish -> Poland
    "tr" to "TR", // Turkish -> Turkey
    "nl" to "NL", // Dutch -> Netherlands
    "sv" to "SE", // Swedish -> Sweden
    "no" to "NO", // Norwegian -> Norway
    "da" to "DK", // Danish -> Denmark
    "fi" to "FI", // Finnish -> Finland
    "el" to "GR", // Greek -> Greece
    "hu" to "HU", // Hungarian -> Hungary
    "cs" to "CZ", // Czech -> Czech Republic
    "ro" to "RO", // Romanian -> Romania
    "sk" to "SK", // Slovak -> Slovakia
    "bg" to "BG", // Bulgarian -> Bulgaria
    "hr" to "HR", // Croatian -> Croatia
    "sr" to "RS", // Serbian -> Serbia
    "mk" to "MK", // Macedonian -> North Macedonia
    "lv" to "LV", // Latvian -> Latvia
    "lt" to "LT", // Lithuanian -> Lithuania
    "et" to "EE", // Estonian -> Estonia
    "is" to "IS", // Icelandic -> Iceland
    "sq" to "AL", // Albanian -> Albania
    "hy" to "AM", // Armenian -> Armenia
    "ka" to "GE", // Georgian -> Georgia
    "az" to "AZ", // Azerbaijani -> Azerbaijan
    "km" to "KH", // Khmer -> Cambodia
    "lo" to "LA", // Lao -> Laos
    "my" to "MM", // Burmese -> Myanmar
    "th" to "TH", // Thai -> Thailand
    "km" to "KH", // Khmer -> Cambodia
    "ne" to "NP", // Nepali -> Nepal
    "si" to "LK", // Sinhala -> Sri Lanka
    "cy" to "GB", // Welsh -> United Kingdom
    "gl" to "ES", // Galician -> Spain
    "eu" to "ES", // Basque -> Spain
    "sq" to "AL", // Albanian -> Albania
    "mk" to "MK", // Macedonian -> North Macedonia
    "bs" to "BA", // Bosnian -> Bosnia and Herzegovina
    "oc" to "FR", // Occitan -> France
    "sw" to "KE", // Swahili -> Kenya
    "am" to "ET", // Amharic -> Ethiopia
    "iw" to "IL", // Hebrew -> Israel
    "yi" to "US", // Yiddish -> United States
    "zu" to "ZA", // Zulu -> South Africa
)