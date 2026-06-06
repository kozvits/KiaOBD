package com.yourapp.obd.data.speedcam

object SpeedCamConstants {

    data class SourcePreset(
        val name: String,
        val description: String,
        val url1: String,
        val url2: String = "",
        val url3: String = "",
        val requiresApiKey: Boolean = false,
        val apiKeyHint: String = ""
    )

    private const val BY_OVERPASS_NODES =
        "https://overpass-api.de/api/interpreter?data=" +
        "%5Bout%3Ajson%5D%5Btimeout%3A90%5D%3B" +
        "area%5B%22ISO3166-1%22%3D%22BY%22%5D%5Badmin_level%3D2%5D-%3E.a%3B" +
        "%28node%28area.a%29%5B%22highway%22%3D%22speed_camera%22%5D%3B" +
        "node%28area.a%29%5B%22enforcement%22%3D%22speed_camera%22%5D%3B" +
        "node%28area.a%29%5B%22highway%22%3D%22speed_display%22%5D%3B%29%3B" +
        "out%20body%3B"

    val PRESETS = listOf(
        SourcePreset(
            name = "Беларусь (OSM Overpass)",
            description = "Камеры из OpenStreetMap для РБ. Городские и трассовые. Бесплатно, без ключа.",
            url1 = BY_OVERPASS_NODES
        ),
        SourcePreset(
            name = "Беларусь (резервный сервер)",
            description = "Тот же запрос через зеркало Overpass Kumi Systems (если основной недоступен).",
            url1 = BY_OVERPASS_NODES.replace(
                "https://overpass-api.de/api/interpreter",
                "https://overpass.kumi.systems/api/interpreter"
            )
        )
    )

    val DEFAULT_PRESET: SourcePreset = PRESETS.first()
}
