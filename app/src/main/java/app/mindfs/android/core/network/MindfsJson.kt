package app.mindfs.android.core.network

import kotlinx.serialization.json.Json

/** Shared Json instance: MindFS evolves weekly, so unknown fields must never break decoding. */
val MindfsJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
}
