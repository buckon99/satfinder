package com.jbuckon.satfinder.models

import android.arch.lifecycle.ViewModel
import java.util.ArrayList
import java.util.HashMap

data class SatSource (
        var id: String = "-1",
        var name: String = "",
        var tle_url: String = ""
)

class SatelliteSourceViewModel : ViewModel() {

    var sources = ArrayList<SatSource>()
    var sourceMap = HashMap<String, SatSource?>()

    fun clear() {
        this.sources.clear()
    }
    fun get(id: String): SatSource? {
        return sourceMap[id]
    }
    fun remove(sat: SatSource) {
        sourceMap[sat.id] = null
        sources.remove(sat)
    }
    fun add(sat: SatSource) {
        sourceMap[sat.name] = sat
        sources.add(sat)
    }
}