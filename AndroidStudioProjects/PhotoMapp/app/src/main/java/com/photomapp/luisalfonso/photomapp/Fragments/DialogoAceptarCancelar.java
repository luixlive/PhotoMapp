package com.photomapp.luisalfonso.photomapp.Fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Window;

import com.photomapp.luisalfonso.photomapp.R;

/**
 * Clase DialogoAceptarCancelar: Crea un dialogo con titulo, mensaje y botones de aceptar y
 * cancelar
 */
public class DialogoAceptarCancelar extends DialogFragment {

    //Macros
    private static final String MENSAJE_KEY = "mensaje";
    private static final String TITULO_KEY = "titulo";

    //Listener para escuchar eventos
    private DecisionUsuarioListener listener;

    /**
     * nuevaInstancia: Obtiene un nuevo dialogo
     * @param titulo String titulo del dialogo
     * @param mensaje String se le mostrara al usuario en el dialogo
     * @return DialogoAceptarCancelar listo para mostrarse
     */
    public static DialogoAceptarCancelar nuevaInstancia(String titulo, String mensaje) {
        DialogoAceptarCancelar dialogo = new DialogoAceptarCancelar();
        Bundle argumentos = new Bundle();

        //Enviamos el titulo y el mensaje como argumentos
        argumentos.putString(TITULO_KEY, titulo);
        argumentos.putString(MENSAJE_KEY, mensaje);

        dialogo.setArguments(argumentos);
        return dialogo;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //Se capturan los argumentos
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        String titulo = getArguments().getString(TITULO_KEY);
        String mensaje = getArguments().getString(MENSAJE_KEY);

        //Se construye y regresa el dialogo
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