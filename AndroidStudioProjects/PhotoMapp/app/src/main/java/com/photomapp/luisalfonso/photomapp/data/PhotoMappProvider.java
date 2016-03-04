package com.photomapp.luisalfonso.photomapp.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * Clase PhotoMappProvider: ContentProvider para obtener acceder a la informacion de las fotos dentro de la app.
 */
public class PhotoMappProvider extends ContentProvider {

    private static final UriMatcher uri_matcher = construirUriMatcher();
    private BaseDatos photomapp_db_opener_helper;

    static final int FOTO = 100;

    static UriMatcher construirUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = ContratoPhotoMapp.CONTENT_AUTHORITY;
        matcher.addURI(authority, ContratoPhotoMapp.PATH_FOTOS, FOTO);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        photomapp_db_opener_helper = new BaseDatos(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor_retorno;
        switch (uri_matcher.match(uri)) {
            case FOTO:
                cursor_retorno = photomapp_db_opener_helper.getReadableDatabase().query(ContratoPhotoMapp.Fotos.NOMBRE_TABLA,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown uri: " + uri);
        }
        cursor_retorno.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor_retorno;
    }

    @Override
    public String getType(Uri uri) {
        final int match = uri_matcher.match(uri);

        switch (match) {
            case FOTO:
                return ContratoPhotoMapp.Fotos.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase base_datos = photomapp_db_opener_helper.getWritableDatabase();
        final int match = uri_matcher.match(uri);
        Uri uri_retorno;

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
        getContext().getContentResolver().notifyChange(uri, null);
        return uri_retorno;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase base_datos = photomapp_db_opener_helper.getWritableDatabase();
        final int match = uri_matcher.match(uri);
        int filas_eliminadas;

        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";
        switch (match) {
            case FOTO:
                filas_eliminadas = base_datos.delete(ContratoPhotoMapp.Fotos.NOMBRE_TABLA, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (filas_eliminadas != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return filas_eliminadas;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase base_datos = photomapp_db_opener_helper.getWritableDatabase();
        final int match = uri_matcher.match(uri);
        int filas_actualizadas;

        switch (match) {
            case FOTO:
                filas_actualizadas = base_datos.update(ContratoPhotoMapp.Fotos.NOMBRE_TABLA, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown uri: " + uri);
        }
        if (filas_actualizadas != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return filas_actualizadas;
    }

}