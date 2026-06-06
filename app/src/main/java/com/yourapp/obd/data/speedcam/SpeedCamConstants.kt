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
            name = "Беларусь (OSM Overpass)",
            description = "Камеры из OpenStreetMap для РБ (городские и трассовые). Обновляется сообществом. Бесплатно.",
            url1 = "https://overpass-api.de/api/interpreter?data=%5Bout%3Ajson%5D%5Btimeout%3A45%5D%3Barea%5B%22ISO3166-1%22%3D%22BY%22%5D%5Badmin_level%3D2%5D-%3E.a%3B%28node%28area.a%29%5B%22highway%22%3D%22speed_camera%22%5D%3Bnode%28area.a%29%5B%22enforcement%22%3D%22speed_camera%22%5D%3Bnode%28area.a%29%5B%22highway%22%3D%22speed_display%22%5D%3B%29%3Bout%20body%3B",
            url2 = "https://overpass-api.de/api/interpreter?data=%5Bout%3Ajson%5D%5Btimeout%3A45%5D%3Barea%5B%22ISO3166-1%22%3D%22BY%22%5D%5Badmin_level%3D2%5D-%3E.a%3B%28way%28area.a%29%5B%22highway%22%3D%22speed_camera%22%5D%3Brelation%28area.a%29%5B%22highway%22%3D%22speed_camera%22%5D%3B%29%3Bout%20center%3B",
            url3 = ""
        ),
        SourcePreset(
            name = "Беларусь (Mapillary)",
            description = "Камеры из Mapillary для РБ. Бесплатно, данные сообщества.",
            url1 = "https://a.mapillary.com/v3/images?closeto=53.9,27.57&radius=50000&limit=500&client_id=REPLACE_WITH_YOUR_CLIENT_ID",
            url2 = "",
            url3 = "",
            requiresApiKey = true,
            apiKeyHint = "Зарегистрируйтесь на mapillary.com и получите client_id"
        ),
        SourcePreset(
            name = "Мир (SpeedCams.world)",
            description = "Камеры из OSM по странам. Укажите CSV-ссылку со страницы speedcams.world/download.",
            url1 = ""
        ),
        SourcePreset(
            name = "Европа+СНГ (Lufop)",
            description = "Крупнейшая база: FR, IT, ES, DE, CH, BE, PL, RU и др. Требуется бесплатный ключ с lufop.net.",
            url1 = "https://api.lufop.net/api?key=ВАШ_API_КЛЮЧ&format=json&nbr=10000&pays=fr",
            url2 = "https://api.lufop.net/api?key=ВАШ_API_КЛЮЧ&format=json&nbr=10000&pays=it",
            url3 = "https://api.lufop.net/api?key=ВАШ_API_КЛЮЧ&format=json&nbr=10000&pays=es",
            requiresApiKey = true,
            apiKeyHint = "https://lufop.net/en/lufop-api-access-the-most-complete-free-speed-camera-database/"
        ),
        SourcePreset(
            name = "Франция (data.gouv.fr)",
            description = "Официальные данные ~3400 камер. CSV, обновляется ежемесячно. Без API-ключа.",
            url1 = "https://static.data.gouv.fr/resources/liste-des-radars-fixes-en-france/20251230-134204/jeu-de-donnees-liste-des-radars-fixes-en-france-12-2025.csv",
            url2 = ""
        ),
        SourcePreset(
            name = "США (SpeedCameraAPI)",
            description = "Тестовое API с камерами в крупных городах США. Бесплатно, без ключа.",
            url1 = "https://speedcameraapi.onrender.com/cameras/zipcode/10001",
            url2 = "https://speedcameraapi.onrender.com/cameras/zipcode/90028",
            url3 = "https://speedcameraapi.onrender.com/cameras/zipcode/60601"
        ),
        SourcePreset(
            name = "Австралия (SpeedCam.com.au)",
            description = "Данные правительственных источников Австралии. Бесплатно.",
            url1 = "https://www.speedcam.com.au/data/cameras.json"
        )
    )
}
