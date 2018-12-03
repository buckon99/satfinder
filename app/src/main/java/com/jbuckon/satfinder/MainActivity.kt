package com.jbuckon.satfinder

import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import com.google.android.gms.maps.SupportMapFragment
import kotlinx.android.synthetic.main.fragment_satellite_list.*
import android.content.Intent
import android.view.View
import com.jbuckon.satfinder.ar.SatFinderAndroidActivity
import com.jbuckon.satfinder.fragments.SatelliteFragment
import com.jbuckon.satfinder.fragments.HomeFragment
import com.jbuckon.satfinder.fragments.TrackSatelliteFragment
import com.jbuckon.satfinder.fragments.OptionsFragment
import com.jbuckon.satfinder.models.Satellite
import kotlinx.android.synthetic.main.fragment_enablesatellite_list.*

/*
    MILESTONE 3 PROJECT NOTES
    =========================================================================

    Resources:

    ar app that the code from the package com.jbuckon.satfinder.ar is based off of:
    prediction library that com.jbuckon.satfinder.predict is a modified version of: https://github.com/g4dpz/predict4java

    Milestone 3 Notes:

    - For AR Tracking of Satellites, I decided not to go with AR Core because after researching and testing the Library,
    I realized it was not the right use case. Instead, for the AR functionality I modified code from an open source app that
    projects images over the camera at a given azimuth and elevation using the gyroscope and compass of a device.

    - in milestone 3 I mentioned my data would come from celestrak, instead I decided to make data sources configurable to
    support other sources as well. the UI is not in the app, however, data sources are pulled from firebase. the only data
    source currently loaded in is http://mstl.atl.calpoly.edu/~ops/keps/kepler.txt which is a filtered version of celestrak's
    data

    - parsing and calculating pass schedule is cpu intensive so there is a bit of a delay on launch before map populates

    - currently the map and AR view do not refresh, but that is functionality that will be in the final app

    - app has SLO location hardcoded, will in future use location permissions to use users' location for AR tracking

    - date/time picking is not yet implemented

    Known Issues:
    - there is a problem I have yet to find a solution for with the home page that causes it to not work after the options page is visited.
    for changes to the options page to take effect, the app has to be restarted.

 */
class MainActivity : AppCompatActivity(), TrackSatelliteFragment.OnTrackListFragmentInteractionListener, SatelliteFragment.OnListFragmentInteractionListener, HomeFragment.OnFragmentInteractionListener{
    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented")
    }

    override fun onTrackListFragmentInteraction(item: Satellite?) {
        intent = Intent(this, SatFinderAndroidActivity::class.java)
        if(item?.sat_position != null) {
            intent.putExtra("azimuth", item.sat_position!!.azimuth)
            intent.putExtra("elevation", item.sat_position!!.elevation!!)
            intent.putExtra("name", item.name)
        }

        startActivity(intent)
    }
    override fun onListFragmentInteraction(item: Satellite?) {

    }

    lateinit var satDataStore: SatDataStore
    private lateinit var pagerAdapter: PagerAdapter
    private var locationManager : LocationManager? = null
    //private var homeFragment: HomeFragment = HomeFragment()


    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                viewPager.setCurrentItem(0, false)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_list -> {
                viewPager.setCurrentItem(1, false)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                viewPager.setCurrentItem(2, false)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onDestroy() {
        super.onDestroy()
        //satDataStore.Clear()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        satDataStore = SatDataStore()
        var homeFragment = SupportMapFragment()
        homeFragment.retainInstance = true //this makes sure markers aren't wiped onRotate or leaving the page and coming back


        val satTrackFrag = TrackSatelliteFragment.newInstance(satDataStore)
        val satelliteFrag = SatelliteFragment.newInstance(satDataStore)
        val settings = OptionsFragment.newInstance(satDataStore)

        val run = Runnable {
            satTrackFrag.trackList.adapter?.notifyDataSetChanged()
            satelliteFrag.satList.adapter?.notifyDataSetChanged()
        }
        satDataStore.initFirebase(this, this, homeFragment, run)
        satDataStore.UpdateLoop()

        //TODO: wait for both event listeners, then calculate pass schedule
        /*settings.setOnClickListener(View.OnClickListener{
            viewPager.setCurrentItem(3, false)
        })*/
        pagerAdapter = PagerAdapter(supportFragmentManager, arrayListOf(homeFragment, satTrackFrag, settings, satelliteFrag))

        /*var listener =  object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }
            override fun onPageSelected(position: Int) {
                if (position == 0) {
                    if(firstLoad){
                        firstLoad = false

                    }
                    mapFragment = SupportMapFragment()
                    var ft: FragmentTransaction = supportFragmentManager.beginTransaction()
                    ft.replace(R.id.map_fragment_container, mapFragment)
                    ft.commit()
                    //TODO Date and Time scheduling functionality
                    dateButton.setOnClickListener {
                        val c = Calendar.getInstance()
                        val year = c.get(Calendar.YEAR)
                        val month = c.get(Calendar.MONTH)
                        val day = c.get(Calendar.DAY_OF_MONTH)
                        val dpd = DatePickerDialog(this@MainActivity, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->

                        }, year, month, day)
                        dpd.show()
                    }
                    refreshButton.setOnClickListener{
                        for(src in sources) {
                            SatDataStore.recompute()

                            runOnUiThread{
                                mapFragment.getMapAsync{
                                    it.clear()
                                    val ref = FirebaseDatabase.getInstance().getReference("statusData")
                                    for(sat in SatDataStore.satViewModel.satellites){
                                        if(!allSatellites.contains(sat)) {
                                            ref.child(sat.name.replace("$", "").replace(".", "")).setValue(sat)
                                        }
                                        if(sat.is_enabled) {
                                            var marker = MarkerOptions().position(LatLng(sat.lat!!, sat.lon!!)).title(sat.name)
                                            it.addMarker(marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.sat)))
                                        }
                                    }
                                }

                                count.text = "tracking " + enabledSatellites.count() + " satellites from " + sources.count() + " sources"
                                list?.adapter?.notifyDataSetChanged()
                            }
                        }
                }
                }
            }

        }
        viewPager?.addOnPageChangeListener(listener)
        viewPager.post{ listener.onPageSelected(viewPager.currentItem) }*/
        viewPager.offscreenPageLimit = 4
        viewPager.adapter = pagerAdapter
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }
}
