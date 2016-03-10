package com.photomapp.luisalfonso.photomapp;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

/**
 * Clase DialogoNombreFoto: modela el dialogo que se muestra cada que se capture una foto para elegir el nombre
 * de la misma.
 */
public class DialogoNombreFoto extends DialogFragment {

    //Macro
    private static final String KEY_TITULO_FOTO = "titulo_foto";

    //Variables nombre seleccionado nos ayuda a saber cuando se cierre el dialogo si este fue cancelado o si el usuario
    //si cambio el nombre, y el listener es la interfaz que debe implementar la Activity para recibir los callbacks
    private boolean nombre_seleccionado = false;
    private NombreSeleccionadoListener listener;

    /**
     * nuevoDialogo: crea una nueva instancia de un dialogo y recibe como parametro el titulo que se debe de poner por
     * default para la foto
     * @param titulo_foto titulo a poner por default
     * @return el dialogo nuevo
     */
    public static DialogoNombreFoto nuevoDialogo(String titulo_foto) {
        DialogoNombreFoto dialogo = new DialogoNombreFoto();
        Bundle argumentos = new Bundle();
        //Guardamos en el bundle el titulo
        argumentos.putString(KEY_TITULO_FOTO, titulo_foto);
        dialogo.setArguments(argumentos);
        return dialogo;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        //Obtenemos el listener (activity que implementa la interfaz) para enviar informacion a traves de el
        try {
            listener = (NombreSeleccionadoListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " se debe implementar NombreSeleccionadoListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View layout_dialogo = inflater.inflate(R.layout.dialogo_nombre_foto, container, false);

        //Se recupera el titulo por default de la foto y se pone como hint
        Bundle bundle_argumentos = getArguments();
        String titulo_foto = null;
        if (bundle_argumentos != null) {
            titulo_foto = bundle_argumentos.getString(KEY_TITULO_FOTO);
        }
        final EditText nombre_foto = (EditText) layout_dialogo.findViewById(R.id.nombre_foto_dialogo);
        nombre_foto.setHint(titulo_foto);

        //Si se pulsa aceptar se usa el listener para pasar el nombre escrito en el cuadro de texto a la activity
        Button boton_aceptar = (Button) layout_dialogo.findViewById(R.id.boton_aceptar_dialogo);
        boton_aceptar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View boton) {
                String nombre = nombre_foto.getText().toString();
                if (nombre.isEmpty()) {
                    nombre = nombre_foto.getHint().toString();
                }
                listener.nombreSeleccionado(nombre);
                nombre_seleccionado = true;
                dismiss();
            }
        });

        return layout_dialogo;
    }

    @Override
    public void onDismiss(DialogInterface dialog){
        //Si no se pulso aceptar se avisa a la activity que se cancelo el dialogo
        if (!nombre_seleccionado)
            listener.nombreCancelado();
        super.onDismiss(dialog);
    }

    /**
     * NombreSeleccionadoListener: interfaz que se debe implementar para recibir informacion del dialogo.
     */
    public interface NombreSeleccionadoListener {
        void nombreSeleccionado(String nombre);
        void nombreCancelado();
    }

}