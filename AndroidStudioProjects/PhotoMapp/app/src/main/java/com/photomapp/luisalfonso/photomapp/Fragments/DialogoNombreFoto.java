package com.photomapp.luisalfonso.photomapp.Fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.photomapp.luisalfonso.photomapp.R;

/**
 * Clase DialogoNombreFoto: modela el dialogo que se muestra cada que se capture una foto para
 * elegir el nombre de la misma.
 */
public class DialogoNombreFoto extends DialogFragment {

    //Macro
    private static final String KEY_TITULO_FOTO = "titulo_foto";

    //Bandera para determinar si el usuario acepto cambiar el nombre
    private boolean nombre_seleccionado = false;

    //Listener de los eventos del dialogo
    private NombreSeleccionadoListener listener;

    /**
     * nuevoDialogo: crea una nueva instancia de un dialogo y recibe como parametro el titulo que
     * se debe de poner por default para la foto
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

        //Obtenemos el listener (activity que implementa la interfaz)
        try {
            listener = (NombreSeleccionadoListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " se debe implementar " +
                    "NombreSeleccionadoListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View layout_dialogo = inflater.inflate(R.layout.dialogo_nombre_foto, container, false);

        //Se recupera el titulo por default de la foto y se pone como hint
        Bundle bundle_argumentos = getArguments();
        String titulo_foto = null;
        if (bundle_argumentos != null) {
            titulo_foto = bundle_argumentos.getString(KEY_TITULO_FOTO);
        }
        final EditText nombre_foto =
                (EditText) layout_dialogo.findViewById(R.id.nombre_foto_dialogo);
        nombre_foto.setHint(titulo_foto);

        //Si se pulsa aceptar se usa el listener para pasar el nombre escrito en el cuadro de texto
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

        //Mostramos el teclado para que el usuario ponga el nombre de la foto
        getDialog().getWindow().
                setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return layout_dialogo;
    }

    @Override
    public void onDismiss(DialogInterface dialog){
        super.onDismiss(dialog);

        //Si no se pulso aceptar se avisa a la activity que se cancelo el dialogo
        if (!nombre_seleccionado) {
            listener.nombreCancelado();
        }
    }

    /**
     * NombreSeleccionadoListener: Para escuchar los eventos del dialogo.
     */
    public interface NombreSeleccionadoListener {
        void nombreSeleccionado(String nombre);
        void nombreCancelado();
    }

}