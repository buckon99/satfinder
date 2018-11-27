package com.jbuckon.satfinder

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.jbuckon.satfinder.ar.SatFinderAndroidActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.settings_fragment.*
import kotlinx.android.synthetic.main.settings_fragment.view.*


class SettingsFragment : Fragment() {


    var listener: View.OnClickListener? = null

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
