package com.photomapp.luisalfonso.photomapp;

import android.media.Image;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Clase GuardadorFoto: Guarda fotos en un hilo en segundo plano.
 */
public class GuardadorFoto extends AsyncTask<Image, Integer, Boolean> {

    //Fichero e informacion para el registro
    private final File fichero;
    private final String nombre_foto;

    //Escuchador de eventos
    private GuardarFotoListener listener;

    /**
     * GuardadorImagen: Guarda la imagen.
     * @param fichero File donde se almacenara
     * @param nombre_foto String nombre
     */
    public GuardadorFoto(GuardarFotoListener listener, File fichero, String nombre_foto) {
        this.listener = listener;
        this.fichero = fichero;
        this.nombre_foto = nombre_foto;
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
            return true;
        } catch (IOException e) {
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
            listener.fotoGuardada(nombre_foto);
        } else{
            listener.fotoNoGuardada(nombre_foto);
        }
    }

    /**
     * Interfaz GuardarFotoListener: Por este medio se avisa cuando una foto fue guardada
     * exitosamente o no.
     */
    public interface GuardarFotoListener {
        void fotoGuardada(String nombre_foto);
        void fotoNoGuardada(String nombre_foto);
    }

}