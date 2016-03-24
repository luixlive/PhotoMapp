package com.photomapp.luisalfonso.photomapp;

import android.Manifest;
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
import com.photomapp.luisalfonso.photomapp.Activities.ActivityPrincipal;

/**
 * Clase ManejadorUbicacion: Apoya con buenas practicas para la obtencion de la ubicacion segun:
 * http://developer.android.com/intl/es/guide/topics/location/strategies.html
 */
public class ManejadorUbicacion {

    //Macros para actualizacion de ubicacion (cada 15 segundos o cada que el usuario se mueva 15 metros)
    private static final int TIEMPO_ACTUALIZACION = 1000*15;
    private static final int DISTANCIA_ACTUALIZACION = 15;
    private static final int DOS_MINUTOS = 1000*60*2;

    //Variables de apoyo
    private LocationManager manejador;
    private ActivityPrincipal activity_padre;
    private Location ultima_ubicacion = null;
    private LocationListener cambio_ubicacion_listener;

    /**
     * ManejadorUbicacion; Constructor, inicia las variables de apoyo
     * @param activity Activity padre para el acceso a los servicios de ubicacion
     */
    public ManejadorUbicacion(ActivityPrincipal activity) {
        activity_padre = activity;
        manejador = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        cambio_ubicacion_listener = new LocationListener() {
            public void onLocationChanged(Location location) {
                //Si se detecta una nueva ubicacion se checa si es mejor y de ser asi actualiza la ultima ubicacion
                if (esMejorUbicacion(location, ultima_ubicacion))
                    ultima_ubicacion = location;
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        //Obtenemos la ultima ubicacion conocida por el servicio telefonico en caso de que se tome una foto antes
        //de acceder al GPS
        if (ActivityCompat.checkSelfPermission(activity_padre, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity_padre,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity_padre, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, ActivityPrincipal.PERMISO_ACCESO_UBICACION);
        }
        ultima_ubicacion = manejador.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * comenzarActualizacionUbicacion: Comienza a escuchar los eventos de cambio de ubicacion (gasta bateria,
     * por lo que es recomendable usar detenerActualizaiconUbicacion en cuanto ya no se necesite.
     */
    public void comenzarActualizacionUbicacion() {
        if (ActivityCompat.checkSelfPermission(activity_padre, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity_padre,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity_padre, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, ActivityPrincipal.PERMISO_ACCESO_UBICACION);
        }
        if (activity_padre.ubicacionPermitida()) {
            //Obtenemos la preferencia del usuario respecto a si quiere utilizar el GPS o no
            SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(activity_padre);
            boolean gps = preferencias.getBoolean(ActivityPreferencias.PREFERENCIA_UTILIZAR_GPS_KEY, false);
            if (gps) {
                manejador.requestLocationUpdates(LocationManager.GPS_PROVIDER, TIEMPO_ACTUALIZACION, DISTANCIA_ACTUALIZACION,
                        cambio_ubicacion_listener);
            } else{
                manejador.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, TIEMPO_ACTUALIZACION, DISTANCIA_ACTUALIZACION,
                        cambio_ubicacion_listener);
            }
        }
    }

    /**
     * detenerActualizacionUbicacion: Detiene los eventos de ubicacion.
     */
    public void detenerActualizacionUbicacion() {
        if (ActivityCompat.checkSelfPermission(activity_padre, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity_padre,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity_padre, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, ActivityPrincipal.PERMISO_ACCESO_UBICACION);
        }
        if (activity_padre.ubicacionPermitida()) {
            manejador.removeUpdates(cambio_ubicacion_listener);
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
     * esMejorUbicacion: Algoritmo simple para determinar si es conveniente o no actualizar la ultima ubicacion
     * de acuerdo a las caracteristicas de la nueva y vieja ubicacion.
     * @param ubicacion_nueva Location nueva
     * @param actual_mejor_ubicacion Location que se tenia como la mejor ubicacion
     * @return true si es conveniente acutualizar a la nueva, false de toro modo
     */
    protected boolean esMejorUbicacion(Location ubicacion_nueva, Location actual_mejor_ubicacion) {
        if (actual_mejor_ubicacion == null) {
            return true;
        }

        //Primero checamos que tanta diferencia de tiempo hay, si es mucho mas nueva si conviene cambiar
        long diferencia_tiempo = ubicacion_nueva.getTime() - actual_mejor_ubicacion.getTime();
        boolean es_mucho_mas_nueva = diferencia_tiempo > DOS_MINUTOS;
        boolean es_nueva = diferencia_tiempo > 0;
        if (es_mucho_mas_nueva) {
            return true;
        }

        //Ahora la precision, si es mas precisa, combiene cambiar
        int diferencia_presicion = (int) (ubicacion_nueva.getAccuracy() - actual_mejor_ubicacion.getAccuracy());
        boolean menos_precisa = diferencia_presicion > 0;
        boolean mas_precisa = diferencia_presicion < 0;
        //Si es igual de precisa pero mas nueva tambien combiene cambiar
        if (mas_precisa) {
            return true;
        } else if (es_nueva && !menos_precisa) {
            return true;
        }
        return false;
    }

}
