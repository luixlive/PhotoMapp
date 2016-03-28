package com.photomapp.luisalfonso.photomapp.Fragments;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

/**
 * Clase ManejadorCPImagenes: Clase auxiliar para el manejo del Content Provider de la galeria
 * (mediastore) donde se almacenan las imagenes en el dispositivo.
 */
public class ManejadorCPImagenes {

    private static final String LOG_TAG = "ManejadorCPImagenes";

    public static boolean insertarImagen(String ruta_foto, String tipo_mime,
                                                       ContentResolver cr){
        ContentValues valores = new ContentValues();
        valores.put(MediaStore.Images.Media.DATA, ruta_foto);
        valores.put(MediaStore.Images.Media.MIME_TYPE, tipo_mime);
        return cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valores) != null;
    }

    public static boolean eliminarImagen(String ruta_foto, ContentResolver cr){
        String projection = MediaStore.Images.Media.DATA + "=?";
        String clause[] = { ruta_foto };
        int filas_eliminadas = cr.delete(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                clause
        );
        return filas_eliminadas == 1;
    }

    public static boolean actualizarImagen(String ruta_vieja, String ruta_nueva,
                                           ContentResolver cr){
        ContentValues valores_actualizados = new ContentValues();
        valores_actualizados.put(MediaStore.Images.Media.DATA, ruta_nueva);
        String projection = MediaStore.Images.Media.DATA + "=?";
        String clause[] = { ruta_vieja };
        int filas_actualizadas = cr.update(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                valores_actualizados,
                projection,
                clause
        );
        return filas_actualizadas == 1;
    }

    public static String obtenerIdImagen(String ruta_imagen, ContentResolver cr) {
        String columnas[] = new String[]{
                MediaStore.Images.Media._ID
        };
        String projection = MediaStore.Images.Media.DATA + "=?";
        String clause[] = new String[]{ ruta_imagen };
        Cursor cursor = cr.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                columnas,
                projection,
                clause,
                null
        );
        if (cursor == null) {
            Log.e(LOG_TAG, "Se retorno un cursor nulo del query al ContentProvider de la galeria");
        } else if (cursor.getCount() < 1) {
            Log.wtf(LOG_TAG, "No se encontro la foto con el ContentProvider en la galeria");
            cursor.close();
        } else {
            cursor.moveToFirst();
            return cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media._ID));
        }
        return null;
    }
}
