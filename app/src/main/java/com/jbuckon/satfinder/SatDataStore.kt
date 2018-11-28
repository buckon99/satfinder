package com.jbuckon.satfinder;

import android.app.Activity
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v7.widget.RecyclerView
import com.google.firebase.database.*
import com.jbuckon.satfinder.SatDataStore.satSourceViewModel
import com.jbuckon.satfinder.SatDataStore.satViewModel
import com.jbuckon.satfinder.models.SatSource
import com.jbuckon.satfinder.models.SatelliteViewModel
import com.jbuckon.satfinder.models.Satellite
import com.jbuckon.satfinder.models.SatelliteSourceViewModel
import kotlinx.android.synthetic.main.fragment_satellite_list.*

object SatDataStore {

    var counter = 0
    var satViewModel = SatelliteViewModel()
    var satSourceViewModel = SatelliteSourceViewModel()

    private var satSrcRef: DatabaseReference? = null
    private var satRef: DatabaseReference? = null

    fun initFirebase(context: Context, lifeCycleOwner: LifecycleOwner, recycler: RecyclerView?) {
        val database = FirebaseDatabase.getInstance()

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if(prefs.getBoolean("firstRun", true)) {
            database.setPersistenceEnabled(true)
            prefs.edit().putBoolean("firstRun", false).apply()
        }
        satViewModel.liveSats.observe(lifeCycleOwner, Observer<List<Satellite>>{ sat ->
            print(sat)
            recycler?.adapter?.notifyDataSetChanged()
        })

        satSrcRef = database.getReference("satData")
        satRef = database.getReference("statusData")

        satRef?.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.run {
                    val satellites = children.mapNotNull { it.getValue(Satellite::class.java) }
                    satViewModel.update(satellites)
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
                        Thread {
                            for (source in satSourceViewModel.sources) {
                                satSourceViewModel.add(source)
                                satViewModel.CreateSatellites(source, satRef)
                            }
                            locked = false
                        }.start()
                    }*/


                }
            }
            override fun onCancelled(p0: DatabaseError) {
                print("error")
            }
        })
    }
    private var locked = false

    fun toggleSat(sat: Satellite) {
        satRef?.child(sat.name)?.setValue(sat)
    }

}
