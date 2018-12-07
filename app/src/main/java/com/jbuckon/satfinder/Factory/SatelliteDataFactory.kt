package com.jbuckon.satfinder.Factory

import com.jbuckon.satfinder.models.SatPos
import com.jbuckon.satfinder.models.SatSource
import com.jbuckon.satfinder.predict.GroundStationPosition
import com.jbuckon.satfinder.predict.PassPredictor
import com.jbuckon.satfinder.predict.TLE
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class SatelliteDataFactory {

    fun CalcTle(tle: String) : SatPos? {

        var tles = tle.split("\n").toTypedArray()
        val tleObj = TLE(tles)

        var passPredict = PassPredictor(tleObj, GroundStationPosition(35.28, -120.66, 3.0))//TODO: don't hardcode this, get location from user
        val pos = passPredict.getSatPos(Date())
        var nextPass = passPredict.getPasses(Date(), 12, false)

        if (nextPass.count() != 0) {
            return SatPos(
                    SimpleDateFormat("MM/dd/yy hh:mm a", Locale.US).format(nextPass[0].startTime),
                    DecimalFormat("#.##").format(nextPass[0].maxEl),
                    pos.calcLatitude,
                    pos.calcLongitude,
                    pos.calcAzimuth,
                    pos.calcElevation
            )
        }
        return null
    }

    fun CalcFuturePos(tle: String) : Array<SatPos> {
        val satpos = arrayListOf<SatPos>()
        var tles = tle.split("\n").toTypedArray()
        val tleObj = TLE(tles)

        var passPredict = PassPredictor(tleObj, GroundStationPosition(35.28, -120.66, 3.0))//TODO: don't hardcode this, get location from user

        for(i in 0..10) {
            val cal = Calendar.getInstance()
            cal.time = Date()
            cal.add(Calendar.MINUTE, 3*i)
            val pos = passPredict.getSatPos(cal.time)
            satpos.add(SatPos(
                "",
                "",
                pos.calcLatitude,
                pos.calcLongitude,
                pos.calcAzimuth,
                pos.calcElevation
            ))
        }
        return satpos.toTypedArray()
    }

}
