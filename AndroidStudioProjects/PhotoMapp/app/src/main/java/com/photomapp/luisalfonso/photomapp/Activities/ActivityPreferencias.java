package com.photomapp.luisalfonso.photomapp.Activities;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

import com.photomapp.luisalfonso.photomapp.R;

/**
 * Clase ActivityPreferencias: muestra las preferencias al usuario y le permite cambiarlas.
 */
public class ActivityPreferencias extends AppCompatActivity {

    //Macros
    //Mantener sincronizados los nombres de los keys con el archivo Strings
    public static final String PREFERENCIA_UTILIZAR_FLASH_KEY = "usar_flash";
    public static final String PREFERENCIA_UTILIZAR_GPS_KEY = "utilizar_gps";
    public static final String PREFERENCIA_AUTONOMBRAR_FOTO_KEY = "autonombrar_fotos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Se utiliza el SettingsFragment para mostrar las preferencias
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    /**
     * Clase SettingsFragment: extiende de PreferenceFragment y solo se le infla el menu de configuraciones.
     */
    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //Se configuran las preferencias en el xml/configuraciones
            addPreferencesFromResource(R.xml.configuraciones);
        }

    }

}
