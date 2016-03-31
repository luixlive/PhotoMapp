package com.photomapp.luisalfonso.photomapp.Auxiliares;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;

import com.photomapp.luisalfonso.photomapp.Activities.ActivityPreferencias;

/**
 * Clase ManejadorUbicacion: Apoya con buenas practicas para la obtencion de la ubicacion segun:
 * http://developer.android.com/intl/es/guide/topics/location/strategies.html
 */
public class ManejadorUbicacion {

    //Macros para actualizacion de ubicacion (cada 15 segundos o cada que el usuario se mueva 15
    //metros)
    private static final int TIEMPO_ACTUALIZACION = 1000 * 15;
    private static final int DISTANCIA_ACTUALIZACION = 15;
    private static final int DOS_MINUTOS = 1000 * 60 * 2;

    //Variable que nos ayuda a determinar si el proceso esta corriendo
    private boolean proceso_iniciado = false;

    //Variables de apoyo
    private LocationManager manejador;
    private Activity activity;
    private Location ultima_ubicacion = null;
    private LocationListener cambio_ubicacion_listener;

    /**
     * ManejadorUbicacion; Constructor, inicia las variables de apoyo
     * @param activity Activity padre para el acceso a los servicios de ubicacion
     */
    public ManejadorUbicacion(Activity activity) {
        this.activity = activity;
        manejador = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        cambio_ubicacion_listener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                //Si se detecta una nueva ubicacion se checa si es mejor
                if (esMejorUbicacion(location, ultima_ubicacion)) {
                    //Si es mejor, se actualiza
                    ultima_ubicacion = location;
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        //Obtenemos la ultima ubicacion conocida por el servicio telefonico en caso de que se tome
        //una foto antes de acceder al GPS
        ultima_ubicacion = obtenerUltimaUbicacionConocida(activity,
                LocationManager.NETWORK_PROVIDER);
    }

    /**
     * obtenerUltimaUbicacionConocida: Regresa la ultima posicion conocida por el usuario segun
     * el provider establecido.
     * @param activity Activity contexto actual
     * @param provider String provider deseado
     * @return Location ultima ubicacion conocida
     */
    public static Location obtenerUltimaUbicacionConocida(Activity activity, String provider){
        if (ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return ((LocationManager)activity.getSystemService(Context.LOCATION_SERVICE)).
                    getLastKnownLocation(provider);
        }
        return null;
    }

    /**
     * comenzarActualizacionUbicacion: Comienza a escuchar los eventos de cambio de ubicacion (gasta
     * bateria,por lo que es recomendable usar detenerActualizaiconUbicacion en cuanto ya no se
     * necesite.
     */
    public void comenzarActualizacionUbicacion() {
        if (!proceso_iniciado) {
            //Es necesario checar de forma explicita que se tiene el permiso (aunque ya sabemos que se
            //tiene)
            if (ActivityCompat.checkSelfPermission(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //Obtenemos la preferencia del usuario respecto a si quiere utilizar el GPS o no
                SharedPreferences preferencias =
                        PreferenceManager.getDefaultSharedPreferences(activity);
                boolean gps = preferencias.getBoolean(ActivityPreferencias.PREFERENCIA_UTILIZAR_GPS_KEY,
                        false);
                if (gps) {
                    manejador.requestLocationUpdates(LocationManager.GPS_PROVIDER, TIEMPO_ACTUALIZACION,
                            DISTANCIA_ACTUALIZACION, cambio_ubicacion_listener);
                } else {
                    manejador.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            TIEMPO_ACTUALIZACION, DISTANCIA_ACTUALIZACION, cambio_ubicacion_listener);
                }
            }
            proceso_iniciado = true;
        }
    }

    /**
     * detenerActualizacionUbicacion: Detiene los eventos de ubicacion.
     */
    public void detenerActualizacionUbicacion() {
        if (proceso_iniciado) {
            //Se checa de forma explicita el permiso
            if (ActivityCompat.checkSelfPermission(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                manejador.removeUpdates(cambio_ubicacion_listener);
            }
            proceso_iniciado = false;
        }
    }

    /**
     * obtenerUbicacion: Regresa la ubicacion mas reciente obtenida.
     * @return Location con la ultima ubicacion conocida.
     */
    public Location obtenerUbicacion(){
        return ultima_ubicacion;
    }

    /**
     * esMejorUbicacion: Algoritmo simple para determinar si es conveniente o no actualizar la
     * ultima ubicacion de acuerdo a las caracteristicas de la nueva y vieja ubicacion.
     * @param ubicacion_nueva Location nueva
     * @param actual_mejor_ubicacion Location que se tenia como la mejor ubicacion
     * @return true si es conveniente acutualizar a la nueva, false de toro modo
     */
    protected boolean esMejorUbicacion(Location ubicacion_nueva, Location actual_mejor_ubicacion) {
        if (actual_mejor_ubicacion == null) {
            return true;
        }

        //Checamos que tanta diferencia de tiempo hay, si mucho mas nueva si conviene cambiar
        long diferencia_tiempo = ubicacion_nueva.getTime() - actual_mejor_ubicacion.getTime();
        if (diferencia_tiempo > DOS_MINUTOS) {
            return true;
        }

        //Checamos la precision
        int diferencia_presicion = (int) (ubicacion_nueva.getAccuracy() -
                actual_mejor_ubicacion.getAccuracy());
        if (diferencia_presicion < 0) {
            //Es mas precisa, conviene cambiar
            return true;
        } else if (diferencia_tiempo > 0 && diferencia_presicion == 0) {
            //Es igual de precisa y es mas nueva, conviene cambiar
            return true;
        }
        return false;
    }

}