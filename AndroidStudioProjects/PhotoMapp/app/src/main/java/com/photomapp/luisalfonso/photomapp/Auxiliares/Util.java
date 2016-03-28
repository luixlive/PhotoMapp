package com.photomapp.luisalfonso.photomapp.Auxiliares;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
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
     * obtenerNombreCiudad: regresa la direccion de la bicacion especificada.
     * @param context Context de la aplicacion de donde se llama.
     * @param ubicacion LatLng de la ubicacion
     * @return Address con la direccion.
     */
    public static Address obtenerDireccion(Context context, LatLng ubicacion){
        //Usamos la clase Geocoder para obtener un objeto Address
        Geocoder localizador = new Geocoder(context);
        double latitud = ubicacion.latitude;
        double longitud = ubicacion.longitude;
        try {
            //Regresamos la direccion
            List<Address> lista_direccion = localizador.getFromLocation(latitud, longitud, 1);
            return lista_direccion.get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * imprimirBD: Imprime la Base de datos en el LOG.
     * @param cr ContentResolver del contexto de donde se llama la funcion
     */
    /*public static void imprimirBD(ContentResolver cr){
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
                Log.v("IMPRESION BD:", cursor.getInt(0) + " " + cursor.getString(1) + " " +
                        cursor.getString(2) + " " +
                        cursor.getDouble(3) + " " + cursor.getDouble(4));
                cursor.moveToNext();
            }
            cursor.close();
        }
    }*/

    /**
     * obtenerDirectorioFotos: verifica que sea posible guardar datos en el almacenamiento externo y
     * regresa el directorio donde se guardaran las fotos.
     * @return File con el archivo donde se almacenan las fotos, si no es posible obtenerlo, null
     */
    public static File obtenerDirectorioFotos() {
        //Guardamos en sus variables respectivas si podemos escribir y leer del almacenamiento
        if (obtenerEscrituraPosible()) {
            //Obtenemos el directorio de fotogragias con el nombre de la app
            File directorio = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    NOMBRE_ALBUM_FOTOS);
            if (!directorio.mkdirs()) {
                Log.w(LOG_TAG, "No se pudo crear el directorio.");
            }
            return directorio;
        }
        return null;
    }

    /**
     * obtenerEscrituraPosible: Se asegura si es posible escribir en el almacenamiento externo.
     * @return true si es posible escribir en el almacenamiento, false de otro modo.
     */
    public static boolean obtenerEscrituraPosible() {
        String estado_almacenamiento = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(estado_almacenamiento)){
            return true;
        }
        Log.w(LOG_TAG, "No se puede escribir en el almacenamiento externo.");
        return false;
    }

    /**
     * obtenerLecturaPosible: Se asegura si es posible leer del almacenamiento del dispositivo.
     * @return true si es posible leer del almacenamiento, false de otro modo.
     */
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

    /**
     * obtenerNombreImagen: Si se quiere obtener el nombre de una imagen de la base de datos y se
     * tiene el id, este metodo checa el contentProvider y regresa solamente el nombre
     * @param id int _ID de la imagen en la BD
     * @param cr ContentResolver valido
     * @return String nombre de la imagen
     */
    public static String obtenerNombreImagen(int id, ContentResolver cr){
        String nombre = null;
        String projection[] = {
                ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE
        };
        String clause = ContratoPhotoMapp.Fotos._ID + " = ?";
        String[] args = {String.valueOf(id)};
        Cursor cursor = cr.query(
                ContratoPhotoMapp.Fotos.CONTENT_URI,
                projection,
                clause,
                args,
                null
        );
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                nombre = cursor.getString(
                        cursor.getColumnIndex(ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE));
            }
            cursor.close();
        }
        return nombre;
    }

    /**
     * obtenerDimensionesFraccionPantalla: Obtiene las dimensiones de altura de la fraccion de la
     * pantalla deseada.
     * @param recursos Resources de la App
     * @param relacion_pantalla int con la relacion de la fraccion deseada
     * @return int valor de altura de la fraccion
     */
    public static int obtenerDimensionesFraccionPantalla(Resources recursos, int relacion_pantalla){
        int pantalla_alto = recursos.getDisplayMetrics().heightPixels;
        return pantalla_alto/relacion_pantalla;
    }

    /**
     * eliminarImagenesAlmacenamiento: Borra fotos del almacenamiento externo en un hilo en segundo
     * plano.
     * @param nombre_foto String[] con los nombres de las fotos a borrar
     */
    public static void eliminarImagenesAlmacenamiento(String[] nombre_foto) {
        new AsyncTask<String[], Void, Boolean>(){

            @Override
            protected Boolean doInBackground(String[]... params) {
                for (String nombre: params[0]) {
                    File archivo = new File(obtenerDirectorioFotos() + File.separator + nombre +
                            EXTENSION_ARCHIVO_FOTO);
                    if (archivo.exists()) {
                        if (!archivo.delete()) {
                            Log.w(LOG_TAG, "No se pudo borrar la foto " + nombre);
                        }
                    }
                }
                return true;
            }

        }.execute(nombre_foto);

    }

}