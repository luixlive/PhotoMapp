package com.photomapp.luisalfonso.photomapp.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Clase ContratoPhotoMapp: Contrato para el modelado de la BD y los Uris para el ContentProvider.
 */
public class ContratoPhotoMapp {

    //Constantes para la creacion de los Uris
    public static final String CONTENT_AUTHORITY = "com.photomapp.luisalfonso.photomapp";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_FOTOS = "fotos";

    /**
     * ContratoPhotoMapp: constructor vacio por si se crea una instancia de esta clase.
     */
    private ContratoPhotoMapp() {}

    /**
     * Clase FotosTomadas: Modelado de la tabla Fotos.
     */
    public static final class Fotos implements BaseColumns{
        //Variables para la construccion del Uri
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_FOTOS).build();
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" +
                PATH_FOTOS;

        //Nombre y columnas de la tabla
        public static final String NOMBRE_TABLA         = "fotos";
        public static final String _ID                  = "_id";
        public static final String COLUMNA_NOMBRE       = "nombre";
        public static final String COLUMNA_FECHA        = "fecha";
        public static final String COLUMNA_LATITUD      = "latitud";
        public static final String COLUMNA_LONGITUD     = "longitud";

        /**
         * construirFotosUri: construye un uri con el id dado.
         * @param id el valor del id para construir la Uri
         * @return el Uri valido
         */
        public static Uri construirFotosUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

}