package com.photomapp.luisalfonso.photomapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.Environment;
import android.util.Log;

import com.photomapp.luisalfonso.photomapp.data.ContratoPhotoMapp;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Clase Util: Funciones que se utilizan en multiples partes de la app.
 */
public class Util {

    //Macros
    private static final String LOG_TAG = "Util";
    public static final String NOMBRE_ALBUM_FOTOS = "PhotoMapp";
    public static final String EXTENSION_ARCHIVO_FOTO = ".jpg";

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
     * obtenerNombreCiudad: regresa el nombre de la ciudad ubicada en las coordenadas indicadas.
     * @param context Context de la aplicacion de donde se llama.
     * @param latitud double latitud de la ubicacion.
     * @param longitud double longitud de la ubicacion.
     * @return String con el nombre de la ciudad.
     */
    public static String obtenerNombreCiudad(Context context, double latitud, double longitud){
        //Usamos la clase Geocoder para obtener un objeto Address
        Geocoder localizador = new Geocoder(context);
        String ciudad = null;
        try {
            //Leemos la locacion de la direccion
            List<Address> lista_direccion = localizador.getFromLocation(latitud, longitud, 1);
            ciudad = lista_direccion.get(0).getLocality();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ciudad;
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

    /**
     * obtenerDirectorioFotos: verifica que sea posible guardar datos en el almacenamiento externo y regresa el directorio donde
     * se guardaran las fotos.
     * @return File con el archivo donde se almacenan las fotos, si no es posible leer ni guardar regresa null
     */
    public static File obtenerDirectorioFotos() {
        String estado_almacenamiento = Environment.getExternalStorageState();
        //Guardamos en sus variables respectivas si podemos escribir y leer del almacenamiento
        if (Environment.MEDIA_MOUNTED.equals(estado_almacenamiento)) {
            //Obtenemos el directorio de fotogragias con el nombre de la app
            File directorio = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    NOMBRE_ALBUM_FOTOS);
            if (!directorio.mkdirs()) {
                Log.e(LOG_TAG, "No se pudo crear el directorio.");
            }
            return directorio;
        }
        return null;
    }

    public static boolean obtenerEscrituraPosible() {
        String estado_almacenamiento = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(estado_almacenamiento)){
            return true;
        }
        Log.w(LOG_TAG, "No se puede escribir en el almacenamiento externo.");
        return false;
    }

    public static boolean obtenerLecturaPosible() {
        String estado_almacenamiento = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(estado_almacenamiento)) {
            if (!Environment.MEDIA_MOUNTED_READ_ONLY.equals(estado_almacenamiento)) {
                Log.w(LOG_TAG, "No se puede leer en el almacenamiento externo.");
                return false;
            }
        }
        return true;
    }
}
