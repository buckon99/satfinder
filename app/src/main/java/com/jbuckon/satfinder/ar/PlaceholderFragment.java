package com.jbuckon.satfinder.ar;

/**
 * Created by lutusp on 10/1/17.
 */

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jbuckon.satfinder.R;

/**
 * A placeholder fragment containing a simple view.
 */
final public class PlaceholderFragment extends Fragment {

    public PlaceholderFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_skyview, container, false);
    }
}
