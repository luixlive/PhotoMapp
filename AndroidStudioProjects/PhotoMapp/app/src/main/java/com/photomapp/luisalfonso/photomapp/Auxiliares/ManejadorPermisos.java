package com.photomapp.luisalfonso.photomapp.Auxiliares;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import com.photomapp.luisalfonso.photomapp.Fragments.DialogoAceptarCancelar;
import com.photomapp.luisalfonso.photomapp.R;

import java.util.ArrayList;

/**
 * Clase ManejadorPermisos: Funciones estaticas que checan el estado de los permisos del usuario
 * y realiza las acciones necesarias de acuerdo a los estados de estos.
 */
public class ManejadorPermisos {

    //Macros publicas para la solicitud de los permisos
    public static final String PERMISO_ACCESO_ALMACENAMIENTO_EXTERNO =
            Manifest.permission.WRITE_EXTERNAL_STORAGE;
    public static final String PERMISO_ACCESO_CAMARA = Manifest.permission.CAMERA;
    public static final String PERMISO_ACCESO_UBICACION = Manifest.permission.ACCESS_FINE_LOCATION;

    private final static String ETIQUETA_DIALOGO_PERMISOS = "dialogo_permisos";

    /**
     * checarPermisos: Checa el estado de multiples permisos, y de acuerdo a cada uno, se muestran
     * los dialogos necesarios para pedir los permisos del usuario
     * @param permisos int[] con los permisos necesarios usando las macros publicas
     * @param activity Activity de donde se llama el metodo
     * @param id_peticion int id que hay que invocar al pedir al sistema operativo
     * @param mensaje_dialogo String mensaje que se mostrara en caso de necesitar un dialogo
     *                        que explique al usuario porque se requieren los permisos
     * @return true si ya existen los permisos, false de otro modo
     */
    public static boolean checarPermisos(String[] permisos, final Activity activity,
                                         final int id_peticion, final String mensaje_dialogo){
        //Se checa cada permiso y se separan los que ya se pueden solicitar, y los que primero
        //hay que explicar al usuario porque se requieren
        final ArrayList<String> permisos_dialogo_explicacion = new ArrayList<>();
        final ArrayList<String> pedir_permisos = new ArrayList<>();
        for (String permiso: permisos){
            if (ActivityCompat.checkSelfPermission(activity, permiso) !=
                    PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permiso)) {
                    permisos_dialogo_explicacion.add(permiso);
                } else {
                    pedir_permisos.add(permiso);
                }
            }
        }

        if (permisos_dialogo_explicacion.size() > 0){
            //Es necesario crear un dialogo explicando porque se requieren los permisos
            DialogoAceptarCancelar dialogo = DialogoAceptarCancelar.nuevaInstancia(
                    activity.getString(R.string.titulo_dialogo_permisos),
                    mensaje_dialogo
            );
            dialogo.setDecisionUsuarioListener(
                    new DialogoAceptarCancelar.DecisionUsuarioListener() {
                        @Override
                        public void aceptado() {
                            //El usuario dio acepto, iniciamos la peticion de los permisos
                            pedir_permisos.addAll(permisos_dialogo_explicacion);
                            iniciarPeticion(activity,
                                    pedir_permisos.toArray(new String[pedir_permisos.size()]),
                                    id_peticion
                            );
                        }

                        @Override
                        public void cancelado() {
                            //El usuario cancelo, solo se piden los permisos que no requieren
                            //explicacion
                            if (pedir_permisos.size() > 0) {
                                iniciarPeticion(activity,
                                        pedir_permisos.toArray(new String[pedir_permisos.size()]),
                                        id_peticion
                                );
                            }
                        }
                    });
            dialogo.show(activity.getFragmentManager(), ETIQUETA_DIALOGO_PERMISOS);
        } else if (pedir_permisos.size() > 0){
            //No hay necesidad de explicar los permisos
            iniciarPeticion(activity, pedir_permisos.toArray(new String[pedir_permisos.size()]),
                    id_peticion);
        } else{
            return true;
        }
        return false;
    }

    /**
     * iniciarPeticion: Se hace la peticion al sistema para que el usuario acepte
     * @param activity Activity contexto actual
     * @param permisos String[] permisos del Manifest necesarios
     * @param id_peticion int id para el callback al terminar
     */
    private static void iniciarPeticion(Activity activity, String[] permisos, int id_peticion){
        ActivityCompat.requestPermissions(activity, permisos, id_peticion);
    }

    /**
     * checarPermisoAlmacenamiento: Confirma que el usuario haya dado permiso para utilizar el
     * almacenamiento externo
     * @param activity Activity contexto actual
     * @return true si hay permiso, falso de otro modo
     */
    public static boolean checarPermisoAlmacenamiento(final Activity activity){
        return ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * checarPermisoCamara: Confirma que el usuario haya dado permiso para utilizar la camara
     * @param activity Activity contexto actual
     * @return true si hay permiso, falso de otro modo
     */
    public static boolean checarPermisoCamara(final Activity activity){
        return ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * checarPermisoUbicacion: Confirma que el usuario haya dado permiso para utilizar la ubicacion
     * @param activity Activity contexto actual
     * @return true si hay permiso, falso de otro modo
     */
    public static boolean checarPermisoUbicacion(final Activity activity){
        return ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

}