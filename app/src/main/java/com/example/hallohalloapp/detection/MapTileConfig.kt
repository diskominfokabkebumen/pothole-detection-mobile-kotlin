package com.example.hallohalloapp.detection

import org.osmdroid.tileprovider.tilesource.XYTileSource

object MapTileConfig {

    val CARTO_LIGHT = XYTileSource(
        "CartoDBLight",
        0, 20,
        256,
        ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/light_all/",
            "https://b.basemaps.cartocdn.com/light_all/",
            "https://c.basemaps.cartocdn.com/light_all/",
            "https://d.basemaps.cartocdn.com/light_all/"
        )
    )
}