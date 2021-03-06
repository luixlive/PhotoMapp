package com.photomapp.luisalfonso.photomapp.Auxiliares;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
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
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.photomapp.luisalfonso.photomapp.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Clase ManejadorCamara: Se encarga de abrir correctamente la camara y mostrar las imagenes en
 * streaming en el contenedor que se le indique en el constructor, asi como tomar fotos y regresar
 * el resultado por medio de la interfaz TomarFotoListener
 */
public class ManejadorCamara {

    //Etiqueta para la escritura al LOG
    private static final String LOG_TAG = "MANEJADOR_CAMARA";

    //Macros para identificar el estado actual de la camara
    private static final int ESTADO_PREVIEW = 0;
    private static final int ESTADO_ESPERANDO_ENFOQUE = 1;
    private static final int ESTADO_ESPERANDO_PRECAPTURA = 2;
    private static final int ESTADO_ESPERANDO_NO_PRECAPTURA = 3;
    private static final int ESTADO_FOTO_TOMADA = 4;

    //Macros para indicar el angulo de la foto y el tiempo maximo de espera de la camara
    private static final int ORIENTACION_PORTRAIT = 90;
    private static final int CAMARA_TIEMPO_ESPERA = 2500;

    //Bandera para saber si el proceso esta corriendo actualmente
    private boolean proceso_iniciado = false;

    //Listener al que se le comunican los cambios
    private TomarFotoListener listener;

    //Variables auxiliares para el streaming de las imagenes
    private TextureView contenedor_imagenes;
    private Size tam_surface;
    private Activity activity_padre;

    //Variables para las acciones que se ejecutaran en background
    private HandlerThread hilo_background;
    private Handler handler;

    //Estado inicial de la camara
    private int estado_actual_camara = ESTADO_PREVIEW;

    //Variables para la captura de la foto
    private ImageReader lector_imagen_fija;
    private Image foto_tomada;

    //Variables para el uso del Hardware de la camara
    private CameraDevice camara;
    private CaptureRequest.Builder constructor_imagen_preview;
    private CameraCaptureSession sesion_captura_imagen;
    private CaptureRequest solicitud_foto;

    //Listener para los eventos de la captura de imagenes
    private CameraCaptureSession.CaptureCallback captura_imagen_listener =
            new CameraCaptureSession.CaptureCallback() {

                /**
                 * procesar(CaptureResult resultado): de acuerdo al resultado obtenido, realiza las
                 * acciones determinadas con la imagen obtenida.
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
                            //Se checa enfoque y exposicion para decidir si tomar la foto
                            Integer estado_enfoque_automatico =
                                    resultado.get(CaptureResult.CONTROL_AF_STATE);
                            if (estado_enfoque_automatico == null) {
                                tomarFoto();
                            } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ==
                                    estado_enfoque_automatico ||
                                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED ==
                                            estado_enfoque_automatico ||
                                    CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED ==
                                            estado_enfoque_automatico) {

                                Integer estado_exposicion_automatica =
                                        resultado.get(CaptureResult.CONTROL_AE_STATE);

                                if (estado_exposicion_automatica == null ||
                                        estado_exposicion_automatica ==
                                                CaptureResult.CONTROL_AE_STATE_CONVERGED) {
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
                            Integer estado_exposicion_automatica =
                                    resultado.get(CaptureResult.CONTROL_AE_STATE);
                            if (estado_exposicion_automatica == null ||
                                    estado_exposicion_automatica ==
                                            CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                                    estado_exposicion_automatica ==
                                            CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                                estado_actual_camara = ESTADO_ESPERANDO_NO_PRECAPTURA;
                            }
                            break;
                        }
                        //Si ya termino la precaptura
                        case ESTADO_ESPERANDO_NO_PRECAPTURA: {
                            //Si el estado de exposicion ya esta listo, toma la foto
                            Integer estado_exposicion_automatica =
                                    resultado.get(CaptureResult.CONTROL_AE_STATE);
                            if (estado_exposicion_automatica == null ||
                                    estado_exposicion_automatica !=
                                            CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                                estado_actual_camara = ESTADO_FOTO_TOMADA;
                                tomarFoto();
                            }
                            break;
                        }
                    }
                }

                @Override
                public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                                @NonNull CaptureRequest request,
                                                @NonNull CaptureResult partialResult) {
                    procesar(partialResult);
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    procesar(result);
                }

            };

    //Banderas para el uso del flash
    private boolean soporta_flash;
    private boolean preferencia_usuario_flash = false;

    //Semaforo para restringir la apertura y cierre de la camara
    private Semaphore semaforo_abrir_cerrar_camara = new Semaphore(1);

    public ManejadorCamara(Activity activity, TextureView contenedor_imagenes) {
        this.contenedor_imagenes = contenedor_imagenes;
        activity_padre = activity;
    }

    /**
     * iniciar: Inicia el proceso de captura de imagen.
     */
    public void iniciar() {
        if (!proceso_iniciado) {
            iniciarHiloBackground();
            if (contenedor_imagenes.isAvailable()) {
                abrirCamara();
            } else {
                contenedor_imagenes.setSurfaceTextureListener(
                        new TextureView.SurfaceTextureListener() {

                            @Override
                            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                                                                  int height) {
                                //Cuando la Surface se cargue, comienza la apertura de la camara
                                abrirCamara();
                            }

                            @Override
                            public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                                    int width, int height) {}

                            @Override
                            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                                return false;
                            }

                            @Override
                            public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
                });
            }
            proceso_iniciado = true;
        }
    }

    /**
     * terminar: Termina los procesos de background y cierra la camara.
     */
    public void terminar() {
        if (proceso_iniciado) {
            cerrarCamara();
            terminarHiloBackground();
            proceso_iniciado = false;
        }
    }

    /**
     * foto: Inicia el proceso de captura de foto.
     */
    public void foto() {
        enfocar();
    }

    /**
     * fotoTerminada: Termina el proceso de toma de foto y regresa al estado preview.
     */
    public void fotoTerminada() {
        dejarDeEnfocar();
    }

    /**
     * abrirCamara: procedimiento que obtiene las caracteristicas y configuraciones de la camara.
     */
    private void abrirCamara() {
        //Es necesario volver a checar el permiso porque Android pide que se haga de forma
        //explicita, pero sabemos que ya deberiamos tener el permiso
        if (ActivityCompat.checkSelfPermission(activity_padre, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED) {
            //Configuramos las variables y obtenemos el id de la camara principal
            String id_camara = obtenerCaracteristicasCamara();

            CameraManager administrador_camaras =
                    (CameraManager)activity_padre.getSystemService(Context.CAMERA_SERVICE);
            try {
                if (!semaforo_abrir_cerrar_camara.tryAcquire(CAMARA_TIEMPO_ESPERA,
                        TimeUnit.MILLISECONDS)) {
                    Log.e(LOG_TAG, "Demasiado tiempo esperando a la camara.");
                    return;
                }
                if (id_camara != null) {
                    administrador_camaras.openCamera(id_camara, new CameraDevice.StateCallback() {

                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            //Comienza la captura de imagen cuando se abre la camara
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

                    }, handler);
                }
            } catch (CameraAccessException e) {
                Log.e(LOG_TAG, "No se puede acceder a la camara: ");
                e.printStackTrace();
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Se destruyo el hilo de la activity mientras se trataba de " +
                        "adquirir el semaforo: ");
                e.printStackTrace();
            }
        }
    }

    /**
     * obtenerCaracteristicasCamara: obtenemos las caracteristicas de la camara principal del
     * smartphone y con estas prepara las variables para mostrar, capturar y guardar imagenes.
     * @return id de la camara principal.
     */
    private String obtenerCaracteristicasCamara() {
        //Obtenemos las caracteristicas de la camara principal
        CameraManager administrador_camaras =
                (CameraManager) activity_padre.getSystemService(Context.CAMERA_SERVICE);
        try {
            String id_camara_1 = administrador_camaras.getCameraIdList()[0];
            CameraCharacteristics caracteristicas_camara =
                    administrador_camaras.getCameraCharacteristics(id_camara_1);
            StreamConfigurationMap configuraciones = caracteristicas_camara.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (configuraciones == null) {
                return null;
            }

            //Usamos el tamano de imagen mas grande para las fotografias tomadas
            Size mas_grande =
                    Collections.max(Arrays.asList(configuraciones.getOutputSizes(ImageFormat.JPEG)),
                    new CompararTamanosPorArea());

            //Configuramos al lector de las imagenes tomadas por la camara
            lector_imagen_fija = ImageReader.newInstance(mas_grande.getWidth(),
                    mas_grande.getHeight(), ImageFormat.JPEG, 2);
            lector_imagen_fija.setOnImageAvailableListener(
                    new ImageReader.OnImageAvailableListener() {

                @Override
                public void onImageAvailable(ImageReader lector) {
                    //Si es posible leer en el almacenamiento externo guardamos la imagen
                    if (foto_tomada != null) {
                        foto_tomada.close();
                    }
                    foto_tomada = lector.acquireLatestImage();
                    if (listener != null) {
                        listener.fotoTomada(foto_tomada);
                    }
                }

            }, handler);

            //Obtenemos el tamano necesario del Surface
            tam_surface = configuraciones.getOutputSizes(SurfaceTexture.class)[0];

            //Verificamos si el hardware cuenta con flash
            Boolean flash_disponible =
                    caracteristicas_camara.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            soporta_flash = flash_disponible == null ? false : flash_disponible;

            return id_camara_1;
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, "No se puede acceder a las caracteristicas de la camara: ");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * comenzarCapturaImagen: Configura el tamano de la Surface, inicia el constructor y crea un
     * CaptureSession a traves de un listener para comenzar con las capturas de imagen.
     */
    private void comenzarCapturaImagen() {
        try {
            //Configuramos el tamano del surface y obtenemos una instancia tipo Surface
            SurfaceTexture surface_texture = contenedor_imagenes.getSurfaceTexture();
            if (surface_texture != null) {
                surface_texture.setDefaultBufferSize(tam_surface.getWidth(),
                        tam_surface.getHeight());
            }
            Surface surface = new Surface(surface_texture);

            //Obtenemos el constructor de CaptureRequest y le decimos que la salida sera el surface
            constructor_imagen_preview = camara.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            constructor_imagen_preview.addTarget(surface);

            //Creamos una CaptureSession, cuando termine de configurarse comenzamos a actualizar
            //las capturas en el surface
            camara.createCaptureSession(Arrays.asList(surface, lector_imagen_fija.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            if (null == camara) {
                                return;
                            }
                            sesion_captura_imagen = cameraCaptureSession;
                            actualizarCapturaImagen();
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(activity_padre, activity_padre.getString(R.string.captura_imagen_fallida),
                                    Toast.LENGTH_LONG).show();
                            Log.w(LOG_TAG, "No se pudo iniciar la sesion para la preview.");
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
            constructor_imagen_preview.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            configurarAutoFlash(constructor_imagen_preview);

            //Request para comenzar a recibir imagenes de forma repetitiva usando el handler
            solicitud_foto = constructor_imagen_preview.build();
            sesion_captura_imagen.setRepeatingRequest(solicitud_foto, captura_imagen_listener,
                    handler);
        } catch (CameraAccessException e) {
            Log.w(LOG_TAG, "No se puede acceder a la camara para enviar el request del preview: ");
            e.printStackTrace();
        }
    }

    /**
     * enfocar: cambia el estado actual de la camara y vuelve a llamar a la sesion de captura para
     * que haga las acciones necesarias de enfoque por medio del handler.
     */
    private void enfocar() {
        try {
            //Actualiza el constructor, el estado y pide la captura por medio de la sesion
            constructor_imagen_preview.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            estado_actual_camara = ESTADO_ESPERANDO_ENFOQUE;
            sesion_captura_imagen.capture(constructor_imagen_preview.build(),
                    captura_imagen_listener, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * tomarSecuenciaPrecaptura: Inicia una precaptura y vuelve a llamar a la sesion de captura
     * para que haga las acciones necesarias para configurar la exposicion por medio del handler.
     */
    private void tomarSecuenciaPrecaptura() {
        try {
            //Actualiza el constructor, el estado y pide la captura por medio de la sesion
            constructor_imagen_preview.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            estado_actual_camara = ESTADO_ESPERANDO_PRECAPTURA;
            sesion_captura_imagen.capture(constructor_imagen_preview.build(),
                    captura_imagen_listener, handler);
        } catch (CameraAccessException e) {
            Log.w(LOG_TAG, "No se pudo acceder a la camara para iniciar la secuencia de precaptura.");
        }
    }

    /**
     * tomarFoto: Hace las configuraciones finales con un constructor de foto y toma la foto,
     * despues la manda a guardar.
     */
    private void tomarFoto() {
        try {
            if (null == camara) {
                return;
            }

            //Obtenemos un constructor para configurar la fotografia
            final CaptureRequest.Builder constructor_imagen_foto =
                    camara.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            //Configura el autoenfoque, autoflash, orientacion de la foto (actualmente solo
            //portrait) y el lector de imagen fija para que guarde la fotografia tomada en la ruta
            // de archivo
            constructor_imagen_foto.addTarget(lector_imagen_fija.getSurface());
            constructor_imagen_foto.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
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
            //Vuelve a poner el constructor en las configuraciones iniciales y solicita una sesion
            //repetitiva nuevamente
            constructor_imagen_preview.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            configurarAutoFlash(constructor_imagen_preview);
            sesion_captura_imagen.capture(constructor_imagen_preview.build(),
                    captura_imagen_listener, handler);
            estado_actual_camara = ESTADO_PREVIEW;
            sesion_captura_imagen.setRepeatingRequest(solicitud_foto, captura_imagen_listener,
                    handler);
        } catch (CameraAccessException e) {
            Log.w(LOG_TAG, "No se pudo dejar de enfocar y regresar al modo preview: ");
            e.printStackTrace();
        }
    }

    /**
     * cerrarCamara: Cierra las instancias de la camara, sesion de captura y lector de imagen,
     * bloqueando el semaforo.
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
     * configurarAutoFlash: verifica la variable que indica si el hardware cuenta con flash y
     * configura el constructor del request para solicitar el autoflash.
     * @param constructor_request el constructor que se va  aconfigurar como autoflash
     */
    private void configurarAutoFlash(CaptureRequest.Builder constructor_request) {
        if (soporta_flash && preferencia_usuario_flash) {
            constructor_request.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    /**
     * iniciarHiloBackground: Obtiene una instancia Handler y la asigna a la global handler.
     */
    private void iniciarHiloBackground() {
        hilo_background = new HandlerThread(activity_padre.getString(R.string.nombre_hilo_camara));
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
     * cambiarPreferenciaFlash: Si el usuario cambia la preferencia de uso de flash, se debe
     * notificar al manejador por medio de este metodo.
     * @param usar_flash boolean con la preferencia del usuario.
     */
    public void cambiarPreferenciaFlash(boolean usar_flash){
        preferencia_usuario_flash = usar_flash;
    }

    /**
     * clase CompararTamanosPorArea: Compara dos areas diferentes.
     */
    static class CompararTamanosPorArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            //Regresa la resta de las areas
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() *
                    rhs.getHeight());
        }

    }

    public void setTomarFotoListener(TomarFotoListener listener){
        this.listener = listener;
    }

    /**
     * Interfaz TomarFotoListener: Debe ser implementada por quien usa el manejador para escuchar
     * el evento cuando se toma una foto.
     */
    public interface TomarFotoListener {
        void fotoTomada(Image foto);
    }

}
