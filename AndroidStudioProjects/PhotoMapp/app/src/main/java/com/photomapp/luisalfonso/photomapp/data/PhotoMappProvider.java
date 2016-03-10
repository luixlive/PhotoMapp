package com.photomapp.luisalfonso.photomapp.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Clase PhotoMappProvider: ContentProvider para obtener acceder a la informacion de las fotos dentro de la app.
 */
public class PhotoMappProvider extends ContentProvider {

    //Etiquera para escribir en el LOG
    private static final String LOG_TAG = "PhotoMappProvider";

    //Macro que nos indica si se realizo una accion con el URI de la tabla Fotos
    static final int FOTO = 100;

    //Variables UriMatcher para las acciones a las tablas y la base de datos de PhotoMapp
    private static final UriMatcher uri_matcher = construirUriMatcher();
    private BaseDatos photomapp_db_opener_helper;

    /**
     * construirUriMatcher: Genera el UriMatcher
     * @return UriMatcher con los URIs posibles para nuestro provider
     */
    static UriMatcher construirUriMatcher() {
        //El provider solo acepta URIs con la tabla Fotos
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = ContratoPhotoMapp.CONTENT_AUTHORITY;
        matcher.addURI(authority, ContratoPhotoMapp.PATH_FOTOS, FOTO);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        //Obtiene la base de datos
        photomapp_db_opener_helper = new BaseDatos(getContext());
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor_retorno;
        //Se analiza el uri del query, de existir se pasa el query a la base de datos y se regresa el cursor recibido
        switch (uri_matcher.match(uri)) {
            case FOTO:
                cursor_retorno = photomapp_db_opener_helper.getReadableDatabase().query(ContratoPhotoMapp.Fotos.NOMBRE_TABLA,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown uri: " + uri);
        }
        if (getContext() != null) {
            cursor_retorno.setNotificationUri(getContext().getContentResolver(), uri);
        } else{
            Log.w(LOG_TAG, "Se necesita acceder al ContentResolver pero getContext regresa null");
        }
        return cursor_retorno;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        final int match = uri_matcher.match(uri);
        //El unico tipo que regresa nuestra tabla es Content Type Item
        switch (match) {
            case FOTO:
                return ContratoPhotoMapp.Fotos.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        final SQLiteDatabase base_datos = photomapp_db_opener_helper.getWritableDatabase();
        final int match = uri_matcher.match(uri);
        Uri uri_retorno;

        //Analiza la Uri e inserta los registros
        switch (match) {
            case FOTO: {
                long _id = base_datos.insert(ContratoPhotoMapp.Fotos.NOMBRE_TABLA, null, values);
                if ( _id > 0 )
                    uri_retorno = ContratoPhotoMapp.Fotos.construirFotosUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown uri: " + uri);
        }
        //Se notifica al contentResolver que hubo una modificacion
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        } else{
            Log.w(LOG_TAG, "Se necesita acceder al ContentResolver pero getContext regresa null");
        }
        return uri_retorno;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase base_datos = photomapp_db_opener_helper.getWritableDatabase();
        final int match = uri_matcher.match(uri);
        int filas_eliminadas;

        // Esto hara que se eliminen todos los registros
        if ( null == selection ) selection = "1";
        switch (match) {
            case FOTO:
                filas_eliminadas = base_datos.delete(ContratoPhotoMapp.Fotos.NOMBRE_TABLA, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown uri: " + uri);
        }
        //Si se eliminaron filas se avisa al contentResolver
        if (filas_eliminadas != 0) {
            if (getContext() != null) {
                getContext().getContentResolver().notifyChange(uri, null);
            } else{
                Log.w(LOG_TAG, "Se necesita acceder al ContentResolver pero getContext regresa null");
            }
        }
        return filas_eliminadas;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase base_datos = photomapp_db_opener_helper.getWritableDatabase();
        final int match = uri_matcher.match(uri);
        int filas_actualizadas;

        //Se analiza el uri y se modifican los registros
        switch (match) {
            case FOTO:
                filas_actualizadas = base_datos.update(ContratoPhotoMapp.Fotos.NOMBRE_TABLA, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown uri: " + uri);
        }
        //Si se modificaron de forma exitosa se avisa al contentResolver
        if (filas_actualizadas != 0) {
            if (getContext() != null) {
                getContext().getContentResolver().notifyChange(uri, null);
            } else{
                Log.w(LOG_TAG, "Se necesita acceder al ContentResolver pero getContext regresa null");
            }
        }
        return filas_actualizadas;
    }

}