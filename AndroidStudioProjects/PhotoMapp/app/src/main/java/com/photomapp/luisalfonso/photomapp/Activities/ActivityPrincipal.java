/**
 * Luis Alfonso Chávez Abbadie
 * Ingeniería de Software 2
 * Ingeniería en Cibernética y en Sistemas Computacionales, 6 semestre
 */
package com.photomapp.luisalfonso.photomapp.Activities;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.photomapp.luisalfonso.photomapp.Auxiliares.ManejadorPermisos;
import com.photomapp.luisalfonso.photomapp.Fragments.DialogoNombreFoto;
import com.photomapp.luisalfonso.photomapp.Auxiliares.GuardadorFoto;
import com.photomapp.luisalfonso.photomapp.Auxiliares.ManejadorCamara;
import com.photomapp.luisalfonso.photomapp.Auxiliares.ManejadorFotoTomada;
import com.photomapp.luisalfonso.photomapp.Auxiliares.ManejadorUbicacion;
import com.photomapp.luisalfonso.photomapp.R;
import com.photomapp.luisalfonso.photomapp.Auxiliares.Util;
import com.photomapp.luisalfonso.photomapp.data.ContratoPhotoMapp;

/**
 * Clase ActivityPrincipal: accede a la camara del smartphone y muestra al usuario un preview de la
 * imagen para que pueda tomar fotos. Cuenta con un menu que da acceso al mapa y a las
 * configuraciones de la app.
 */
public class ActivityPrincipal extends AppCompatActivity implements
        DialogoNombreFoto.NombreSeleccionadoListener{

    //Macros
    private final static int TIEMPO_MOSTRAR_FOTO = 400;

    //Variables de las preferencias del usuario
    private boolean autonombrar_fotos;

    //Objetos para el manejo de la ubicacion, camara y fotos tomadas
    private ManejadorUbicacion manejador_ubicacion;
    private ManejadorCamara manejador_camara;
    private ManejadorFotoTomada manejador_foto;

    //Variables utilizadas para el guardado de las fotos tomadas
    private String fecha;
    private double latitud, longitud;
    private Bitmap foto_tomada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);

        //Obtenemos los manejadores de camara y ubicacion
        if(ManejadorPermisos.checarPermisoUbicacion(this)) {
            manejador_ubicacion = new ManejadorUbicacion(this);
        }

        if (ManejadorPermisos.checarPermisoCamara(this)) {
            manejador_camara = new ManejadorCamara(this,
                    (TextureView) findViewById(R.id.contenedor_imagen_camara));
            manejador_camara.setTomarFotoListener(new ManejadorCamara.TomarFotoListener() {

                @Override
                public void fotoTomada(Image foto) {
                    if (Util.obtenerEscrituraPosible()) {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.toast_foto_tomada), Toast.LENGTH_SHORT).show();
                        obtenerYMostrarFotoTomada(foto);
                    } else {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.no_escritura_posible),
                                Toast.LENGTH_SHORT).show();
                    }
                }

            });
        }
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

        //Se obtienen las preferencias de usuario y se comienza a actualizar la ubicacion
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this);
        autonombrar_fotos = preferencias.getBoolean(
                ActivityPreferencias.PREFERENCIA_AUTONOMBRAR_FOTO_KEY, false);
        //Primero se asegura de tener permiso del usuario para acceder a la camara
        if (manejador_camara != null) {
            manejador_camara.iniciar();
            manejador_camara.cambiarPreferenciaFlash(
                    preferencias.getBoolean(ActivityPreferencias.PREFERENCIA_UTILIZAR_FLASH_KEY,
                            false)
            );
        } else {
            ManejadorPermisos.checarPermisoCamara(this);
        }
        if (manejador_ubicacion != null) {
            manejador_ubicacion.comenzarActualizacionUbicacion();
        } else{
            ManejadorPermisos.checarPermisoUbicacion(this);
        }
    }

    @Override
    protected void onPause() {
        //Cierra los procesos de camara y ubicacion
        if (manejador_camara != null) {
            manejador_camara.terminar();
        }
        if (manejador_ubicacion != null) {
            manejador_ubicacion.detenerActualizacionUbicacion();
        }

        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Si se pulsa un boton del menu
        switch (item.getItemId()) {
            case R.id.preferencias:
                startActivity(new Intent(this, ActivityPreferencias.class));
                return true;
            case R.id.mapa:
                startActivity(new Intent(this, ActivityMapa.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        //Si fue necesario pedir permiso al usuario para acceso a camara o ubicacion
        switch (requestCode) {

            case ManejadorPermisos.PERMISO_ACCESO_CAMARA:
                if (grantResults.length > 0 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    manejador_camara = new ManejadorCamara(this,
                            (TextureView) findViewById(R.id.contenedor_imagen_camara));
                    manejador_camara.setTomarFotoListener(
                            new ManejadorCamara.TomarFotoListener() {

                                @Override
                                public void fotoTomada(Image foto) {
                                    if (Util.obtenerEscrituraPosible()) {
                                        Toast.makeText(getApplicationContext(),
                                                getString(R.string.toast_foto_tomada),
                                                Toast.LENGTH_SHORT).show();
                                        obtenerYMostrarFotoTomada(foto);
                                    } else {
                                        Toast.makeText(getApplicationContext(),
                                                getString(R.string.no_escritura_posible),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                    );
                    //Despues de inicia el proceso de captura de imagen
                    manejador_camara.iniciar();
                } else {
                    Toast.makeText(this, getString(R.string.permiso_camara_denegado),
                            Toast.LENGTH_LONG).show();
                }
                break;

            case ManejadorPermisos.PERMISO_ACCESO_UBICACION:
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    manejador_ubicacion = new ManejadorUbicacion(this);
                    manejador_ubicacion.comenzarActualizacionUbicacion();
                } else{
                    Toast.makeText(this, getString(R.string.permiso_ubicacion_denegado),
                            Toast.LENGTH_LONG).show();
                }

            case ManejadorPermisos.PERMISO_ACCESO_ALMACENAMIENTO_EXTERNO:
                if (!(grantResults.length > 0 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, getString(R.string.permiso_almacenamiento_denegado),
                            Toast.LENGTH_LONG).show();
                }
        }
    }

    @Override
    public void nombreSeleccionado(String nombre) {
        //Si el termina de introducir el nombre de la foto, se guarda y se la muestra del resultado
        guardarFoto(nombre);
        manejador_foto.quitarFotoMostrada();
    }

    @Override
    public void nombreCancelado() {
        manejador_foto.quitarFotoMostrada();
    }

    /**
     * obtenerYMostrarFotoTomada: Obtiene un bitmap a partir del objeto Image que regresa la camara
     * y muestra el resultado en la pantalla
     * @param foto Image obtenida por la camara al tomar la foto
     */
    private void obtenerYMostrarFotoTomada(Image foto) {
        View contenedor_camara = findViewById(R.id.contenedor_imagen_camara);
        manejador_foto = new ManejadorFotoTomada(
                foto,
                contenedor_camara,
                findViewById(R.id.foto_tomada),
                getResources().getInteger(android.R.integer.config_shortAnimTime),
                contenedor_camara.getHeight(),
                contenedor_camara.getWidth()
        );
        manejador_foto.setManejadorFotoTomadaListener(
                new ManejadorFotoTomada.ManejadorFotoTomadaListener() {

                    @Override
                    public void fotoObtenida(Bitmap foto) {
                        foto_tomada = foto;
                        String nombre_foto =
                                Util.obtenerFecha(getString(R.string.nombre_foto_formato_fecha))
                                        + getString(R.string.app_name);

                        //Si elegio autonombrar fotos en preferencias se pone el nombre por default
                        if (autonombrar_fotos) {
                            //Se guarda la foto y se muestra el resultado por un lapso
                            guardarFoto(nombre_foto);
                            new AsyncTask<Void, Void, Integer>() {

                                @Override
                                protected Integer doInBackground(Void... params) {
                                    try {
                                        Thread.sleep(TIEMPO_MOSTRAR_FOTO);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    return 0;
                                }

                                @Override
                                protected void onPostExecute(Integer result) {
                                    manejador_foto.quitarFotoMostrada();
                                }

                            }.execute();
                        }
                        //Si no eligio autonombrar se muestra un dialogo para que elija el nombre
                        else {
                            DialogoNombreFoto dialogo_nombre_foto =
                                    DialogoNombreFoto.nuevoDialogo(nombre_foto);
                            dialogo_nombre_foto.show(getFragmentManager(),
                                    DialogoNombreFoto.class.getName());
                        }
                    }

                    @Override
                    public void fotoMostrada() {
                        //La foto esta actualmente en la pantalla
                    }

                    @Override
                    public void fotoQuitada() {
                        //Cuando se retire la foto, volvemos a poner la camara
                        manejador_camara.fotoTerminada();
                    }

                });
        manejador_foto.execute();
    }

    /**
     * guardarFoto: Guarda la foto en el almacenamiento externo
     * @param nombre String nombre con que se guardara la foto
     */
    public void guardarFoto(String nombre){
        //Obtenemos los datos necesarios para guardar la foto y el registro en la base de datos
        Location ubicacion = manejador_ubicacion.obtenerUbicacion();
        LatLng lat_y_long;
        if (ubicacion != null) {
            lat_y_long = new LatLng(ubicacion.getLatitude(), ubicacion.getLongitude());
            fecha = Util.obtenerFecha(getString(R.string.base_datos_formato_fecha));
            latitud = lat_y_long.latitude;
            longitud = lat_y_long.longitude;
            GuardadorFoto guardador = new GuardadorFoto(nombre, foto_tomada, getContentResolver());
            //Si se guarda exitosamente se almacenan los registros
            guardador.setGuardadorFotoListener(new GuardadorFoto.GuardarFotoListener() {

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
                public void fotoNoGuardada() {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.toast_foto_no_tomada), Toast.LENGTH_SHORT).show();
                }

            });
            guardador.execute();
        }
        foto_tomada = null;
    }

    /**
     * foto: obtiene una captura de imagen de la camara. Solo se llama cuando el usuario pulsa el
     * FAB "Camara".
     * @param boton_camara: vista del FAB
     */
    public void foto(View boton_camara) {
        //Se necesitan permisos para acceder a la camara, obtener la ubicacion y guardar la foto
        if (ManejadorPermisos.checarPermisoCamara(this)){
            if (ManejadorPermisos.checarPermisoUbicacion(this)){
                if (ManejadorPermisos.checarPermisoAlmacenamiento(this)) {
                    manejador_camara.foto();
                }
            }
        }
    }

}