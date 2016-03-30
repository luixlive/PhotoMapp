package com.photomapp.luisalfonso.photomapp.Auxiliares;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.photomapp.luisalfonso.photomapp.R;

/**
 * Clase ManejadorPermisos: Funciones estaticas que checan el estado de los permisos del usuario
 * y realiza las acciones necesarias de acuerdo a los estados de estos.
 */
public class ManejadorPermisos {

    private static final String LOG_TAG = "ManejadorPermisos";

    //Macros publicas para las etiquetas de los permisos al crear los dialogos de SO
    public static final int PERMISO_ACCESO_ALMACENAMIENTO_EXTERNO = 0;
    public static final int PERMISO_ACCESO_CAMARA = 1;
    public static final int PERMISO_ACCESO_UBICACION = 2;

    //Macros
    private static final String ETIQUETA_DIALOGO_PERMISO_ALMACENAMIENTO = "dialogo_almacenamiento";
    private static final String ETIQUETA_DIALOGO_PERMISO_CAMARA = "dialogo_camara";
    private static final String ETIQUETA_DIALOGO_PERMISO_UBICACION = "dialogo_ubicacion";

    /**
     * checarPermisoAlmacenamiento: Confirma que el usuario haya dado permiso para utilizar el
     * almacenamiento externo, de no ser asi se lo solicita
     * @param activity Activity contexto actual
     * @return true si hay permiso, falso de otro modo
     */
    public static boolean checarPermisoAlmacenamiento(final Activity activity){
        //Checamos si tenemos permiso
        int permiso = ActivityCompat.
                checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permiso != PackageManager.PERMISSION_GRANTED) {
            //Checamos si es necesario explicar al usuario porque se necesita el permiso
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                DialogoExplicacion dialogo = DialogoExplicacion.nuevaInstancia(
                        activity.getString(R.string.titulo_dialogo_permiso_almacenamiento),
                        activity.getString(R.string.mensaje_dialogo_permiso_almacenamiento)
                );
                dialogo.setDecisionUsuarioListener(
                        new DialogoExplicacion.DecisionUsuarioListener() {
                            @Override
                            public void aceptado() {
                                //El usuario dio acepto, iniciamos la peticion del permiso
                                String[] permisos = { Manifest.permission.WRITE_EXTERNAL_STORAGE };
                                ActivityCompat.requestPermissions(
                                        activity,
                                        permisos,
                                        PERMISO_ACCESO_ALMACENAMIENTO_EXTERNO
                                );
                            }

                            @Override
                            public void cancelado() {
                                //El usuario decidio no dar permiso
                                Log.w(LOG_TAG, "El usuario denego el dialogo informativo para permitir " +
                                        "acceso al almacenamiento");
                            }
                        });
                dialogo.show(activity.getFragmentManager(),
                        ETIQUETA_DIALOGO_PERMISO_ALMACENAMIENTO);
            } else {
                //No tenemos permiso para acceder al almacenamiento externo, lo pedimos
                String[] permisos = { Manifest.permission.WRITE_EXTERNAL_STORAGE };
                ActivityCompat.requestPermissions(
                        activity,
                        permisos,
                        PERMISO_ACCESO_ALMACENAMIENTO_EXTERNO
                );
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     * checarPermisoCamara: Confirma que el usuario haya dado permiso para utilizar la camara, de
     * no ser asi se lo solicita
     * @param activity Activity contexto actual
     * @return true si hay permiso, falso de otro modo
     */
    public static boolean checarPermisoCamara(final Activity activity){
        //Checamos si tenemos permiso
        int permiso = ActivityCompat.
                checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (permiso != PackageManager.PERMISSION_GRANTED) {
            //Checamos si es necesario explicar al usuario porque se necesita el permiso
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.CAMERA)) {
                DialogoExplicacion dialogo = DialogoExplicacion.nuevaInstancia(
                        activity.getString(R.string.titulo_dialogo_permiso_camara),
                        activity.getString(R.string.mensaje_dialogo_permiso_camara)
                );
                dialogo.setDecisionUsuarioListener(
                        new DialogoExplicacion.DecisionUsuarioListener() {
                            @Override
                            public void aceptado() {
                                //El usuario dio acepto, iniciamos la peticion del permiso
                                String[] permisos = { Manifest.permission.CAMERA };
                                ActivityCompat.requestPermissions(
                                        activity,
                                        permisos,
                                        PERMISO_ACCESO_CAMARA
                                );
                            }

                            @Override
                            public void cancelado() {
                                //El usuario decidio no dar permiso
                                Log.w(LOG_TAG, "El usuario denego el dialogo informativo para permitir " +
                                        "acceso a la cámara");
                            }
                        });
                dialogo.show(activity.getFragmentManager(), ETIQUETA_DIALOGO_PERMISO_CAMARA);
            } else {
                //No tenemos permiso para acceder a la camara, lo pedimos
                String[] permisos = { Manifest.permission.CAMERA };
                ActivityCompat.requestPermissions(
                        activity,
                        permisos,
                        PERMISO_ACCESO_CAMARA
                );
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     * checarPermisoUbicacion: Confirma que el usuario haya dado permiso para utilizar la ubicacion,
     * de no ser asi se lo solicita
     * @param activity Activity contexto actual
     * @return true si hay permiso, falso de otro modo
     */
    public static boolean checarPermisoUbicacion(final Activity activity){
        //Checamos si tenemos permiso
        int permiso = ActivityCompat.
                checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permiso != PackageManager.PERMISSION_GRANTED) {
            //Checamos si es necesario explicar al usuario porque se necesita el permiso
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                DialogoExplicacion dialogo = DialogoExplicacion.nuevaInstancia(
                        activity.getString(R.string.titulo_dialogo_permiso_ubicacion),
                        activity.getString(R.string.mensaje_dialogo_permiso_ubicacion)
                );
                dialogo.setDecisionUsuarioListener(
                        new DialogoExplicacion.DecisionUsuarioListener() {
                            @Override
                            public void aceptado() {
                                //El usuario dio acepto, iniciamos la peticion del permiso
                                String[] permisos = { Manifest.permission.ACCESS_FINE_LOCATION };
                                ActivityCompat.requestPermissions(
                                        activity,
                                        permisos,
                                        PERMISO_ACCESO_UBICACION
                                );
                            }

                            @Override
                            public void cancelado() {
                                //El usuario decidio no dar permiso
                                Log.w(LOG_TAG, "El usuario denego el dialogo informativo para " +
                                        "permitir acceso a la ubicación");
                            }
                        });
                dialogo.show(activity.getFragmentManager(), ETIQUETA_DIALOGO_PERMISO_UBICACION);
            } else {
                //No tenemos permiso para acceder a la ubicacion, lo pedimos
                String[] permisos = { Manifest.permission.ACCESS_FINE_LOCATION };
                ActivityCompat.requestPermissions(
                        activity,
                        permisos,
                        PERMISO_ACCESO_UBICACION
                );
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     * Clase DialogoExplicacion: Crea dialogos simples para explicar al usuario porque es necesario
     * que de permiso para utilizar determinado componente.
     */
    public static class DialogoExplicacion extends DialogFragment {

        //Macros
        private static final String TITULO_KEY = "titulo";
        private static final String MENSAJE_KEY = "mensaje";

        //Listener para escuchar eventos
        private DecisionUsuarioListener listener;

        /**
         * nuevaInstancia: Regresa una instancia de Dialogo lista para mostrarse
         * @param titulo String titulo del dialogo
         * @param mensaje String mensaje del dialogo
         * @return DialogoExplicacion creado
         */
        public static DialogoExplicacion nuevaInstancia(String titulo, String mensaje) {
            DialogoExplicacion frag = new DialogoExplicacion();
            Bundle args = new Bundle();
            args.putString(TITULO_KEY, titulo);
            args.putString(MENSAJE_KEY, mensaje);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            //Se capturan los argumentos y se regresa construye el dialogo
            String titulo = getArguments().getString(TITULO_KEY);
            String mensaje = getArguments().getString(MENSAJE_KEY);

            return new AlertDialog.Builder(getActivity())
                    .setTitle(titulo)
                    .setMessage(mensaje)
                    .setPositiveButton(getString(R.string.aceptar),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (listener != null) {
                                        listener.aceptado();
                                    }
                                }
                            })
                    .setNegativeButton(getString(R.string.cancelar),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (listener != null) {
                                        listener.cancelado();
                                    }
                                }
                            })
                    .create();
        }

        /**
         * setDecisionUsuarioListener: Subscribe un listener para escuchar los eventos del dialogo
         * @param listener DecisionUsuarioListener
         */
        public void setDecisionUsuarioListener(DecisionUsuarioListener listener){
            this.listener = listener;
        }

        /**
         * Interfaz DecisionUsuarioListener: Para escuchar los eventos del dialogo
         */
        public interface DecisionUsuarioListener {
            void aceptado();
            void cancelado();
        }
    }

}