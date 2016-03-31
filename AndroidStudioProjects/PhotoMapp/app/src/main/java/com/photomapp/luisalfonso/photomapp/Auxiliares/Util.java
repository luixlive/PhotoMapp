package com.photomapp.luisalfonso.photomapp.Auxiliares;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.photomapp.luisalfonso.photomapp.data.ContratoPhotoMapp;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
        if (obtenerEscrituraPosible()) {
            //Obtenemos el directorio de fotogragias con el nombre de la app
            File directorio = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    NOMBRE_ALBUM_FOTOS);
            if (!directorio.mkdirs() && !directorio.exists()) {
                Log.w(LOG_TAG, "No se pudo crear el directorio PhotoMapp en el dispositivo");
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
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
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
        if (!obtenerEscrituraPosible()) {
            if (!Environment.MEDIA_MOUNTED_READ_ONLY.
                    equals(Environment.getExternalStorageState())) {
                Log.w(LOG_TAG, "No se puede leer en el almacenamiento externo");
                return false;
            }
        }
        return true;
    }

    /**
     * obtenerIdsImagenes: Regresa todos los ids registrados en el Content Provider
     * @param cr ContentResolver
     * @return ArrayList<Integer> con todos los ids
     */
    public static ArrayList<Integer> obtenerIdsImagenes(ContentResolver cr){
        String projection[] = { ContratoPhotoMapp.Fotos._ID };
        Cursor cursor = cr.query(
                ContratoPhotoMapp.Fotos.CONTENT_URI,
                projection,
                null,
                null,
                null
        );

        ArrayList<Integer> ids = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getInt(cursor.getColumnIndex(ContratoPhotoMapp.Fotos._ID)));
            }
            cursor.close();
        } else {
            Log.w(LOG_TAG, "El Content Provider regreso cursor nulo al intentar obtener los ids");
        }
        return ids;
    }

    /**
     * obtenerNombrePorId: Obtiene el nombre de una imagen a partir de su id en el ContentProvider.
     * @param id int ID de la imagen en el CP
     * @param cr ContentResolver
     * @return String nombre de la imagen o null si no se encontro
     */
    public static String obtenerNombrePorId(int id, ContentResolver cr){
        String projection[] = {
                ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE
        };
        String clause = ContratoPhotoMapp.Fotos._ID + " = ?";
        String[] args = { String.valueOf(id) };
        Cursor cursor = cr.query(
                ContratoPhotoMapp.Fotos.CONTENT_URI,
                projection,
                clause,
                args,
                null
        );

        String nombre = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                nombre = cursor.getString(
                        cursor.getColumnIndex(ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE));

            } else {
                Log.w(LOG_TAG, "No se encontró la imgen de id: " + id);
            }
            cursor.close();
        }
        return nombre;
    }

    /**
     * obtenerImagenPorId: Obtiene objetos ImagenPhotoMapp  a partir del id haciendo querys al
     * Content Provider.
     * @param id int id de la imagen que se desea obtener
     * @param cr ContentResolver
     * @return ImagenPhotoMapp de la imagen o null si no se encontro
     */
    public static ImagenPhotoMapp obtenerImagenPorId(int id, ContentResolver cr){
        //Hacemos un query para obtener las especificaciones de la foto con el id dado
        String projection[] = {
                ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE,
                ContratoPhotoMapp.Fotos.COLUMNA_FECHA,
                ContratoPhotoMapp.Fotos.COLUMNA_LATITUD,
                ContratoPhotoMapp.Fotos.COLUMNA_LONGITUD
        };
        String clause = ContratoPhotoMapp.Fotos._ID + " = ?";
        String[] args = { String.valueOf(id) };
        Cursor cursor = cr.query(
                ContratoPhotoMapp.Fotos.CONTENT_URI,
                projection,
                clause,
                args,
                null
        );

        //Creamos un objeto ImagenPhotoMapp con las especificaciones
        ImagenPhotoMapp informacion_imagen = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String nombre = cursor.getString(
                        cursor.getColumnIndex(ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE));
                String fecha = cursor.getString(
                        cursor.getColumnIndex(ContratoPhotoMapp.Fotos.COLUMNA_FECHA));
                double latitud = cursor.getDouble(
                        cursor.getColumnIndex(ContratoPhotoMapp.Fotos.COLUMNA_LATITUD));
                double longitud = cursor.getDouble(
                        cursor.getColumnIndex(ContratoPhotoMapp.Fotos.COLUMNA_LONGITUD));
                informacion_imagen = new ImagenPhotoMapp(id, nombre, fecha, latitud, longitud);

            } else {
                Log.w(LOG_TAG, "No se encontró la imgen de id: " + id);
            }
            cursor.close();
        }
        return informacion_imagen;
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
                    File archivo = new File(obtenerRutaArchivoImagen(nombre));
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

    /**
     * obtenerRutaArchivoImagen: Obtiene la ruta del almacenamiento donde deberia* ubicarse la
     * imagen con el nombre dado.
     * @param nombre String nombre de la imagen
     * @return String de la ruta donde deberia estar la imagen
     */
    public static String obtenerRutaArchivoImagen(String nombre){
        return obtenerDirectorioFotos().getPath() + File.separator + nombre +
                EXTENSION_ARCHIVO_FOTO;
    }

}