package com.photomapp.luisalfonso.photomapp.Auxiliares;

import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.photomapp.luisalfonso.photomapp.Activities.ActivityMapa;
import com.photomapp.luisalfonso.photomapp.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase AdaptadorListaFotos: Adaptador que llena una RecyclerView para mostrar las fotos debajo del GoogleMap en la
 * ActivityMapa.
 */
public class AdaptadorListaFotos extends RecyclerView.Adapter<AdaptadorListaFotos.ContenedorFotos>  {

    //Rutas de las fotos
    public ArrayList<String> rutas_fotos = new ArrayList<>();

    //Listener para informar cuando se pulsan las imagenes
    private EventosAdaptadorListener listener;

    //Manejador de bitmaps
    private LectorBitmaps lector_imagenes;

    //Activity padre
    private ActivityMapa activity_padre;

    //Arreglo de booleanos donde iremos almacenando los items seleccionados
    private SparseBooleanArray items_seleccionados;

    public AdaptadorListaFotos(ActivityMapa activity, String nombres_fotos[]){
        activity_padre = activity;
        items_seleccionados = new SparseBooleanArray();

        for (String nombres_foto : nombres_fotos) {
            rutas_fotos.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator +
                    Util.NOMBRE_ALBUM_FOTOS + File.separator + nombres_foto +
                    Util.EXTENSION_ARCHIVO_FOTO);
        }

        //Iniciamos el lector de las imagenes
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
        ImageView contenedor_imagen = holder.obtenerContenedorImagen();
        //Declaramos si esta activado o no (seleccionado) para pintar su background
        contenedor_imagen.setActivated(items_seleccionados.get(position));
        //Cuando un holder esta listo, se pobla el layout de la imagen con su foto correspondiente
        lector_imagenes.extraerImagenListaEn(activity_padre, contenedor_imagen, rutas_fotos.get(position));
    }

    @Override
    public int getItemCount() {
        return rutas_fotos.size();
    }

    /**
     * Interface EventosAdaptadorListener: Por default, el RecyclerView no cuenta con OnItemClickListener, por lo que nosotros lo
     * creamos a traves del adaptador.
     */
    public interface EventosAdaptadorListener {
        void itemClick(View view , int position);
        void itemLongClick(View view , int position);
    }

    /**
     * setOnItemClickListener: Inicializa el OnItemClickListener
     * @param listener onbjeto ImagenPulsadaListener que utilizaremos
     */
    public void setOnItemClickListener(final EventosAdaptadorListener listener){
        this.listener = listener;
    }

    /**
     * cambiarEstadoSeleccion: Cambia el estado del item en la posicion establecida entre seleccionado y no seleccionado
     * @param posicion int correspondiente al index del item
     */
    public void cambiarEstadoSeleccion(int posicion){
        if (items_seleccionados.get(posicion, false)) {
            items_seleccionados.delete(posicion);
        }
        else {
            items_seleccionados.put(posicion, true);
        }
        //Despues de cambiar el estado se notifica al adaptador para que actualice la lista
        notifyItemChanged(posicion);
    }

    /**
     * borrarSelecciones: Limpia todas las selecciones de imagenes.
     */
    public void borrarSelecciones(){
        //Se limpia la lista y se notifica al adaptador
        if (items_seleccionados.size() > 0) {
            items_seleccionados.clear();
            notifyDataSetChanged();
        }
    }

    /**
     * obtenerNumeroItemsSeleccionados: Regresa el numero total de items seleccionados.
     * @return int de items seleccionados.
     */
    public int obtenerNumeroItemsSeleccionados() {
        return items_seleccionados.size();
    }

    /**
     * obtenerItemsSelecionados: Regresa una lista con los items seleccionados.
     * @return List de Integers que equivale a la posicion de las imagenes en la lista
     */
    public List<Integer> obtenerItemsSelecionados(){
        List<Integer> items = new ArrayList<>(items_seleccionados.size());
        for (int i = 0; i < items_seleccionados.size(); i++) {
            items.add(items_seleccionados.keyAt(i));
        }
        return items;
    }

    /**
     * eliminarImagenesLista: Se remueven la imagen de la posicion especificada de la lista.
     * @param index int con la posicion de la foto a remover.
     */
    public void eliminarImagenesLista(int index){
        //Si la imagen a eliminar esta seleccionada, se elimina la seleccion antes
        if (items_seleccionados.get(index)){
            items_seleccionados.delete(index);
        }
        rutas_fotos.remove(index);
        notifyItemRemoved(index);
    }

    /**
     * Clase ContenedorFotos: Clase auxiliar que utilizamos como ViewHolder pero indicamos los recursos que utilizaaremos
     * (un ImageView para la foto).
     */
    public class ContenedorFotos extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        //Contenedor para la foto
        private ImageView contenedor_imagen;

        public ContenedorFotos(View layout_foto) {
            super(layout_foto);

            contenedor_imagen = (ImageView)layout_foto.findViewById(R.id.foto);
            layout_foto.setOnClickListener(this);
            layout_foto.setOnLongClickListener(this);
        }

        public ImageView obtenerContenedorImagen(){
            return contenedor_imagen;
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.itemClick(v, getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (listener != null) {
                listener.itemLongClick(v, getAdapterPosition());
            }
            return true;
        }
    }

}