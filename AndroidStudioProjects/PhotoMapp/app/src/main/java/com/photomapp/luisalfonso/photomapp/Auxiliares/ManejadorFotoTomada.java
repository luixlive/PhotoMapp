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

    private double alto_contenedor, ancho_contenedor;

    //Listener que escucha los eventos producidos por el manejo de la foto
    private ManejadorFotoTomadaListener listener;

    public ManejadorFotoTomada(Image imagen, View contenedor_camara, View contenedor_foto,
                               int longitud_animacion, int alto_contenedor, int ancho_contenedor) {
        this.contenedor_foto = contenedor_foto;
        this.contenedor_camara = contenedor_camara;
        this.imagen = imagen;
        this.alto_contenedor = alto_contenedor;
        this.ancho_contenedor = ancho_contenedor;
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
        Bitmap imagen = LectorBitmaps.extraerBitmapEscaladoBytes(bytes, tamano_foto_tomada);

        //Si el ancho de la imagen es mayor que el del contenedor, se ajusta
        if (imagen.getWidth() > ancho_contenedor) {
            double ancho_imagen_nueva = (imagen.getHeight() * ancho_contenedor) / alto_contenedor;
            imagen = Bitmap.createBitmap(
                    imagen,
                    (int) ((imagen.getWidth() / 2) - (ancho_imagen_nueva / 2)),
                    0,
                    (int) ancho_imagen_nueva,
                    imagen.getHeight()
            );
        } else{
            //Si el ancho es menor que el del contenedor, lo que se ajusta es la altura
            double altura_imagen_nueva = (imagen.getWidth() * alto_contenedor) / ancho_contenedor;
            imagen = Bitmap.createBitmap(
                    imagen,
                    0,
                    (int) ((imagen.getHeight() / 2) - (altura_imagen_nueva / 2)),
                    imagen.getWidth(),
                    (int)altura_imagen_nueva
            );
        }

        return imagen;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        //Avisamos al listener que se obtuvo el bitmap y se le pasa
        if (listener != null){
            listener.fotoObtenida(result);
        }
        //Se muestra la foto tomada con una animcion y se avisa al listener
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