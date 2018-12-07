package com.jbuckon.satfinder.fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.preference.PreferenceFragmentCompat
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.jbuckon.satfinder.R
import com.jbuckon.satfinder.SatDataStore





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
            val positiveClick = DialogInterface.OnClickListener { dialog, id ->
                val feedBackText = ((dialog as AlertDialog).findViewById(R.id.feedback) as EditText).text
                val database = FirebaseDatabase.getInstance()
                val sourceRef = database.getReference("feedback")
                val newPostRef = sourceRef.push()
                newPostRef.setValue(feedBackText.toString())

                Toast.makeText(context, "Feedback submitted", Toast.LENGTH_SHORT).show()
            }
            val builder = AlertDialog.Builder(context)

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(layoutInflater.inflate(R.layout.dialog_feedback, null))
                // Add action buttons
                .setPositiveButton("Submit", positiveClick)
                .setNegativeButton("Cancel", { dialog, id ->
                    dialog.cancel()
                })
            builder.create()
            var dialog = builder.show()
            true
        }
    }

}
