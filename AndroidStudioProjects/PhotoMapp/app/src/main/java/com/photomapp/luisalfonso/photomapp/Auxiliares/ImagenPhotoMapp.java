package com.photomapp.luisalfonso.photomapp.Auxiliares;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;

/**
 * Clase ImagenPhotoMapp: Clase para crear objetos que representen a las imagenes de la aplicacion.
 * Esta clase no guarda los bitmaps de las fotos, solamente se asocia por el nombre al bitmap y
 * guarda la informacion
 */
public class ImagenPhotoMapp {

    private final static String LOG_TAG = "ImagenPhotoMapp";

    //Informacion de la foto
    private int id_bd;
    private String nombre;
    private double latitud;
    private double longitud;
    private String fecha;

    public ImagenPhotoMapp(int id, String nombre, String fecha, double latitud, double longitud){
        id_bd = id;
        this.nombre = nombre;
        this.fecha = fecha;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    //Getters comunes
    public String getFecha() {
        return fecha;
    }

    public String getNombre() {
        return nombre;
    }

    public int getId_bd() {
        return id_bd;
    }

    public double getLatitud(){
        return latitud;
    }

    public double getLongitud(){
        return  longitud;
    }

    /**
     * getLatLng: Regresa la ubicacion donde se encuenetra esta foto
     * @return LatLng con las coordenadas de l aubicacion
     */
    public LatLng getLatLng() {
        return new LatLng(latitud, longitud);
    }

    /**
     * obtenerNombreCiudad: regresa la direccion de la ubicacion especificada.
     * @param context Context de la aplicacion de donde se llama.
     * @return Address con la direccion o null si no se pudo obtener.
     */
    public Address obtenerDireccion(Context context){
        //Usamos la clase Geocoder para obtener un objeto Address
        Geocoder localizador = new Geocoder(context);
        LatLng ubicacion = getLatLng();
        try {
            //Regresamos la direccion
            return localizador.getFromLocation(ubicacion.latitude, ubicacion.longitude, 1).get(0);
        } catch (IOException e) {
            Log.e(LOG_TAG, "No se pudo obtener la direccion: ");
            e.printStackTrace();
        }
        return null;
    }

}