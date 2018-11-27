package com.jbuckon.satfinder.models

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
    var enabledSatellites = ArrayList<Satellite>()
    var satelliteMap = HashMap<String, Satellite?>()

    fun CreateSatellites(src: SatSource, satRef: DatabaseReference?) {
        Log.d("SatFinder", "attempting to add satellites...")
        var tles = URL(src.tle_url).readText().split("\n")
        for(i in 0 until (tles.count())/3 - 1) { //each TLE is 3 lines
            try {
                val tleStr = tles[i*3] + "\n" + tles[i*3+1] + "\n" + tles[i*3+2]
                val tleArr= arrayOf(tles[i*3], tles[i*3+1], tles[i*3+2])
                val tleObj = TLE(tleArr)
                var sat: Satellite
                if(tleObj.name != null && enabledSatellites.any { t-> t.name == tleObj.name }){

                    var passPredict = PassPredictor(tleObj, GroundStationPosition(35.28, -120.66, 3.0))//TODO: don't hardcode this, get location from user
                    val pos = passPredict.getSatPos(Date())
                    var nextPass = passPredict.getPasses(Date(), 12, false)

                    sat = Satellite((SatDataStore.counter + 1).toString(), tleObj.name, tleStr)
                    if(nextPass.count() != 0) {
                        sat.next_pass = SimpleDateFormat("MM/dd/yy hh:mm a").format(nextPass[0].startTime)
                        sat.max_elevation = DecimalFormat("#.##").format(nextPass[0].maxEl)
                        sat.lat = pos.calcLatitude
                        sat.lon = pos.calcLongitude
                        sat.azimuth = pos.calcAzimuth
                        sat.elevation = pos.calcElevation

                    }
                }else{
                    sat = Satellite((SatDataStore.counter +1).toString(), tleObj.name, tleStr)
                    sat.is_enabled = false
                }
                sat.name = sat.name.replace(".", ".").replace("$", "") //characters in the id that break firebase
                satRef?.child(sat.name)?.setValue(sat)
                add(sat)

            } catch(e: Exception) {

                Log.d("SatFinder", "error adding satellite")
                //TODO: error handling
            }
        }
        Log.d("SatFinder", "finished...added " + satellites.count() + "out of " + tles.count()/3)
    }

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
        satelliteMap[sat.name] = sat
        if(sat.is_enabled && !satellites.any { s -> s.name == sat.name}){
            enabledSatellites.add(sat)
        }else if (!satellites.any { s -> s.name == sat.name}) {
            enabledSatellites.remove(sat)
        }
    }

    fun add(sat: Satellite) {
        set(sat)
        if(satellites.any { s -> s.name == sat.name})
            return

        var id = sat.id.toInt()

        if(id > SatDataStore.counter)
            SatDataStore.counter = id

        if(sat.is_enabled)
            enabledSatellites.add(sat)
        satellites.add(sat)
    }
}