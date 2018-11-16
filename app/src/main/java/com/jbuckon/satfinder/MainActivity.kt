package com.jbuckon.satfinder

import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import android.preference.PreferenceManager
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_satellite_list.*
import android.content.Intent
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.jbuckon.satfinder.ar.SatFinderAndroidActivity


/*
    MILESTONE 3 PROJECT NOTES
    =========================================================================

    For AR Tracking of Satellites, I decided not to go with AR Core because after researching and testing the Library,
    it was not the right use case. Instead, for the AR functionality I used code from an open source app that projects
    images over the camera at a given azimuth and elevation using the gyroscope and compass of a device. I plan to update
    the UI for the AR functionality to something of my own at a later date but for now it works as a good proof of concept.

    ar app that the code from the package com.jbuckon.satfinder.ar is based off of: https://arachnoid.com/android/SatFinderAndroid/index.html
    prediction library that com.jbuckon.satfinder.predict is a modified version of: https://github.com/g4dpz/predict4java



 */
class MainActivity : AppCompatActivity(), SatelliteFragment.OnListFragmentInteractionListener{

    override fun onListFragmentInteraction(item: DataStore.Satellite?) {
        var l = item
        intent = Intent(this, SatFinderAndroidActivity::class.java)
        startActivity(intent)
        finish()
    }

    private var satViewModel: DataStore.SatelliteViewModel = DataStore.SatelliteViewModel(null)
    private lateinit var pagerAdapter: PagerAdapter
    private var enabledSatellites: ArrayList<DataStore.Satellite> = arrayListOf()
    private var allSatellites: ArrayList<DataStore.Satellite> = arrayListOf()

    private var sources: ArrayList<DataStore.SatSource> = arrayListOf()
    private var locationManager : LocationManager? = null
    private var mapFragment: SupportMapFragment = SupportMapFragment()
    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                viewPager.setCurrentItem(0, true)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_list -> {
                viewPager.setCurrentItem(1, false)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                viewPager.setCurrentItem(2, true)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val database = FirebaseDatabase.getInstance()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        /*if(prefs.getBoolean("firstRun", true)) {
            database.setPersistenceEnabled(true)
            prefs.edit().putBoolean("firstRun", false).apply()
        }*/

        val satRef = database.getReference("satData")
        val statusRef = database.getReference("statusData")
        statusRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.run {
                    val satellites = children.mapNotNull { it.getValue(DataStore.Satellite::class.java) }
                    for (sat in satellites) {
                        if(sat.is_enabled)
                            enabledSatellites.add(sat)
                        allSatellites.add(sat)
                    }
                    satRef.addValueEventListener(object: ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.run {
                                val srcs = children.mapNotNull { it.getValue(DataStore.SatSource::class.java) }
                                for (src in srcs) {
                                    if(!sources.contains(src)){
                                        println(src)
                                        sources.add(src)

                                        Thread {
                                            var sats = DataStore.createSatellites(src)
                                            runOnUiThread{
                                                mapFragment.getMapAsync{

                                                    val ref = FirebaseDatabase.getInstance().getReference("statusData")
                                                    for(sat in sats){
                                                        if(!allSatellites.contains(sat)) {
                                                            ref.child(sat.name.replace("$", "").replace(".", "")).setValue(sat)
                                                        }
                                                        if(sat.is_enabled) {
                                                            var marker = MarkerOptions().position(LatLng(sat.lat!!, sat.lon!!)).title(sat.name)
                                                            it.addMarker(marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.sat)))
                                                        }
                                                    }
                                                }
                                                list?.adapter?.notifyDataSetChanged()
                                            }
                                        }.start()
                                    }
                                }
                            }
                        }
                        override fun onCancelled(p0: DatabaseError) {
                            print("error")
                        }
                    })
                }
            }
            override fun onCancelled(p0: DatabaseError) {
                print("error")
            }
        })

        //TODO: wait for both event listeners, then calculate pass schedule
        pagerAdapter = PagerAdapter(supportFragmentManager, arrayListOf(mapFragment, SatelliteFragment(), SettingsFragment()))
        viewPager.adapter = pagerAdapter
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }
}
