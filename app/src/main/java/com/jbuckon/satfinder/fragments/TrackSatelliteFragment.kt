package com.jbuckon.satfinder.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jbuckon.satfinder.MySatelliteRecyclerViewAdapter
import com.jbuckon.satfinder.R
import com.jbuckon.satfinder.SatDataStore
import com.jbuckon.satfinder.models.Satellite


/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [TrackSatelliteFragment.OnListFragmentInteractionListener] interface.
 */
class TrackSatelliteFragment : Fragment() {

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        fun newInstance(satDataStore: SatDataStore): TrackSatelliteFragment {
            var frag = TrackSatelliteFragment()
            frag.dataStore = satDataStore
            //val args = Bundle()
            //args.putInt("example", position)
            //frag.setArguments(args)
            return frag
        }
    }

    // TODO: Customize parameters
    private var columnCount = 1

    private var listener: OnTrackListFragmentInteractionListener? = null

    private lateinit var dataStore: SatDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_satellite_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }

                if(dataStore.satViewModel.liveSats.value == null)
                    dataStore.satViewModel.liveSats.value = arrayOf()
                adapter = MySatelliteRecyclerViewAdapter(dataStore.satViewModel.enabledSatellites, listener)
            }
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnTrackListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnTrackListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnTrackListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onTrackListFragmentInteraction(item: Satellite?)
    }
}
