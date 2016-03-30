package com.photomapp.luisalfonso.photomapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.photomapp.luisalfonso.photomapp.data.ContratoPhotoMapp.Fotos;

/**
 * Clase BaseDatos: Crea y mantiene actualizada la base de datos de la app donde se almacena el
 * nombre de la foto, la fecha en que se tomo y la ubicacion.
 */
public class BaseDatos extends SQLiteOpenHelper {

    //Actualizar la version cada vez que se modifiquen las tablas
    public static final  int    VERSION_BASE_DATOS  = 1;
    public static final  String NOMBRE_BASE_DATOS   = "photomappdata.db";

    //Macros para creacion de la tabla
    private static final String TIPO_TEXTO          = " TEXT";
    private static final String TIPO_REAL           = " REAL";
    private static final String SEPARACION_COMA     = ",";

    //Comando de SQLite para crear la tabla
    private static final String SQL_CREAR_TABLA_FOTOS =
            "CREATE TABLE " + Fotos.NOMBRE_TABLA + " (" +
            Fotos._ID + " INTEGER PRIMARY KEY," +
            Fotos.COLUMNA_NOMBRE + TIPO_TEXTO + SEPARACION_COMA +
            Fotos.COLUMNA_FECHA + TIPO_TEXTO + SEPARACION_COMA +
            Fotos.COLUMNA_LATITUD + TIPO_REAL + SEPARACION_COMA +
            Fotos.COLUMNA_LONGITUD + TIPO_REAL + " );";

    //Comando de SQLite para borrar la tabla
    //private static final String SQL_BORRAR_TABLA = "DROP TABLE IF EXISTS " + Fotos.NOMBRE_TABLA;

    public BaseDatos(Context context) {
        super(context, NOMBRE_BASE_DATOS, null, VERSION_BASE_DATOS);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREAR_TABLA_FOTOS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //if (oldVersion < 2) {
        //CUANDO SE ACTUALICE LA BASE DE DATOS A LA VERSION 2
        //https://thebhwgroup.com/blog/how-android-sqlite-onupgrade
        //}
        //...
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

}