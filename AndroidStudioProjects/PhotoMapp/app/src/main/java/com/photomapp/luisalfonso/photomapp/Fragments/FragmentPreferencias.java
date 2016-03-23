package com.photomapp.luisalfonso.photomapp.Fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.photomapp.luisalfonso.photomapp.R;

/**
 * Clase SettingsFragment: extiende de PreferenceFragment y solo se le infla el menu de configuraciones.
 */
public class FragmentPreferencias extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Se configuran las preferencias en el xml/configuraciones
        addPreferencesFromResource(R.xml.configuraciones);
    }

}

