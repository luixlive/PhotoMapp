package com.photomapp.luisalfonso.photomapp.Auxiliares;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

import java.nio.ByteBuffer;

/**
 * Clase ManejadorFotoTomada: Maneja las fotografias tomadas por la camara para obtener los
 * bitmaps y mostrar los resultados en pantalla
 */
public class ManejadorFotoTomada extends AsyncTask<Void, Void, Bitmap> {

    //Contenedores de la foto resultado y de la textura donde se muestra la camara
    private View contenedor_foto, contenedor_camara;

    //Imagen obtenido por le Hardware de la camara
    private Image imagen;

    //Tamano de altura del contenedor de la foto tomada
    private int tamano_foto_tomada;

    //Animador para mostrar la foto resultado
    private Animacion animador;

    //Listener que escucha los eventos producidos por el manejo de la foto
    private ManejadorFotoTomadaListener listener;

    public ManejadorFotoTomada(Image imagen, View contenedor_camara, View contenedor_foto,
                               int longitud_animacion) {
        this.contenedor_foto = contenedor_foto;
        this.contenedor_camara = contenedor_camara;
        this.imagen = imagen;
        tamano_foto_tomada = ((View)contenedor_foto.getParent()).getHeight();
        animador = new Animacion(longitud_animacion);
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        //Obtenemos un bitmap a partir del objeto Image en segundo plano
        ByteBuffer buffer = imagen.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        imagen.close();
        return LectorBitmaps.extraerBitmapEscaladoBytes(bytes, tamano_foto_tomada);
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        //Avisamos al listener que se obtuvo el bitmap y se le pasa
        if (listener != null){
            listener.fotoObtenida(result);
        }

        //Se muestra la foto tomada (tecnica center crop) con una animacion y se llama el evento
        ((ImageView)contenedor_foto).setImageBitmap(result);
        animador.disolverAparecer(contenedor_foto);
        animador.disolverDesaparecer(contenedor_camara);
        if (listener != null){
            listener.fotoMostrada();
        }
    }

    /**
     * quitarFotoMostrada: Quita la foto mostrada y vuelve a colocar el texture para mostrar
     * las imagenes de la camara de forma animcada
     */
    public void quitarFotoMostrada(){
        animador.disolverAparecer(contenedor_camara);
        animador.disolverDesaparecer(contenedor_foto);
        if (listener != null){
            listener.fotoQuitada();
        }
    }

    /**
     * setManejadorFotoTomadaListener: Se subscribe un listener para recibir los eventos
     * @param listener ManejadorFotoTomadaListener
     */
    public void setManejadorFotoTomadaListener(ManejadorFotoTomadaListener listener){
        this.listener = listener;
    }

    /**
     * Interfaz ManejadorFotoTomadaListener: Se utiliza para crear listeners que reciban los
     * eventos.
     */
    public interface ManejadorFotoTomadaListener {
        void fotoObtenida(Bitmap foto);
        void fotoMostrada();
        void fotoQuitada();
    }

}