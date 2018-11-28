package com.jbuckon.satfinder.models

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.jbuckon.satfinder.SatDataStore
import com.jbuckon.satfinder.predict.GroundStationPosition
import com.jbuckon.satfinder.predict.PassPredictor
import com.jbuckon.satfinder.predict.TLE
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

data class Satellite (
        var id: String = "-1",
        var name: String = "",

        var TLE: String = "",
        var is_enabled: Boolean = false,

        var next_pass: String? = null,
        var max_elevation: String = "",

        var lat: Double? = null,
        var lon: Double? = null,

        var azimuth: Double? = null,
        var elevation: Double? = null
)

class SatelliteViewModel : ViewModel() {
    var satellites = ArrayList<Satellite>()
    var liveSats = MutableLiveData<List<Satellite>>()
    var enabledSatellites = ArrayList<Satellite>()
    var satelliteMap = HashMap<String, Satellite?>()


    fun get(id: String): Satellite? {
        return satelliteMap[id]
    }

    fun clear() {
        this.enabledSatellites.clear()
        this.satellites.clear()
    }

    fun remove(sat: Satellite) {
        enabledSatellites.remove(sat)
        satellites.remove(sat)
        satelliteMap[sat.name] = null
    }

    fun set(sat: Satellite) {

        if(!satellites.any { s -> s.name == sat.name})
            satellites.add(sat)

        satelliteMap[sat.name] = sat
        if(sat.is_enabled && !satellites.any { s -> s.name == sat.name}){
            enabledSatellites.add(sat)
        }else if (!satellites.any { s -> s.name == sat.name}) {
            enabledSatellites.remove(sat)
        }
    }

    fun update(sats: List<Satellite>) {
        for(sat in sats) {
            set(sat)
        }
        liveSats.value = satellites
    }
    fun add(sat: Satellite) {
        set(sat)
    }
}