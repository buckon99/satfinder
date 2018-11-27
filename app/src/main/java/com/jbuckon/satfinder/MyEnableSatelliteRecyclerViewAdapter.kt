package com.jbuckon.satfinder

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView


import com.jbuckon.satfinder.EnableSatelliteFragment.OnListFragmentInteractionListener
import com.jbuckon.satfinder.models.Satellite

import kotlinx.android.synthetic.main.fragment_enablesatellite.view.*

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class MyEnableSatelliteRecyclerViewAdapter(
        private val mValues: List<Satellite>,
        private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<MyEnableSatelliteRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as Satellite
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_enablesatellite, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.mToggle.isChecked = item.is_enabled
        holder.mToggle.setOnClickListener{
            item.is_enabled = !item.is_enabled
            SatDataStore.toggleSat(item)
        }

        holder.mContentView.text = item.name

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mContentView: TextView = mView.content
        var mToggle: Switch = mView.switch1
        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}
