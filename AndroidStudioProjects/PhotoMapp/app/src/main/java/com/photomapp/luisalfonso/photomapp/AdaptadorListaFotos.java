package com.photomapp.luisalfonso.photomapp;

import android.app.Activity;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.photomapp.luisalfonso.photomapp.Activities.ActivityPrincipal;

import java.io.File;

/**
 * Clase AdaptadorListaFotos: Adaptador que llena una RecyclerView para mostrar las fotos debajo del GoogleMap en la
 * ActivityMapa.
 */
public class AdaptadorListaFotos extends RecyclerView.Adapter<AdaptadorListaFotos.ContenedorFotos>  {

    //Fotos
    private String rutas_fotos[];
    private OnItemClickListener listener;
    private LectorBitmaps lector_imagenes;
    private Activity activity_padre;

    public AdaptadorListaFotos(Activity activity, String nombres_fotos[]){
        this.rutas_fotos = new String[nombres_fotos.length];
        activity_padre = activity;

        for (int i = 0; i < nombres_fotos.length; i++){
            rutas_fotos[i] = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator +
                    ActivityPrincipal.NOMBRE_ALBUM_FOTOS + File.separator + nombres_fotos[i] + ActivityPrincipal.EXTENSION_ARCHIVO_FOTO;
        }

        //Iniciamos el lector de las imagenes y le pasamos la imagen "cargando"
        lector_imagenes = new LectorBitmaps(activity);
    }

    @Override
    public AdaptadorListaFotos.ContenedorFotos onCreateViewHolder(ViewGroup parent, int viewType) {
        //Cada que se crea una vista nueva en el RecyclerView se infla el layout elemento_lista_fotos en como holder
        View layout_fotos = LayoutInflater.from(parent.getContext()).inflate(R.layout.elemento_lista_fotos, parent, false);
        return new ContenedorFotos(layout_fotos);
    }

    @Override
    public void onBindViewHolder(ContenedorFotos holder, int position) {
        //Limitamos que cada contenedor de las fotos solo pueda ser tan ancho como es de alto
        ImageView contenedor_imagen = holder.obtenerContenedorImagen();
        contenedor_imagen.setAdjustViewBounds(true);
        contenedor_imagen.setMaxWidth(lector_imagenes.obtenerLongitudImagenLista());
        //Cuando un holder esta listo, se pobla el layout de la imagen con su foto correspondiente
        lector_imagenes.extraerImagenEn(activity_padre, contenedor_imagen, rutas_fotos[position], LectorBitmaps.IMAGEN_LISTA);
    }

    @Override
    public int getItemCount() {
        return rutas_fotos.length;
    }

    /**
     * Interface OnItemClickListener: Por default, el RecyclerView no cuenta con OnItemClickListener, por lo que nosotros lo
     * creamos a traves del adaptador.
     */
    public interface OnItemClickListener {
        void onItemClick(View view , int position);
    }

    /**
     * setOnItemClickListener: Inicializa el OnItemClickListener
     * @param listener onbjeto OnItemClickListener que utilizaremos
     */
    public void setOnItemClickListener(final OnItemClickListener listener){
        this.listener = listener;
    }

    /**
     * Clase ContenedorFotos: Clase auxiliar que utilizamos como ViewHolder pero indicamos los recursos que utilizaaremos
     * (un ImageView para la foto).
     */
    public class ContenedorFotos extends RecyclerView.ViewHolder implements View.OnClickListener {

        //Contenedor para la foto
        private ImageView contenedor_imagen;

        public ContenedorFotos(View layout_foto) {
            super(layout_foto);
            contenedor_imagen = (ImageView)layout_foto.findViewById(R.id.foto);
            layout_foto.setOnClickListener(this);
        }

        public ImageView obtenerContenedorImagen(){
            return contenedor_imagen;
        }

        @Override
        public void onClick(View v) {
            //Avisamos al listener que se presiono un item
            if (listener != null){
                listener.onItemClick(v, getAdapterPosition());
            }
        }
    }

}
