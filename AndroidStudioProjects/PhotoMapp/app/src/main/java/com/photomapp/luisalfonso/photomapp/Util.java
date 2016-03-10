package com.photomapp.luisalfonso.photomapp;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;

import com.photomapp.luisalfonso.photomapp.data.ContratoPhotoMapp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Clase Util: Funciones que se utilizan en multiples partes de la app.
 */
public class Util {

    /**
     * obtenerFecha: regresa la fecha actual en el formato especificado.
     * @param formato String con el formato de la fecha
     * @return fecha actual con ese formato
     */
    public static String obtenerFecha(String formato){
        SimpleDateFormat formato_fecha = new SimpleDateFormat(formato, Locale.US);
        return formato_fecha.format(Calendar.getInstance().getTime());
    }

    /**
     * imprimirBD: Imprime la Base de datos en el LOG.
     * @param cr ContentResolver del contexto de donde se llama la funcion
     */
    public static void imprimirBD(ContentResolver cr){
        String[] projection = {
                ContratoPhotoMapp.Fotos._ID,
                ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE,
                ContratoPhotoMapp.Fotos.COLUMNA_FECHA,
                ContratoPhotoMapp.Fotos.COLUMNA_LATITUD,
                ContratoPhotoMapp.Fotos.COLUMNA_LONGITUD
        };
        Cursor cursor = cr.query(ContratoPhotoMapp.Fotos.CONTENT_URI, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                Log.v("IMPRESION BD:", cursor.getInt(0) + " " + cursor.getString(1) + " " + cursor.getString(2) + " " +
                        cursor.getDouble(3) + " " + cursor.getDouble(4));
                cursor.moveToNext();
            }
            cursor.close();
        }
    }

}
