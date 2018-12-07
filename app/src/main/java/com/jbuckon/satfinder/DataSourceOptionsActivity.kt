package com.jbuckon.satfinder

import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.support.v14.preference.PreferenceFragment
import android.support.v4.app.NavUtils
import android.text.TextUtils
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jbuckon.satfinder.R.id.source
import com.jbuckon.satfinder.models.Satellite
import com.jbuckon.satfinder.models.SatSource
import java.net.HttpURLConnection
import java.net.URL


/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 * See [Android Design: Settings](http://developer.android.com/design/patterns/settings.html)
 * for design guidelines and the [Settings API Guide](http://developer.android.com/guide/topics/ui/settings.html)
 * for more information on developing a Settings UI.
 */
class DataSourceOptionsActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()
        val database = FirebaseDatabase.getInstance()
        val sourceRef = database.getReference("satSources")
        val satellites = database.getReference("satellites")
        val existingSources = arrayListOf<SatSource>()

        val context = this
        val screen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen = screen

        var index = 0
        sourceRef.addValueEventListener(object:  ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.run {
                    val screen = preferenceManager.createPreferenceScreen(context)
                    preferenceScreen = screen
                    val sources = children.mapNotNull { it.getValue(SatSource::class.java) }

                    var cat = PreferenceCategory(context)
                    cat.title = "Options"
                    screen.addPreference(cat)
                    var button : Preference? = null
                    button = Preference(context)
                    button.key = "update"
                    button.title = "Update Satellites From Sources"
                    button.setOnPreferenceClickListener {
                        Toast.makeText(context, "Pulling Data...", Toast.LENGTH_SHORT).show()
                        try{
                            Thread{
                                try{
                                    for(source in sources) {
                                        val path = URL( source.tle_url)
                                        val c = path.openConnection() as HttpURLConnection
                                        val data = c.inputStream.bufferedReader().readText()
                                        val lines = data.split("\n")
                                        for(i in 0 until lines.count()/3) {
                                            val tle = lines[i*3] + "\n" + lines[i*3 + 1] + "\n" + lines[i*3 + 2]
                                            val sat = Satellite(lines[i*3].replace("$", "").replace(".", ""), tle, false)
                                            val result = HashMap<String, Any>()
                                            result["name"] = sat.name
                                            result["tle"] = tle
                                            satellites.child(sat.name).updateChildren(result)
                                        }
                                    }

                                    runOnUiThread{
                                        Toast.makeText(context, "Update Successful", Toast.LENGTH_SHORT).show()
                                    }

                                    //todo add satellites to firebase
                                }catch (e: Exception){
                                    runOnUiThread{
                                        Toast.makeText(context, "Error Getting Updates", Toast.LENGTH_SHORT).show()
                                    }
                                }

                            }.start()
                        }catch (e: Exception) {
                            Toast.makeText(context, "Error in update thread", Toast.LENGTH_SHORT).show()
                        }

                        true
                    }
                    cat.addPreference(button)

                    val edit = Preference(context)
                    edit.key = "add"
                    edit.title = "Add TLE Source"

                    edit.setOnPreferenceClickListener {

                        val builder = AlertDialog.Builder(context)

                        val positiveClick = DialogInterface.OnClickListener { dialog, id ->
                            val name = ((dialog as AlertDialog).findViewById(R.id.name) as EditText).text
                            val url = ((dialog as AlertDialog).findViewById(R.id.source) as EditText).text
                            val src = SatSource(index.toString(), name.toString(), url.toString())
                            try{
                                Thread{
                                    try{
                                        val path = URL( url.toString())
                                        var c = path.openConnection() as HttpURLConnection
                                        var data = c.inputStream.bufferedReader().readText()
                                        val lines = data.split("\n")
                                        for(i in 0 until lines.count()/3) {
                                            var tle = lines[i*3] + "\n" + lines[i*3 + 1] + "\n" + lines[i*3 + 2]
                                            val sat = Satellite(lines[i*3], tle, false)
                                            satellites.child(sat.name).setValue(sat)
                                        }

                                        sourceRef.child(src.id).setValue(src)
                                        runOnUiThread{
                                            Toast.makeText(context, "Data Source Added Successfully", Toast.LENGTH_SHORT).show()
                                        }

                                        //todo add satellites to firebase
                                    }catch (e: Exception){
                                        runOnUiThread{
                                            Toast.makeText(context, "Error adding Data Source", Toast.LENGTH_SHORT).show()
                                        }


                                        //Toast error
                                    }

                                }.start()
                            }catch (e: Exception) {
                                Toast.makeText(context, "Error adding Data Source", Toast.LENGTH_SHORT).show()

                            }
                        }
                        // Inflate and set the layout for the dialog
                        // Pass null as the parent view because its going in the dialog layout
                        builder.setView(layoutInflater.inflate(R.layout.dialog_add_source, null))
                            // Add action buttons
                            .setPositiveButton("Submit", positiveClick)
                            .setNegativeButton("Cancel", { dialog, id ->
                                    dialog.cancel()
                                })
                        builder.create()
                        var dialog = builder.show()
                        true
                    }
                    cat.addPreference(edit)

                    cat = PreferenceCategory(context)
                    cat.title = "Sources"
                    screen.addPreference(cat)
                    for(source in sources)
                    {
                        val existingSource = existingSources.firstOrNull { x -> x.name == source.name }

                        if(index <= source.id.toInt()) {
                            index = source.id.toInt() + 1
                        }
                        var checkBox : Preference? = null
                        if(existingSource == null){
                            checkBox = Preference(context)
                            checkBox.key = source.name
                            checkBox.title = source.name
                            checkBox.setOnPreferenceClickListener {
                                Toast.makeText(context, source.tle_url, Toast.LENGTH_SHORT).show()
                                true
                            }
                            cat.addPreference(checkBox)
                        }
                    }
                }
            }
            override fun onCancelled(p0: DatabaseError) {
                print("error")
            }
        })


        //
        //r.addPreference(Preference())
        // Bind the summaries of EditText/List/Dialog/Ringtone preferences
        // to their values. When their values change, their summaries are
        // updated to reflect the new value, per the Android Design
        // guidelines.
        //bindPreferenceSummaryToValue(findPreference("example_text"))
        //bindPreferenceSummaryToValue(findPreference("example_list"))
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this)
            }
            return true
        }
        return super.onMenuItemSelected(featureId, item)
    }

    /**
     * {@inheritDoc}
     */
    override fun onIsMultiPane(): Boolean {
        return isXLargeTablet(this)
    }


    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName
                || GeneralPreferenceFragment::class.java.name == fragmentName
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class GeneralPreferenceFragment : PreferenceFragment() {
        override fun onCreatePreferences(p0: Bundle?, p1: String?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, DataSourceOptionsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }

    companion object {

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val listPreference = preference
                val index = listPreference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(
                    if (index >= 0)
                        listPreference.entries[index]
                    else
                        null
                )

            }
            true
        }

        /**
         * Helper method to determine if the device has an extra-large screen. For
         * example, 10" tablets are extra-large.
         */
        private fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
        }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.

         * @see .sBindPreferenceSummaryToValueListener
         */
        private fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(
                preference,
                PreferenceManager
                    .getDefaultSharedPreferences(preference.context)
                    .getString(preference.key, "")
            )
        }
    }
}
