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

    val PRESETS = listOf(
        SourcePreset(
            name = "OpenStreetMap (Авто)",
            description = "Камеры из OSM для вашей страны. Нажмите «Авто» для заполнения. Бесплатно, без ключа.",
            url1 = ""
        ),
        SourcePreset(
            name = "Франция (data.gouv.fr)",
            description = "Официальные данные ~3400 камер. CSV, обновляется ежемесячно. Без API-ключа.",
            url1 = "https://www.data.gouv.fr/fr/datasets/r/87e2e9a4-c499-43e8-8580-7967a81c8e21",
            url2 = "https://www.data.gouv.fr/fr/datasets/r/9820dd82-ec3f-424e-bd49-d0ffa49619c2"
        ),
        SourcePreset(
            name = "Европа (Lufop)",
            description = "Крупнейшая база: FR, IT, ES, DE, CH, BE и др. Требуется бесплатный ключ с lufop.net.",
            url1 = "https://api.lufop.net/api?key=ВАШ_API_КЛЮЧ&format=json&nbr=10000&pays=fr",
            url2 = "https://api.lufop.net/api?key=ВАШ_API_КЛЮЧ&format=json&nbr=10000&pays=it",
            url3 = "https://api.lufop.net/api?key=ВАШ_API_КЛЮЧ&format=json&nbr=10000&pays=es",
            requiresApiKey = true,
            apiKeyHint = "https://lufop.net/en/lufop-api-access-the-most-complete-free-speed-camera-database/"
        ),
        SourcePreset(
            name = "США (SpeedCameraAPI)",
            description = "Тестовое API с камерами в крупных городах США. Бесплатно, без ключа.",
            url1 = "https://speedcameraapi.onrender.com/cameras/zipcode/10001",
            url2 = "https://speedcameraapi.onrender.com/cameras/zipcode/90028",
            url3 = "https://speedcameraapi.onrender.com/cameras/zipcode/60601"
        ),
        SourcePreset(
            name = "Мир (SpeedCams.world)",
            description = "68 000+ камер в 44 странах из OSM. CSV, обновляется регулярно.",
            url1 = "https://speedcams.world/download/all.csv"
        ),
        SourcePreset(
            name = "Австралия (SpeedCam.com.au)",
            description = "Данные правительственных источников Австралии. Бесплатно.",
            url1 = "https://www.speedcam.com.au/data/cameras.json"
        )
    )
}
