package com.photomapp.luisalfonso.photomapp.Fragments;

import android.app.DialogFragment;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.photomapp.luisalfonso.photomapp.Activities.ActivityPrincipal;
import com.photomapp.luisalfonso.photomapp.LectorBitmaps;
import com.photomapp.luisalfonso.photomapp.R;
import com.photomapp.luisalfonso.photomapp.Util;

import java.io.File;

/**
 * Created by luixlive on 11/03/16.
 */
public class DialogoMostrarFoto extends DialogFragment {

    //Macro
    private static final String KEY_RUTA_FOTO = "ruta_foto";
    private static final String KEY_FECHA_FOTO = "fecha_foto";
    private static final String KEY_CIUDAD_FOTO = "ciudad_foto";

    public static DialogoMostrarFoto nuevoDialogo(String nombre, String fecha, String ciudad) {
        DialogoMostrarFoto nuevo_fragment = new DialogoMostrarFoto();
        Bundle args = new Bundle();
        String ruta_foto = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator +
                Util.NOMBRE_ALBUM_FOTOS + File.separator + nombre + Util.EXTENSION_ARCHIVO_FOTO;
        args.putString(KEY_RUTA_FOTO, ruta_foto);
        args.putString(KEY_FECHA_FOTO, fecha);
        args.putString(KEY_CIUDAD_FOTO, ciudad);
        nuevo_fragment.setArguments(args);

        return nuevo_fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View layout_fragment_foto = inflater.inflate(R.layout.fragment_mostrar_foto, container, false);

        Bundle bundle_argumentos = getArguments();
        String ciudad_foto = null;
        String fecha = null;
        String ruta = null;
        if (bundle_argumentos != null) {
            ciudad_foto = bundle_argumentos.getString(KEY_CIUDAD_FOTO);
            fecha = bundle_argumentos.getString(KEY_FECHA_FOTO);
            ruta = bundle_argumentos.getString(KEY_RUTA_FOTO);
        }

        LectorBitmaps lector_imagen = LectorBitmaps.obtenerInstancia(getActivity());

        ImageView foto = (ImageView)layout_fragment_foto.findViewById(R.id.foto_seleccionada);
        lector_imagen.extraerImagenEn(getActivity(), foto, ruta, LectorBitmaps.IMAGEN_AMPLIADA);
        TextView nombre_ciudad = (TextView)layout_fragment_foto.findViewById(R.id.nombre_ciudad_foto);
        nombre_ciudad.setText(ciudad_foto);
        TextView fecha_foto = (TextView)layout_fragment_foto.findViewById(R.id.fecha_foto);
        fecha_foto.setText(fecha);

        return layout_fragment_foto;
    }

}
