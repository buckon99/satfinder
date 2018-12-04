package com.jbuckon.satfinder.models

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Exclude
import com.jbuckon.satfinder.SatDataStore
import com.jbuckon.satfinder.predict.GroundStationPosition
import com.jbuckon.satfinder.predict.PassPredictor
import com.jbuckon.satfinder.predict.TLE
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*


data class SatPos (
        var next_pass: String = "",
        var max_elevation: String = "",

        var lat: Double,
        var lon: Double,

        var azimuth: Double,
        var elevation: Double
)

data class Satellite (
        var name: String = "",

        var TLE: String = "",
        var is_enabled: Boolean = false,
        @get:Exclude var sat_position: SatPos? = null,
        @get:Exclude var marker: Marker? = null //marker and sat_position change by the second, so there is no point in storing and retrieving them from firebase
)

class SatelliteViewModel : ViewModel() {
    var satellites = ArrayList<Satellite>()
    var liveSats = MutableLiveData<Array<Satellite>>()
    var enabledSatellites = ArrayList<Satellite>()
    var satelliteMap = HashMap<String, Satellite?>()


    fun get(id: String): Satellite? {
        return satelliteMap[id]
    }

    fun clear() {
        this.enabledSatellites.clear()
        this.liveSats.value = null
        this.satellites.clear()
        this.satelliteMap.clear()
    }

    fun remove(sat: Satellite) {
        enabledSatellites.remove(sat)
        satellites.remove(sat)
        satelliteMap[sat.name] = null
    }

    fun setPos(sat: Satellite) {
        satelliteMap[sat.name]?.sat_position = sat.sat_position
        liveSats.postValue(enabledSatellites.toTypedArray())

    }

    fun set(sat: Satellite) {

        satelliteMap[sat.name] = sat
        if(!satellites.any { s -> s.name == sat.name}) { // if satellite does not already exists, add it to list
            satellites.add(sat)
        }


        val existingSat = enabledSatellites.firstOrNull{ x -> x.name == sat.name}
        if(!sat.is_enabled &&  existingSat != null) { //if satellite is disabled, but in enabled array, remove it
            enabledSatellites.remove(existingSat)
            existingSat.marker?.remove()
            existingSat.sat_position = null
            existingSat.marker = null

        } else if(sat.is_enabled && !enabledSatellites.contains(sat)){ //if satellite is set to be enabled but not in enabled array, add it2
            enabledSatellites.add(sat)
        }
        liveSats.value = enabledSatellites.toTypedArray()
    }

    //firebase flag tells the function to not override existing marker and sat_position, since that is not stored in Firebase
    fun update(sats: List<Satellite>, firebase: Boolean = false) {
        for(sat in sats) {
            if(firebase){
                if(satelliteMap[sat.name] != null) {
                    sat.marker = satelliteMap[sat.name]?.marker
                    sat.sat_position = satelliteMap[sat.name]?.sat_position
                }
            }
            if(sat.name=="AAUCUBE2")
                print(sat)
            set(sat)
        }
        liveSats.value = enabledSatellites.toTypedArray()
    }
}