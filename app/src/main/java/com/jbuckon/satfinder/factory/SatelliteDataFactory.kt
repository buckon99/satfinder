package com.jbuckon.satfinder.factory

import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.jbuckon.satfinder.SatDataStore
import com.jbuckon.satfinder.models.SatSource
import com.jbuckon.satfinder.models.Satellite
import com.jbuckon.satfinder.predict.GroundStationPosition
import com.jbuckon.satfinder.predict.PassPredictor
import com.jbuckon.satfinder.predict.TLE
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class SatelliteDataFactory {

    fun GetTLEs(src: SatSource): ArrayList<String> {

        var tleArr = ArrayList<String>()
        var tles = URL(src.tle_url).readText().split("\n")
        for(i in 0 until (tles.count())/3 - 1) { //each TLE is 3 lines
            try {
                tleArr.add(tles[i * 3] + "\n" + tles[i * 3 + 1] + "\n" + tles[i * 3 + 2])
            }catch(e: Exception) {
                Log.d("SatFinder", "error creatign TLE")
            }
        }
        return tleArr
    }

    fun CalcTle(tle: String) : Satellite {

        var tles = tle.split("\n")
        val tleArr= arrayOf(tles[0], tles[1], tles[2])
        val tleObj = TLE(tleArr)

        if(tleObj.name != null && enabledSatellites.any { t-> t.name == tleObj.name }){

            var passPredict = PassPredictor(tleObj, GroundStationPosition(35.28, -120.66, 3.0))//TODO: don't hardcode this, get location from user
            val pos = passPredict.getSatPos(Date())
            var nextPass = passPredict.getPasses(Date(), 12, false)

            sat = Satellite((SatDataStore.counter + 1).toString(), tleObj.name, tleStr)
            if(nextPass.count() != 0) {
                sat.next_pass = SimpleDateFormat("MM/dd/yy hh:mm a", Locale.US).format(nextPass[0].startTime)
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

    }
    fun CreateSatellites(src: SatSource, satRef: DatabaseReference?) {
        Log.d("SatFinder", "attempting to add satellites...")

        for(i in 0 until (tles.count())/3 - 1) { //each TLE is 3 lines
            try {
                val tleStr = tles[i*3] + "\n" + tles[i*3+1] + "\n" + tles[i*3+2]

                var sat: Satellite

                sat.name = sat.name.replace(".", "").replace("$", "") //characters in the id that break firebase
                satRef?.child(sat.name)?.setValue(sat)
                add(sat)

            } catch(e: Exception) {

                Log.d("SatFinder", "error adding satellite")
                //TODO: error handling
            }
        }
        Log.d("SatFinder", "finished...added " + satellites.count() + "out of " + tles.count()/3)
    }

}
