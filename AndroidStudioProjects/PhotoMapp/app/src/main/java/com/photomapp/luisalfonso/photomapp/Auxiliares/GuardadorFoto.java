package com.photomapp.luisalfonso.photomapp.Auxiliares;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Clase GuardadorFoto: Guarda fotos en un hilo en segundo plano.
 */
public class GuardadorFoto extends AsyncTask<Void, Void, Boolean> {

    private static final String LOG_TAG = "GuardadorFoto";

    //Macro
    private static final String TIPO_MIME = "image/jpeg";

    //Fichero e informacion para el registro
    private final String nombre;
    private Bitmap foto;
    private ContentResolver cr;

    //Escuchador de eventos
    private GuardarFotoListener listener;

    public GuardadorFoto(String nombre, Bitmap foto, ContentResolver cr) {
        this.nombre = nombre;
        this.foto = foto;
        this.cr = cr;
    }

    public void setGuardadorFotoListener(GuardarFotoListener listener){
        this.listener = listener;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        //Escribe los bytes en el fichero de salida
        Log.i(LOG_TAG, "background");
        String ruta_foto = Util.obtenerDirectorioFotos() + File.separator + nombre +
                Util.EXTENSION_ARCHIVO_FOTO;
        Log.i(LOG_TAG, "se obtuvo directorio");
        FileOutputStream salida = null;
        try {
            salida = new FileOutputStream(ruta_foto);
            foto.compress(Bitmap.CompressFormat.JPEG, 85, salida);
            Log.i(LOG_TAG, "se guardo foto");
        } catch (IOException e) {
            Log.e(LOG_TAG, "No se pudo guardar la foto: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            //Cierra la imagen y el escritor
            foto = null;
            if (null != salida) {
                try {
                    salida.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //Actualizamos el Content Provider de la galeria del dispositivo
        if (!ManejadorCPImagenes.insertarImagen(ruta_foto, TIPO_MIME, cr)){
            Log.w(LOG_TAG, "No se pudo insertar la imagen en el ContentProvider de la galeria");
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (listener != null) {
            if (result) {
                listener.fotoGuardada(nombre);
            } else {
                listener.fotoNoGuardada();
            }
        }
    }

    /**
     * Interfaz GuardarFotoListener: Por este medio se avisa cuando una foto fue guardada
     * exitosamente o no.
     */
    public interface GuardarFotoListener {
        void fotoGuardada(String ruta_foto);
        void fotoNoGuardada();
    }

}