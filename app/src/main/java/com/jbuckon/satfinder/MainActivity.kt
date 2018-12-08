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
import com.jbuckon.satfinder.ar.SatFinderAndroidActivity
import com.jbuckon.satfinder.fragments.SatelliteFragment
import com.jbuckon.satfinder.fragments.HomeFragment
import com.jbuckon.satfinder.fragments.TrackSatelliteFragment
import com.jbuckon.satfinder.fragments.OptionsFragment
import com.jbuckon.satfinder.models.Satellite
import kotlinx.android.synthetic.main.fragment_enablesatellite_list.*

/*
    MILESTONE 4 PROJECT NOTES
    =========================================================================

    Resources:

    ar app that the code from the package com.jbuckon.satfinder.ar is based off of: https://arachnoid.com/android/SatFinderAndroid/index.html
        - original library was for tracking geostationary satellites, repurposed to track moving satellites
    prediction library that com.jbuckon.satfinder.predict is a modified version of: https://github.com/g4dpz/predict4java

    Milestone 4 Notes:

    - For AR Tracking of Satellites, I decided not to go with AR Core because after researching and testing the Library,
    I realized it was not the right use case. Instead, for the AR functionality I modified code from an open source app that
    projects images over the camera at a given azimuth and elevation using the gyroscope and compass of a device.

    - parsing and calculating pass schedule is cpu intensive so there is a bit of a delay on launch before map & list view populates.
      the delay is worse with the more objects being tracked. the recommended number is under 10

    - app has SLO location hardcoded, will not work elsewhere

    Known Issue:

    - In the emulator, sometimes the AR activity doesn't immediately load when clicking on the satellite in the list view.
    After clicking on a card in the list view few times, the AR view should load. this problem only appears on emulators

 */
class MainActivity : AppCompatActivity(), TrackSatelliteFragment.OnTrackListFragmentInteractionListener,
    SatelliteFragment.OnListFragmentInteractionListener, HomeFragment.OnFragmentInteractionListener, OptionsFragment.OpenFragmentListener{
    override fun openFragment() {
        intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    override fun openDataSourceFragment() {
        intent = Intent(this, DataSourceOptionsActivity::class.java)
        startActivity(intent)
    }

    override fun openFeedbackFragment() {

    }
    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented")
    }

    override fun onTrackListFragmentInteraction(item: Satellite?) {
        intent = Intent(this, SatFinderAndroidActivity::class.java)
        if(item != null) {
            intent.putExtra("TLE", item.TLE)
            intent.putExtra("name", item.name)
        }

        startActivity(intent)
    }
    override fun onListFragmentInteraction(item: Satellite?) {

    }

    private lateinit var satelliteFrag : SatelliteFragment
    private lateinit var optionsFragment: OptionsFragment
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
        satelliteFrag = SatelliteFragment.newInstance(satDataStore)
        optionsFragment = OptionsFragment.newInstance(satDataStore, this)

        val run = Runnable {
            satTrackFrag.trackList.adapter?.notifyDataSetChanged()
            if(satelliteFrag.isVisible)
                satelliteFrag.satList.adapter?.notifyDataSetChanged()
        }
        satDataStore.initFirebase(this, this, homeFragment, run)
        satDataStore.UpdateLoop()

        //TODO: wait for both event listeners, then calculate pass schedule
        /*settings.setOnClickListener(View.OnClickListener{
            viewPager.setCurrentItem(3, false)
        })*/
        pagerAdapter = PagerAdapter(supportFragmentManager, arrayListOf(homeFragment, satTrackFrag, optionsFragment))

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
        viewPager.offscreenPageLimit = 3
        viewPager.adapter = pagerAdapter
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }
}
