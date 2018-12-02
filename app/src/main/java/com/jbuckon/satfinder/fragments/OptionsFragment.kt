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
import kotlinx.android.synthetic.main.settings_fragment.view.*

class OptionsFragment : Fragment() {


    var listener: View.OnClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        var f = inflater.inflate(R.layout.settings_fragment, container, false)
        f.satButton.setOnClickListener(listener)
        return f
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO: Use the ViewModel
    }
    fun setOnClickListener(listen: View.OnClickListener) {
        this.listener = listen
    }

}
