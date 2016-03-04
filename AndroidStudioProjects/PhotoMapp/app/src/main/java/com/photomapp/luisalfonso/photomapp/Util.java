package com.photomapp.luisalfonso.photomapp;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by LUIS ALFONSO on 26/02/2016.
 */
public class Util {

    public static String obtenerFecha(String formato){
        SimpleDateFormat formato_fecha = new SimpleDateFormat(formato, Locale.US);
        String fecha = formato_fecha.format(Calendar.getInstance().getTime());
        return fecha;
    }

}
