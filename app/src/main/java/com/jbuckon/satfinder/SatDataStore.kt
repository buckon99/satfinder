package com.jbuckon.satfinder;

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

object SatDataStore {

    var counter = 0
    var satViewModel = SatelliteViewModel()
    var satSourceViewModel = SatelliteSourceViewModel()

    private var satSrcRef: DatabaseReference? = null
    private var satRef: DatabaseReference? = null

    fun initFirebase(context: Context, recyler: RecyclerView?) {
        val database = FirebaseDatabase.getInstance()

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if(prefs.getBoolean("firstRun", true)) {
            database.setPersistenceEnabled(true)
            prefs.edit().putBoolean("firstRun", false).apply()
        }


        satSrcRef = database.getReference("satData")
        satRef = database.getReference("statusData")

        satRef?.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.run {
                    val satellites = children.mapNotNull { it.getValue(Satellite::class.java) }

                    for (sat in satellites) {
                        satViewModel.add(sat)
                    }
                    recyler?.adapter?.notifyDataSetChanged()
                }
            }
            override fun onCancelled(p0: DatabaseError) {
                print("error")
            }
        })

        satSrcRef?.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.run {
                    var sources = children.mapNotNull { it.getValue(SatSource::class.java) }
                    for (source in sources)
                        queue.add(source)

                }
            }
            override fun onCancelled(p0: DatabaseError) {
                print("error")
            }
        })
        scheduleUpdates()
    }
    private var locked = false
    private var queue = ArrayList<SatSource>()
    private fun scheduleUpdates() {
        if(!locked) {
            locked = true
            Thread {
                if (queue.count() > 0) {
                    var source = queue.get(0)
                    queue.remove(source)
                    satSourceViewModel.add(source)
                    satViewModel.CreateSatellites(source, satRef)
                } else {
                    for (source in satSourceViewModel.sources) {
                        satSourceViewModel.add(source)
                        satViewModel.CreateSatellites(source, satRef)
                    }
                    locked = false
                }
            }.start()
        }

    }

    fun toggleSat(sat: Satellite) {
        satRef?.child(sat.name)?.setValue(sat)
    }

}
