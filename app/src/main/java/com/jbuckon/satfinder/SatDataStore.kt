package com.jbuckon.satfinder

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.content.Context
import android.graphics.Color
import android.preference.PreferenceManager
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*
import com.jbuckon.satfinder.Factory.SatelliteDataFactory
import com.jbuckon.satfinder.models.SatelliteViewModel
import com.jbuckon.satfinder.models.Satellite
import com.jbuckon.satfinder.models.SatelliteSourceViewModel

class SatDataStore {

    lateinit var satViewModel: SatelliteViewModel
    lateinit var satSourceViewModel: SatelliteSourceViewModel

    private var satSrcRef: DatabaseReference? = null
    private var satRef: DatabaseReference? = null


    private var loop: Boolean = true

    constructor() {
        satViewModel = SatelliteViewModel()
        satSourceViewModel = SatelliteSourceViewModel()
    }
    fun ClearMarkers() {
        for(sat in satViewModel.enabledSatellites.toTypedArray()) {
            sat.marker = null
        }
    }


    fun initFirebase(context: Context?, lifeCycleOwner: LifecycleOwner?, mapFragment: SupportMapFragment?, callback: Runnable?) {

        val database = FirebaseDatabase.getInstance()

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if(prefs.getBoolean("firstRun", true)) {
            database.setPersistenceEnabled(true)
            prefs.edit().putBoolean("firstRun", false).apply()
        }
        var polyline: Polyline? = null
        if(lifeCycleOwner != null) {
            satViewModel.liveSats.observe(lifeCycleOwner, Observer<Array<Satellite>>{ sats ->
                mapFragment?.getMapAsync{
                    val map = it
                    it.setOnMarkerClickListener {
                        polyline?.remove()
                        if(satViewModel.satelliteMap[it.title]?.TLE != null){
                            var markers = SatelliteDataFactory().CalcFuturePos(satViewModel.satelliteMap[it.title]?.TLE!!)
                            val options = PolylineOptions()
                            for(marker in markers) {
                                options.add(LatLng(marker.lat, marker.lon))
                            }
                            options.width(5.0f)
                            options.color(Color.GREEN)
                            polyline = map.addPolyline(options)
                        }

                        it.showInfoWindow()
                        true
                    }
                    for(sat in sats!!){
                        if(sat.is_enabled && sat.sat_position != null) {
                            var pos = LatLng(sat.sat_position!!.lat, sat.sat_position!!.lon)
                            if(sat.marker != null) {
                                sat.marker?.position = pos
                            } else {
                                //create new marker for satellite
                                sat.marker = it.addMarker(MarkerOptions().position(pos).title(sat.name).icon(BitmapDescriptorFactory.fromResource(R.drawable.sat)))

                            }
                        }
                    }
                    val count: Int = sats?.count()!!
                    val sources : Int = satSourceViewModel.sources.count()
                    if(/*sources > 0 && */count > 0)
                    {
                        //count?.text = "tracking " + sats + " satellites from " + sources + " sources"
                        callback?.run()
                    }
                }
            })
        }



        satSrcRef = database.getReference("satSources")
        satRef = database.getReference("satellites")

        /* Initialization source
            satSrcRef?.child("0")?.setValue(SatSource(
            "0",
            "CalPoly CubeSat Lab",
            "http://mstl.atl.calpoly.edu/~ops/keps/kepler.txt"

        ))*/

        satRef?.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.run {
                    val satellites = children.mapNotNull { it.getValue(Satellite::class.java) }
                    satViewModel.update(satellites, firebase = true)
                }
            }
            override fun onCancelled(p0: DatabaseError) {
                print("error")
            }
        })

        satSrcRef?.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.run {
                    /*var sources = children.mapNotNull { it.getValue(SatSource::class.java) }
                    for (source in sources){
                        satSourceViewModel.add(source)
                        Thread{
                            for (source in satSourceViewModel.sources) {
                                satSourceViewModel.add(source)
                                var arr = SatelliteDataFactory().GetSatellites(source)
                                for(sat in arr) {
                                    satViewModel.add(sat)
                                    satRef?.child(sat.name)?.setValue(sat)
                                }
                            }
                        }.start()
                    }*/
                }
            }
            override fun onCancelled(p0: DatabaseError) {
                print("error")
            }
        })
    }

    fun Clear() {
        satViewModel.clear()
        loop = false

    }
    fun UpdateLoop() {
        loop = true
        Thread{
            do{

                val array = arrayOfNulls<Satellite>( satViewModel.enabledSatellites.size)
                satViewModel.enabledSatellites.toArray(array)
                for(sat in array) {

                    if(sat != null) {
                        sat.sat_position = SatelliteDataFactory().CalcTle(sat.TLE)
                        satViewModel.setPos(sat)
                    }
                }
                Thread.sleep(500)//make sure to not update to fast as to not drain battery
            }while(loop)
        }.start()
    }


    fun toggleSat(sat: Satellite) {
        if(!sat.is_enabled) {
            satViewModel.set(sat)
        }

        satRef?.child(sat.name)?.setValue(sat)
    }

}
