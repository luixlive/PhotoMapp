/**
 * Ingeniería de Software 2
 * Ingeniería en Cibernética y en Sistemas Computacionales, 6 semestre
 */
package com.photomapp.luisalfonso.photomapp.Activities;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.location.LocationManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.photomapp.luisalfonso.photomapp.DialogoNombreFoto;
import com.photomapp.luisalfonso.photomapp.R;
import com.photomapp.luisalfonso.photomapp.Util;
import com.photomapp.luisalfonso.photomapp.data.ContratoPhotoMapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Clase ActivityPrincipal: accede a la camara del smartphone y muestra al usuario un preview de la imagen para que
 * pueda tomar fotos. Cuenta con un menu que da acceso al mapa y a las configuraciones de la app. Implementa la interfaz
 * SurfaceTextureListener para tener acceso a los eventos de la TextureView que es donde se muestra el stream de la camara.
 */
public class ActivityPrincipal extends AppCompatActivity implements TextureView.SurfaceTextureListener,
        DialogoNombreFoto.NombreSeleccionadoListener {

    private static final String LOG_TAG = ActivityPrincipal.class.getName();

    //Macros publicas para compartir datos entre activities
    public static final String EXTRA_LECTURA_POSIBLE = "ELP";
    public static final String EXTRA_ESCRITURA_POSIBLE = "EEP";

    //Macros
    private static final int PERMISO_ACCESO_CAMARA = 0;
    private static final int PERMISO_ACCESO_UBICACION = 1;
    private static final int ESTADO_PREVIEW = 0;
    private static final int ESTADO_ESPERANDO_ENFOQUE = 1;
    private static final int ESTADO_ESPERANDO_PRECAPTURA = 2;
    private static final int ESTADO_ESPERANDO_NO_PRECAPTURA = 3;
    private static final int ESTADO_FOTO_TOMADA = 4;
    private static final int ORIENTACION_PORTRAIT = 90;
    public static final String NOMBRE_ALBUM_FOTOS = "PhotoMapp";
    public static final String EXTENSION_ARCHIVO_FOTO = ".jpg";

    //Variables de las preferencias del usuario
    private boolean autonombrar_fotos;
    private boolean permiso_acceso_camara = true;
    private boolean permiso_acceso_ubicacion = true;

    //Variables de apoyo para manejar la TextureView
    private TextureView contenedor_imagen_camara;
    private Size tam_surface;

    //Variables para el uso del almacenamiento externo
    private ImageReader lector_imagen_fija;
    private File archivo;
    private boolean escritura_posible = true;
    private boolean lectura_posible = true;
    private Image foto_tomada;

    //Variables para las acciones que se ejecutaran en background
    private HandlerThread hilo_background;
    private Handler handler;

    //Variables de apoyo para manejar la camara
    private int estado_actual_camara = ESTADO_PREVIEW;
    private CameraDevice camara;
    private CaptureRequest.Builder constructor_imagen_preview;
    private CameraCaptureSession sesion_captura_imagen;
    private boolean soporta_flash;
    private Semaphore semaforo_abrir_cerrar_camara = new Semaphore(1);
    private CaptureRequest solicitud_foto;
    private CameraDevice.StateCallback estado_camara_listener = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //Comienza la captura de imagen cuando se abre la camara y liberamos el semaforo
            semaforo_abrir_cerrar_camara.release();
            camara = camera;
            comenzarCapturaImagen();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            //Si la camara se separa del hardware se cierra
            semaforo_abrir_cerrar_camara.release();
            camara.close();
            camara = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            //Si ocurre un error con la camara se cierra
            semaforo_abrir_cerrar_camara.release();
            camara.close();
            camara = null;
        }

    };
    private final ImageReader.OnImageAvailableListener imagen_disponible_listener =
            new ImageReader.OnImageAvailableListener() {

                @Override
                public void onImageAvailable(ImageReader lector) {
                    //Si es posible leer en el almacenamiento externo guardamos la imagen
                    if (foto_tomada != null)
                        foto_tomada.close();
                    foto_tomada = lector.acquireLatestImage();
                    if (escritura_posible) {
                        //Obtenemos la imagen y el nombre por default
                        String nombre_foto = Util.obtenerFecha("ddMMyyyyHHmmss") + getString(R.string.app_name);

                        //Si elegio autonombrar fotos en preferencias se pone el nombre por default
                        if (autonombrar_fotos) {
                            Toast.makeText(ActivityPrincipal.this, getString(R.string.toast_foto_tomada), Toast.LENGTH_SHORT).show();
                            Location ubicacion = obtenerUbicacion();
                            LatLng lat_y_long = null;
                            if (ubicacion != null) {
                                lat_y_long = new LatLng(ubicacion.getLatitude(), ubicacion.getLongitude());
                            }
                            handler.post(new GuardadorImagen(foto_tomada, new File(archivo + File.separator + nombre_foto +
                                    EXTENSION_ARCHIVO_FOTO), nombre_foto, Util.obtenerFecha("dd-MM-yyyy"),
                                    lat_y_long.latitude, lat_y_long.longitude));
                            dejarDeEnfocar();
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

            };
    private CameraCaptureSession.CaptureCallback captura_imagen_listener = new CameraCaptureSession.CaptureCallback() {

        /**
         * procesar(CaptureResult resultado): de acuerdo al resultado obtenido, realiza las acciones determinadas
         * con la imagen obtenida.
         * @param resultado resultado regresado por la camara.
         */
        private void procesar(CaptureResult resultado) {
            switch (estado_actual_camara) {
                //Si estamos en modo preview no hacemos nada especial
                case ESTADO_PREVIEW: {
                    break;
                }
                //Si aun no se enfoca
                case ESTADO_ESPERANDO_ENFOQUE: {
                    //Dependiendo del estao del enfoque y exposicion de la imagen decide si tomar foto o iniciar
                    //una precaptura
                    Integer estado_enfoque_automatico = resultado.get(CaptureResult.CONTROL_AF_STATE);
                    if (estado_enfoque_automatico == null) {
                        tomarFoto();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == estado_enfoque_automatico ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == estado_enfoque_automatico ||
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == estado_enfoque_automatico) {
                        Integer estado_exposicion_automatica = resultado.get(CaptureResult.CONTROL_AE_STATE);
                        if (estado_exposicion_automatica == null ||
                                estado_exposicion_automatica == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            estado_actual_camara = ESTADO_FOTO_TOMADA;
                            tomarFoto();
                        } else {
                            tomarSecuenciaPrecaptura();
                        }
                    }
                    break;
                }
                //Si aun no comienza la precaptura
                case ESTADO_ESPERANDO_PRECAPTURA: {
                    //Si se esta tomando una precaptura
                    Integer estado_exposicion_automatica = resultado.get(CaptureResult.CONTROL_AE_STATE);
                    if (estado_exposicion_automatica == null ||
                            estado_exposicion_automatica == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            estado_exposicion_automatica == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        estado_actual_camara = ESTADO_ESPERANDO_NO_PRECAPTURA;
                    }
                    break;
                }
                //Si ya termino la precaptura
                case ESTADO_ESPERANDO_NO_PRECAPTURA: {
                    //Si el estado de exposicion ya esta listo, toma la foto
                    Integer estado_exposicion_automatica = resultado.get(CaptureResult.CONTROL_AE_STATE);
                    if (estado_exposicion_automatica == null ||
                            estado_exposicion_automatica != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        estado_actual_camara = ESTADO_FOTO_TOMADA;
                        tomarFoto();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            procesar(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            procesar(result);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);

        //Directorio de las fotos
        archivo = obtenerDirectorioFotos();
        contenedor_imagen_camara = (TextureView) findViewById(R.id.contenedor_imagen_camara);
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

        //Se inicia el hilo que ayudara con las acciones en bacgkground y se prepara el contenedor
        iniciarHiloBackground();
        if (contenedor_imagen_camara.isAvailable())
            abrirCamara();
        else
            contenedor_imagen_camara.setSurfaceTextureListener(this);
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this);
        autonombrar_fotos = preferencias.getBoolean(ActivityPreferencias.PREFERENCIA_AUTONOMBRAR_FOTO_KEY, false);
    }

    @Override
    protected void onPause() {
        //Cierra la camara y termina con los procesos de background
        cerrarCamara();
        terminarHiloBackground();

        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {     //Capturamos el item seleccionado por el usuario del menu (preferncias o mapa)
            case R.id.preferencias:
                startActivity(new Intent(this, ActivityPreferencias.class));
                return true;
            case R.id.mapa:
                //Informamos a la activity si se puede acceder al almacenamiento externo
                Intent intent = new Intent(this, ActivityMapa.class);
                intent.putExtra(EXTRA_LECTURA_POSIBLE, lectura_posible);
                intent.putExtra(EXTRA_ESCRITURA_POSIBLE, escritura_posible);
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

    //Metodos de la interfaz SurfaceTextureListener
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        //Cuando la Surface se cargue, comienza la apertura de la camara
        abrirCamara();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void nombreSeleccionado(String nombre) {
        //Una vez que el usuario seleccione el nombre, se guarda la imagen y deja de enfocar
        Toast.makeText(ActivityPrincipal.this, getString(R.string.toast_foto_tomada), Toast.LENGTH_SHORT).show();
        Location ubicacion = obtenerUbicacion();
        LatLng lat_y_long = null;
        if (ubicacion != null) {
            lat_y_long = new LatLng(ubicacion.getLatitude(), ubicacion.getLongitude());
        }
        handler.post(new GuardadorImagen(foto_tomada, new File(archivo + File.separator + nombre + EXTENSION_ARCHIVO_FOTO), nombre,
                Util.obtenerFecha("dd-MM-yyyy"), lat_y_long.latitude, lat_y_long.longitude));
        dejarDeEnfocar();
    }

    @Override
    public void nombreCancelado() {
        //Si el usuario cierra el dialogo de alguna forma, no guardamos la foto
        dejarDeEnfocar();
    }

    /**
     * iniciarHiloBackground: Obtiene una instancia Handler y la asigna a la global handler para su uso.
     */
    private void iniciarHiloBackground() {
        hilo_background = new HandlerThread(getString(R.string.nombre_hilo_camara));
        hilo_background.start();
        handler = new Handler(hilo_background.getLooper());
    }

    /**
     * terminarHiloBackground: Cierra el hilo de backgroun y el handler de forma no peligrosa.
     */
    private void terminarHiloBackground() {
        hilo_background.quitSafely();
        try {
            hilo_background.join();
            hilo_background = null;
            handler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * abrirCamara: procedimiento que obtiene las caracteristicas y configuraciones de la camara.
     */
    private void abrirCamara() {
        //Primero nos aseguramos de tener permiso del usuario para acceder a la camara
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISO_ACCESO_CAMARA);
        }

        //Configuramos las variables y obtenemos el id de la camara principal
        String id_camara = obtenerCaracteristicasCamara();

        //Abrimos la camara utilizando el handler y nos apoyamos de un semaforo para bloquear apertura y cerrado
        //de la camara
        CameraManager administrador_camaras = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!semaforo_abrir_cerrar_camara.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                Log.e(LOG_TAG, "Demasiado tiempo esperando a la camara.");
            }
            if (id_camara != null)
                administrador_camaras.openCamera(id_camara, estado_camara_listener, handler);
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, "No se puede acceder a la camara: ");
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Se destruyo el hilo de la activity mientras se trataba de adquirir el semaforo: ");
            e.printStackTrace();
        }
    }

    /**
     * obtenerCaracteristicasCamara: obtenemos las caracteristicas de la camara principal del smartphone y con
     * estas prepara las variables para mostrar, capturar y guardar imagenes.
     * @return id de la camara principal.
     */
    private String obtenerCaracteristicasCamara() {
        //Obtenemos las caracteristicas de la camara principal
        CameraManager administrador_camaras = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String id_camara_1 = administrador_camaras.getCameraIdList()[0];
            CameraCharacteristics caracteristicas_camara = administrador_camaras.getCameraCharacteristics(id_camara_1);
            StreamConfigurationMap configuraciones =
                    caracteristicas_camara.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (configuraciones == null)
                return null;

            //Usamos el tamano de imagen mas grande para las fotografias tomadas
            Size mas_grande = Collections.max(Arrays.asList(configuraciones.getOutputSizes(ImageFormat.JPEG)),
                    new CompararTamanosPorArea());

            //Configuramos al lector de las imagenes tomadas por la camara
            lector_imagen_fija = ImageReader.newInstance(mas_grande.getWidth(), mas_grande.getHeight(),
                    ImageFormat.JPEG, 2);
            lector_imagen_fija.setOnImageAvailableListener(imagen_disponible_listener, handler);

            //Obtenemos el tamano necesario del Surface
            tam_surface = configuraciones.getOutputSizes(SurfaceTexture.class)[0];

            //Verificamos si el hardware cuenta con flash
            Boolean flash_disponible = caracteristicas_camara.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            soporta_flash = flash_disponible == null ? false : flash_disponible;

            return id_camara_1;
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, "No se puede acceder a las caracteristicas de la camara: ");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * comenzarCapturaImagen: Configura el tamano de la Surface, inicia el constructor y crea un CaptureSession a
     * traves de un listener para comenzar con las capturas de imagen.
     */
    private void comenzarCapturaImagen() {
        try {
            //Configuramos el tamano del surface y obtenemos una instancia tipo Surface
            SurfaceTexture surface_texture = contenedor_imagen_camara.getSurfaceTexture();
            if (surface_texture != null)
                surface_texture.setDefaultBufferSize(tam_surface.getWidth(), tam_surface.getHeight());
            Surface surface = new Surface(surface_texture);

            //Obtenemos el constructor tipo CaptureRequest.Builder y le decimos que la salida sera el surface
            constructor_imagen_preview = camara.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            constructor_imagen_preview.addTarget(surface);

            //Creamos una CaptureSession, cuando termine de configurarse comenzamos a actualizar las capturas
            //en el surface
            camara.createCaptureSession(Arrays.asList(surface, lector_imagen_fija.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (null == camara)
                                return;

                            sesion_captura_imagen = cameraCaptureSession;
                            actualizarCapturaImagen();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(ActivityPrincipal.this, getString(R.string.captura_imagen_fallida),
                                    Toast.LENGTH_LONG).show();
                            Log.w(LOG_TAG, "No se pudo iniciar la sesion de capturas para la preview.");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, "No se puede acceder a la camara para iniciar el perview de capturas: ");
            e.printStackTrace();
        }
    }

    /**
     * actualizarCapturaImagen: Actualiza la sesion de captura de imagenes.
     */
    private void actualizarCapturaImagen() {
        try {
            //Configuramos el constructor como autoenfoque y autoflash
            constructor_imagen_preview.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            configurarAutoFlash(constructor_imagen_preview);

            //Enviamos un request para comenzar a recibir imagenes de forma repetitiva usando el handler
            solicitud_foto = constructor_imagen_preview.build();
            sesion_captura_imagen.setRepeatingRequest(solicitud_foto, captura_imagen_listener, handler);
        } catch (CameraAccessException e) {
            Log.w(LOG_TAG, "No se puede acceder a la camara para enviar el request del preview: ");
            e.printStackTrace();
        }
    }

    /**
     * enfocar: cambia el estado actual de la camara y vuelve a llamar a la sesion de captura para que haga las
     * acciones necesarias de enfoque por medio del handler.
     */
    private void enfocar() {
        try {
            //Actualiza el constructor, el estado y pide la captura por medio de la sesion
            constructor_imagen_preview.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            estado_actual_camara = ESTADO_ESPERANDO_ENFOQUE;
            sesion_captura_imagen.capture(constructor_imagen_preview.build(), captura_imagen_listener, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * tomarSecuenciaPrecaptura: Inicia una precaptura y vuelve a llamar a la sesion de captura para que haga las
     * acciones necesarias para configurar la exposicion por medio del handler.
     */
    private void tomarSecuenciaPrecaptura() {
        try {
            //Actualiza el constructor, el estado y pide la captura por medio de la sesion
            constructor_imagen_preview.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            estado_actual_camara = ESTADO_ESPERANDO_PRECAPTURA;
            sesion_captura_imagen.capture(constructor_imagen_preview.build(), captura_imagen_listener, handler);
        } catch (CameraAccessException e) {
            Log.w(LOG_TAG, "No se pudo acceder a la camara para iniciar la secuencia de precaptura.");
        }
    }

    /**
     * tomarFoto: Hace las configuraciones finales con un constructor de foto y toma la foto, despues la manda a
     * guardar.
     */
    private void tomarFoto() {
        try {
            if (null == camara || !permiso_acceso_camara)
                return;

            //Obtenemos un constructor para configurar la fotografia
            final CaptureRequest.Builder constructor_imagen_foto =
                    camara.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //Configura el autoenfoque, autoflash, orientacion de la foto (actualmente solo portrait) y el lector
            //de imagen fija para que guarde la fotografia tomada en la ruta de archivo
            constructor_imagen_foto.addTarget(lector_imagen_fija.getSurface());
            constructor_imagen_foto.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            configurarAutoFlash(constructor_imagen_foto);
            constructor_imagen_foto.set(CaptureRequest.JPEG_ORIENTATION, ORIENTACION_PORTRAIT);

            //Terminamos la sesion repetitiva y solicitamos la toma de la foto en le hilo actual
            sesion_captura_imagen.stopRepeating();
            sesion_captura_imagen.capture(constructor_imagen_foto.build(), null, null);
        } catch (CameraAccessException e) {
            Log.w(LOG_TAG, "No se pudo acceder a la camara para tomar la foto: ");
            e.printStackTrace();
        }
    }

    /**
     * dejarDeEnfocar: Deja de mantener el enfoque actual y vuelve al estado de preview.
     */
    private void dejarDeEnfocar() {
        try {
            //Vuelve a poner el constructor en las configuraciones iniciales y solicita una sesion repetitiva
            //nuevamente
            constructor_imagen_preview.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            configurarAutoFlash(constructor_imagen_preview);
            sesion_captura_imagen.capture(constructor_imagen_preview.build(), captura_imagen_listener, handler);
            estado_actual_camara = ESTADO_PREVIEW;
            sesion_captura_imagen.setRepeatingRequest(solicitud_foto, captura_imagen_listener, handler);
        } catch (CameraAccessException e) {
            Log.w(LOG_TAG, "No se pudo acceder a la camara para dejar de enfocar y regresar al modo preview: ");
            e.printStackTrace();
        }
    }

    /**
     * cerrarCamara: Cierra las instancias de la camara, sesion de captura y lector de imagen, bloqueando el
     * semaforo.
     */
    private void cerrarCamara() {
        try {
            semaforo_abrir_cerrar_camara.acquire();
            if (null != sesion_captura_imagen) {
                sesion_captura_imagen.close();
                sesion_captura_imagen = null;
            }
            if (null != camara) {
                camara.close();
                camara = null;
            }
            if (null != lector_imagen_fija) {
                lector_imagen_fija.close();
                lector_imagen_fija = null;
            }
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "No se pudo cerrar la camara.");
            e.printStackTrace();
        } finally {
            semaforo_abrir_cerrar_camara.release();
        }
    }

    /**
     * foto: obtiene una captura de imagen de la camara. Solo se llama cuando el usuario pulsa el FAB "Camara".
     * @param boton_camara: vista del FAB
     */
    public void foto(View boton_camara) {
        enfocar();
    }

    /**
     * configurarAutoFlash: verifica la variable que indica si el hardware cuenta con flash y configura el
     * constructor del request para solicitar el autoflash.
     * @param constructor_request el constructor que se va  aconfigurar como autoflash
     */
    private void configurarAutoFlash(CaptureRequest.Builder constructor_request) {
        if (soporta_flash)
            constructor_request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
    }

    /**
     * obtenerDirectorioFotos: verifica que sea posible guardar datos en el almacenamiento externo y regresa el directorio donde
     * se guardaran las fotos.
     * @return File con el archivo donde se almacenan las fotos, si no es posible leer ni guardar regresa null
     */
    private File obtenerDirectorioFotos() {
        String estado_almacenamiento = Environment.getExternalStorageState();
        //Guardamos en sus variables respectivas si podemos escribir y leer del almacenamiento
        if (!Environment.MEDIA_MOUNTED.equals(estado_almacenamiento)) {
            escritura_posible = false;
            Log.w(LOG_TAG, "No se puede escribir en el almacenamiento externo.");
            if (!Environment.MEDIA_MOUNTED_READ_ONLY.equals(estado_almacenamiento)) {
                lectura_posible = false;
                Log.e(LOG_TAG, "No se puede leer del almacenamiento externo.");
                return null;
            }
        }
        //Obtenemos el directorio de fotogragias con el nombre de la app
        File directorio = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), NOMBRE_ALBUM_FOTOS);
        if (!directorio.mkdirs()) {
            Log.e(LOG_TAG, "No se pudo crear el directorio.");
        }
        return directorio;
    }

    /**
     * obtenerUbicacion: Regresa la ultima ubicacion conocida del usuario.
     * @return Location con la ubicacion del usuario.
     */
    private Location obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISO_ACCESO_UBICACION);
        }
        return ((LocationManager) getSystemService(Context.LOCATION_SERVICE)).getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * clase GuardadorImagen: implementa un runnable para guardar una imagen en un fichero determinado.
     */
    private class GuardadorImagen implements Runnable {

        //Imagen y fichero
        private final Image imagen_a_guardar;
        private final File fichero;
        private final String nombre_foto, fecha;
        private final double latitud, longitud;

        /**
         * GuardadorImagen: Constructor que requiere la imagen y el fichero donde guardarla.
         * @param imagen: imagen a guardar.
         * @param fichero: ruta donde guardar.
         */
        public GuardadorImagen(Image imagen, File fichero, String nombre_foto, String fecha, double latitud, double longitud) {
            imagen_a_guardar = imagen;
            this.fichero = fichero;
            this.nombre_foto = nombre_foto;
            this.fecha = fecha;
            this.latitud = latitud;
            this.longitud = longitud;
        }

        @Override
        public void run() {
            //Obtiene los bytes de la imagen
            ByteBuffer buffer = imagen_a_guardar.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            //Escribe los bytes en el fichero de salida
            FileOutputStream salida = null;
            try {
                salida = new FileOutputStream(fichero);
                salida.write(bytes);

                ContentValues values = new ContentValues();
                values.put(ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE, nombre_foto);
                values.put(ContratoPhotoMapp.Fotos.COLUMNA_FECHA, fecha);
                values.put(ContratoPhotoMapp.Fotos.COLUMNA_LATITUD, latitud);
                values.put(ContratoPhotoMapp.Fotos.COLUMNA_LONGITUD, longitud);
                getContentResolver().insert(ContratoPhotoMapp.Fotos.CONTENT_URI, values);
            } catch (IOException e) {
                Log.e(LOG_TAG, "No se pudo guardar la foto: ");
                e.printStackTrace();
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

    }

    /**
     * clase CompararTamanosPorArea: Compara dos areas diferentes.
     */
    static class CompararTamanosPorArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            //Regresa la resta de las areas
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }

    }
}