package com.jbuckon.satfinder;

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.v7.widget.RecyclerView
import android.util.Log
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.FirebaseDatabase
import com.jbuckon.satfinder.predict.*
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object DataStore {

    data class SatSource (
            var id: String = "-1",
            var name: String = "",
            var tle_url: String = ""
    )

    data class Satellite (
            var id: String = "-1",
            var name: String = "",
            var TLE: String = "",
            var next_pass: String? = null,
            var max_elevation: String = "",
            var is_enabled: Boolean = false,
            var lat: Double? = null,
            var lon: Double? = null
    )

    var counter = 0
    var satVideModel = SatelliteViewModel(null)
    class SatelliteSourceViewModel(recycleList: RecyclerView?) : ViewModel(){
        var recycle = recycleList
        var sources = ArrayList<SatSource>()
        var sourceMap = HashMap<String, SatSource?>()
        private val ref = FirebaseDatabase.getInstance().getReference("satData")

        fun clear() {
            this.sources.clear()
        }
        fun get(id: String): SatSource? {
            return sourceMap[id]
        }
        fun remove(sat: SatSource) {
            ref.child(sat.name).removeValue()
        }
        fun add(sat: SatSource) {
            sourceMap[sat.name] = sat
            var id = sat.id.toInt()
            if(id > counter)
                counter = id
            sources.add(sat)
            this.recycle?.adapter?.notifyDataSetChanged()
        }
    }

    class SatelliteViewModel(recycleList: RecyclerView?) : ViewModel() {
        var recycle = recycleList
        var satellites = ArrayList<Satellite>()
        var satelliteMap = HashMap<String, Satellite?>()
        private val ref = FirebaseDatabase.getInstance().getReference("statusData")

        fun get(id: String): Satellite? {
                return satelliteMap[id]
        }

        fun clear() {
                this.satellites.clear()
        }
        fun remove(sat: SatSource) {
                ref.child(sat.id).removeValue()
        }
        fun add(sat: Satellite) {
            satelliteMap[sat.name] = sat
            var id = sat.id.toInt()
            if(id > counter)
                counter = id
            satellites.add(sat)
            this.recycle?.adapter?.notifyDataSetChanged()
        }
    }


   fun createSatellites(src: SatSource) : ArrayList<Satellite> {
        val satellites = arrayListOf<Satellite>()
        var tles = URL(src.tle_url).readText().split("\n")
        for(i in 0 until (tles.count())/3 - 1)
        {
            var tle = tles[i*3] + "\n" + tles[i*3+1] + "\n" + tles[i*3+2]
            val arr= arrayOf(tles[i*3], tles[i*3+1], tles[i*3+2])
            val tleObj = TLE(arr)
            try{
                if(tleObj.name != "" && tleObj.name != null){
                    var p = PassPredictor(tleObj, GroundStationPosition(35.28, -120.66, 3.0))//TODO: don't hardcode this
                    val pos = p.getSatPos(Date())


                    var nextPass = p.getPasses(Date(), 12, false)
                    if(nextPass.count() != 0) {
                        var sat = Satellite((counter + 1).toString(), tleObj.name, tle, SimpleDateFormat("MM/dd/yy hh:mm a").format(nextPass[0].startTime), DecimalFormat("#.##").format(nextPass[0].maxEl),
                                true, pos.latitude, pos.longitude)
                        satellites.add(sat)
                        satVideModel.add(sat)
                    }
                }
            } catch(e: Exception) {
                Log.d("SatFinder", "error adding satellite")
                //TODO: error handling
            }
        }
        Log.d("SatFinder", "added satellite")
        return satellites
    }

}
