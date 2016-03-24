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
import android.os.AsyncTask;
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
import com.photomapp.luisalfonso.photomapp.ManejadorCamara;
import com.photomapp.luisalfonso.photomapp.ManejadorUbicacion;
import com.photomapp.luisalfonso.photomapp.R;
import com.photomapp.luisalfonso.photomapp.Util;
import com.photomapp.luisalfonso.photomapp.data.ContratoPhotoMapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Clase ActivityPrincipal: accede a la camara del smartphone y muestra al usuario un preview de la imagen para que
 * pueda tomar fotos. Cuenta con un menu que da acceso al mapa y a las configuraciones de la app. Implementa la interfaz
 * SurfaceTextureListener para tener acceso a los eventos de la TextureView que es donde se muestra el stream de la camara.
 */
public class ActivityPrincipal extends AppCompatActivity implements DialogoNombreFoto.NombreSeleccionadoListener,
        ManejadorCamara.TomarFotoListener{

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

            GuardadorImagen guardador = new GuardadorImagen(new File(archivo + File.separator + nombre +
                    Util.EXTENSION_ARCHIVO_FOTO), nombre,Util.obtenerFecha(getString(R.string.base_datos_formato_fecha)),
                    lat_y_long.latitude, lat_y_long.longitude);
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

    /**
     * foto: obtiene una captura de imagen de la camara. Solo se llama cuando el usuario pulsa el FAB "Camara".
     * @param boton_camara: vista del FAB
     */
    public void foto(View boton_camara) {
        manejador_camara.foto();
    }

    @Override
    public void fotoTomada(Image foto) {
        foto_tomada = foto;
        if (Util.obtenerEscrituraPosible()) {
            //Obtenemos la imagen y el nombre por default
            String nombre_foto = Util.obtenerFecha(getString(R.string.nombre_foto_formato_fecha)) + getString(R.string.app_name);

            //Si elegio autonombrar fotos en preferencias se pone el nombre por default
            if (autonombrar_fotos) {
                Toast.makeText(ActivityPrincipal.this, getString(R.string.toast_foto_tomada), Toast.LENGTH_SHORT).show();
                Location ubicacion = manejador_ubicacion.obtenerUbicacion();
                LatLng lat_y_long;
                if (ubicacion != null) {
                    lat_y_long = new LatLng(ubicacion.getLatitude(), ubicacion.getLongitude());
                    GuardadorImagen guardador = new GuardadorImagen(new File(archivo + File.separator + nombre_foto +
                            Util.EXTENSION_ARCHIVO_FOTO), nombre_foto,Util.obtenerFecha(getString(R.string.base_datos_formato_fecha)),
                            lat_y_long.latitude, lat_y_long.longitude);
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

    /**
     * clase GuardadorImagen: AsynkTask que almacena la imagen y se inserta el nuevo registro en la base de datos en
     * background.
     */
    private class GuardadorImagen extends AsyncTask<Image, Integer, Boolean> {

        //Fichero e informacion para el registro
        private final File fichero;
        private final String nombre_foto, fecha;
        private final double latitud, longitud;

        /**
         * GuardadorImagen: Guarda la imagen.
         * @param fichero File donde se almacenara
         * @param nombre_foto String nombre
         * @param fecha String fecha
         * @param latitud Double latitud
         * @param longitud Double Longitud
         */
        public GuardadorImagen(File fichero, String nombre_foto, String fecha, double latitud, double longitud) {
            this.fichero = fichero;
            this.nombre_foto = nombre_foto;
            this.fecha = fecha;
            this.latitud = latitud;
            this.longitud = longitud;
        }

        @Override
        protected Boolean doInBackground(Image... params) {
            //Obtiene los bytes de la imagen
            Image imagen_a_guardar = params[0];
            ByteBuffer buffer = imagen_a_guardar.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            //Escribe los bytes en el fichero de salida
            FileOutputStream salida = null;
            try {
                salida = new FileOutputStream(fichero);
                salida.write(bytes);

                //Insertamos el registro en el ContentProvider (y base de datos)
                ContentValues values = new ContentValues();
                values.put(ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE, nombre_foto);
                values.put(ContratoPhotoMapp.Fotos.COLUMNA_FECHA, fecha);
                values.put(ContratoPhotoMapp.Fotos.COLUMNA_LATITUD, latitud);
                values.put(ContratoPhotoMapp.Fotos.COLUMNA_LONGITUD, longitud);
                getContentResolver().insert(ContratoPhotoMapp.Fotos.CONTENT_URI, values);
                return true;
            } catch (IOException e) {
                Log.e(LOG_TAG, "No se pudo guardar la foto: ");
                e.printStackTrace();
                return false;
            } finally {
                //Cierra la imagen y el escritor
                imagen_a_guardar.close();
                if (null != salida) {
                    try {
                        salida.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            //Indica al usuario si se almaceno su imagen exitosamente o no
            if (result) {
                Toast.makeText(ActivityPrincipal.this, getString(R.string.toast_foto_tomada), Toast.LENGTH_SHORT).show();
            } else{
                Toast.makeText(ActivityPrincipal.this, getString(R.string.toast_foto_no_tomada), Toast.LENGTH_SHORT).show();
            }
        }
    }
}