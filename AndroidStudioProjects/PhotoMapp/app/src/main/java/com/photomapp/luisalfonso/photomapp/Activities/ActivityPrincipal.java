/**
 * Ingeniería de Software 2
 * Ingeniería en Cibernética y en Sistemas Computacionales, 6 semestre
 */
package com.photomapp.luisalfonso.photomapp.Activities;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.Image;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.photomapp.luisalfonso.photomapp.Fragments.DialogoNombreFoto;
import com.photomapp.luisalfonso.photomapp.GuardadorFoto;
import com.photomapp.luisalfonso.photomapp.ManejadorCamara;
import com.photomapp.luisalfonso.photomapp.ManejadorUbicacion;
import com.photomapp.luisalfonso.photomapp.R;
import com.photomapp.luisalfonso.photomapp.Util;
import com.photomapp.luisalfonso.photomapp.data.ContratoPhotoMapp;

import java.io.File;

/**
 * Clase ActivityPrincipal: accede a la camara del smartphone y muestra al usuario un preview de la imagen para que
 * pueda tomar fotos. Cuenta con un menu que da acceso al mapa y a las configuraciones de la app. Implementa la interfaz
 * SurfaceTextureListener para tener acceso a los eventos de la TextureView que es donde se muestra el stream de la camara.
 */
public class ActivityPrincipal extends AppCompatActivity implements DialogoNombreFoto.NombreSeleccionadoListener,
        ManejadorCamara.TomarFotoListener, GuardadorFoto.GuardarFotoListener{

    private static final String LOG_TAG = "ACTIVITY PRINCIPAL";

    //Macros
    public static final int PERMISO_ACCESO_CAMARA = 0;
    public static final int PERMISO_ACCESO_UBICACION = 1;

    //Variables de las preferencias del usuario
    private boolean autonombrar_fotos;
    private boolean permiso_acceso_camara = true;
    private boolean permiso_acceso_ubicacion = true;

    //Variables para el uso del almacenamiento externo
    private File archivo;

    //Variable apoyo para obtencion de la ubicacion
    private ManejadorUbicacion manejador_ubicacion;

    //Variables de apoyo para el manejo de la camara en el TextureView y la captura
    private ManejadorCamara manejador_camara;
    private Image foto_tomada;

    private String fecha;
    private double latitud, longitud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);

        //Iniciamos el manejador de la ubicacion, el direcotorio de las fotos y el contenedor de las imagenes
        manejador_ubicacion = new ManejadorUbicacion(this);
        archivo = Util.obtenerDirectorioFotos();
        manejador_camara = new ManejadorCamara(this, (TextureView) findViewById(R.id.contenedor_imagen_camara));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Definimos el menu principal
        getMenuInflater().inflate(R.menu.menu_activity_principal, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        manejador_camara.iniciar();
        //Se obtienen las preferencias de usuario y se comienza a actualizar la ubicacion
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this);
        autonombrar_fotos = preferencias.getBoolean(ActivityPreferencias.PREFERENCIA_AUTONOMBRAR_FOTO_KEY, false);
        manejador_camara.cambiarPreferenciaFlash(preferencias.getBoolean(ActivityPreferencias.PREFERENCIA_UTILIZAR_FLASH_KEY,
                false));
        manejador_ubicacion.comenzarActualizacionUbicacion();
    }

    @Override
    protected void onPause() {
        //Cierra la camara y termina con los procesos de background y obtencion de ubicacion
        manejador_camara.terminar();
        manejador_ubicacion.detenerActualizacionUbicacion();

        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {     //Capturamos el item seleccionado por el usuario del menu (preferncias o mapa)
            case R.id.preferencias:
                startActivity(new Intent(this, ActivityPreferencias.class));
                return true;
            case R.id.mapa:
                Intent intent = new Intent(this, ActivityMapa.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            //Verificamos si el usuario dio acceso o no a la camara
            case PERMISO_ACCESO_CAMARA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permiso_acceso_camara = true;
                } else {
                    permiso_acceso_camara = false;
                    Toast.makeText(this, getString(R.string.permiso_camara_denegado), Toast.LENGTH_LONG).show();
                }
                break;
            case PERMISO_ACCESO_UBICACION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permiso_acceso_ubicacion = true;
                } else {
                    permiso_acceso_ubicacion = false;
                    Toast.makeText(this, getString(R.string.permiso_ubicacion_denegado), Toast.LENGTH_LONG).show();
                }
        }
    }

    @Override
    public void nombreSeleccionado(String nombre) {
        //Una vez que el usuario seleccione el nombre, se guarda la imagen y deja de enfocar
        Location ubicacion = manejador_ubicacion.obtenerUbicacion();
        if (ubicacion != null) {
            LatLng lat_y_long = new LatLng(ubicacion.getLatitude(), ubicacion.getLongitude());

            fecha = Util.obtenerFecha(getString(R.string.base_datos_formato_fecha));
            latitud = lat_y_long.latitude;
            longitud = lat_y_long.longitude;
            GuardadorFoto guardador = new GuardadorFoto(this,
                    new File(archivo + File.separator + nombre + Util.EXTENSION_ARCHIVO_FOTO),
                    nombre);
            guardador.execute(foto_tomada);
        } else{
            Toast.makeText(ActivityPrincipal.this, getString(R.string.no_hay_ubicacion), Toast.LENGTH_SHORT).show();
        }
        manejador_camara.fotoTerminada();
    }

    @Override
    public void nombreCancelado() {
        //Si el usuario cierra el dialogo de alguna forma, no guardamos la foto
        manejador_camara.fotoTerminada();
    }

    @Override
    public void fotoTomada(Image foto) {
        foto_tomada = foto;
        if (Util.obtenerEscrituraPosible()) {
            Toast.makeText(getApplicationContext(), getString(R.string.toast_foto_tomada),
                    Toast.LENGTH_SHORT).show();
            //Obtenemos la imagen y el nombre por default
            String nombre_foto = Util.obtenerFecha(getString(R.string.nombre_foto_formato_fecha)) + getString(R.string.app_name);

            //Si elegio autonombrar fotos en preferencias se pone el nombre por default
            if (autonombrar_fotos) {
                Toast.makeText(ActivityPrincipal.this, getString(R.string.toast_foto_tomada), Toast.LENGTH_SHORT).show();
                Location ubicacion = manejador_ubicacion.obtenerUbicacion();
                LatLng lat_y_long;
                if (ubicacion != null) {
                    lat_y_long = new LatLng(ubicacion.getLatitude(), ubicacion.getLongitude());
                    fecha = Util.obtenerFecha(getString(R.string.base_datos_formato_fecha));
                    latitud = lat_y_long.latitude;
                    longitud = lat_y_long.longitude;
                    GuardadorFoto guardador = new GuardadorFoto(this,
                            new File(archivo + File.separator + nombre_foto + Util.EXTENSION_ARCHIVO_FOTO),
                            nombre_foto);
                    guardador.execute(foto_tomada);
                }
                manejador_camara.fotoTerminada();
            }
            //Si no eligio autonombrar se le muestra un dialogo para que elija el nombre de la foto
            else {
                DialogoNombreFoto dialogo_nombre_foto = DialogoNombreFoto.nuevoDialogo(nombre_foto);
                dialogo_nombre_foto.show(getFragmentManager(), DialogoNombreFoto.class.getName());
            }
        } else {
            Toast.makeText(ActivityPrincipal.this, ActivityPrincipal.this.getString(R.string.no_escritura_posible),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void fotoGuardada(String nombre_foto) {
        //Insertamos el registro en el ContentProvider (y base de datos)
        ContentValues values = new ContentValues();
        values.put(ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE, nombre_foto);
        values.put(ContratoPhotoMapp.Fotos.COLUMNA_FECHA, fecha);
        values.put(ContratoPhotoMapp.Fotos.COLUMNA_LATITUD, latitud);
        values.put(ContratoPhotoMapp.Fotos.COLUMNA_LONGITUD, longitud);
        getContentResolver().insert(ContratoPhotoMapp.Fotos.CONTENT_URI, values);
    }

    @Override
    public void fotoNoGuardada(String nombre_foto) {
        Log.w(LOG_TAG, "Foto no guardada");
        Toast.makeText(getApplicationContext(), getString(R.string.toast_foto_no_tomada),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * foto: obtiene una captura de imagen de la camara. Solo se llama cuando el usuario pulsa el FAB "Camara".
     * @param boton_camara: vista del FAB
     */
    public void foto(View boton_camara) {
        manejador_camara.foto();
    }

    /**
     * ubicacionPermitida: Indica si el usuario permite acceder a su ubicacion.
     * @return true si es posible acceder a la ubicacion, false de otro modoa
     */
    public boolean ubicacionPermitida(){
        return permiso_acceso_ubicacion;
    }

    /**
     * permisoAccesoCamara: Indica si el usuario permite acceder a su camara.
     * @return true si es posible acceder a la camara, false de otro modoa
     */
    public boolean permisoAccesoCamara(){
        return permiso_acceso_camara;
    }

}