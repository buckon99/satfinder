package com.jbuckon.satfinder.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jbuckon.satfinder.R
import com.jbuckon.satfinder.SatDataStore
import kotlinx.android.synthetic.main.settings_fragment.view.*
import android.app.Activity




class OptionsFragment : PreferenceFragmentCompat(), PreferenceFragmentCompat.OnPreferenceStartScreenCallback  {

    var openFragmentListener: OpenFragmentListener? = null

    interface OpenFragmentListener {
        fun openFragment()
    }

    override fun onPreferenceStartScreen(preferenceFragmentCompat: PreferenceFragmentCompat?, p1: android.support.v7.preference.PreferenceScreen?): Boolean {
        preferenceFragmentCompat?.preferenceScreen = preferenceScreen
        return true
    }

    companion object {

        @JvmStatic
        fun newInstance(dataStore: SatDataStore, listen: OpenFragmentListener): OptionsFragment {
            val enabled = OptionsFragment()
            enabled.openFragmentListener = listen
            enabled.dataStore = dataStore

            return enabled
        }
    }

    private lateinit var dataStore: SatDataStore

    //needed due to bug in PreferenceFragmentCompat:
    //https://stackoverflow.com/questions/34701740/preference-sub-screen-not-opening-when-using-support-v7-preference/34944339
    override fun getCallbackFragment(): Fragment {
        return this
    }

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val preference = this.findPreference("satellites")
        preference.setOnPreferenceClickListener{
            openFragmentListener?.openFragment()
            true
        }

        /*val satellites = findPreference("satellites")
        satellites.setOnPreferenceClickListener {

        }

        val sources = findPreference("sources")
        sources.setOnPreferenceClickListener {  }*/
        // val entries = dataStore.satViewMode
        // l.enabledSatellites.map{x -> x.name}
        /*var listPreference: ListPreference = findPreference("satellites") as ListPreference
        listPreference.entries = entries.toTypedArray()
        listPreference.entryValues = entries.toTypedArray()
        listPreference.setDefaultValue(entries[0])*/
    }

}
