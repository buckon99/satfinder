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
        fun openDataSourceFragment()
        fun openFeedbackFragment()
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
        val dataSources = this.findPreference("sources")
        dataSources.setOnPreferenceClickListener{
            openFragmentListener?.openDataSourceFragment()
            true
        }
        val feedback = this.findPreference("feedback")
        feedback.setOnPreferenceClickListener{
            openFragmentListener?.openFeedbackFragment()
            true
        }
    }

}
