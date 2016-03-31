package com.photomapp.luisalfonso.photomapp.Auxiliares;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.photomapp.luisalfonso.photomapp.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase AdaptadorListaFotos: Adaptador que llena una RecyclerView para mostrar las fotos debajo del
 * GoogleMap en lamActivityMapa.
 */
public class AdaptadorListaFotos extends
        RecyclerView.Adapter<AdaptadorListaFotos.ContenedorFotos>  {

    //Rutas de las fotos
    public ArrayList<String> nombres_fotos;

    //Listener para informar cuando se pulsan las imagenes
    private EventosAdaptadorListener listener;

    //Manejador de bitmaps
    private LectorBitmaps lector_imagenes;

    //Activity padre
    private Activity activity;

    //Arreglo de booleanos donde iremos almacenando los items seleccionados
    private SparseBooleanArray items_seleccionados;

    private boolean estado_seleccion = false;

    public AdaptadorListaFotos(Activity activity, ArrayList<String> nombres_fotos){
        this.activity = activity;
        items_seleccionados = new SparseBooleanArray();
        this.nombres_fotos = nombres_fotos;
        lector_imagenes = new LectorBitmaps(activity);
    }

    @Override
    public AdaptadorListaFotos.ContenedorFotos onCreateViewHolder(ViewGroup parent, int viewType) {
        //Por cada vista del RecyclerView se infla su holder
        View layout_fotos = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.elemento_lista_fotos, parent, false);
        return new ContenedorFotos(layout_fotos);
    }

    @Override
    public void onBindViewHolder(ContenedorFotos holder, int position) {
        ImageView contenedor_imagen = holder.obtenerContenedorImagen();

        //Declaramos si esta activado o no (seleccionado) para pintar su background
        ((View)contenedor_imagen.getParent()).setActivated(items_seleccionados.get(position));

        //Cuando un holder esta listo, se pobla el layout de la imagen con su foto correspondiente
        lector_imagenes.extraerImagenListaEn(activity, contenedor_imagen,
                Util.obtenerRutaArchivoImagen(nombres_fotos.get(position)));
    }

    @Override
    public int getItemCount() {
        return nombres_fotos.size();
    }

    public boolean obtenerEstadoSeleccion(){
        return estado_seleccion;
    }

    public void terminarSeleccion(){
        if (estado_seleccion){
            cambiarEstadoSeleccion();
        }
    }

    private void cambiarEstadoSeleccion(){
        estado_seleccion = !estado_seleccion;
        if (estado_seleccion){
            if (listener != null) {
                listener.inicioSeleccion();
            }
        } else{
            borrarSelecciones();
            if (listener != null) {
                listener.finSeleccion();
            }
        }
    }

    private void cambiarEstadoSeleccionItem(int posicion){
        if (obtenerEstadoSeleccion()) {
            if (listener != null) {
                listener.actualizacionSeleccion();
            }
            if (items_seleccionados.get(posicion, false)) {
                items_seleccionados.delete(posicion);
                if (obtenerNumeroItemsSeleccionados() == 0){
                    cambiarEstadoSeleccion();
                }
            } else {
                items_seleccionados.put(posicion, true);
            }
            //Despues de cambiar el estado se notifica al adaptador para que actualice la lista
            notifyItemChanged(posicion);
        }
    }

    /**
     * borrarSelecciones: Limpia todas las selecciones de imagenes.
     */
    private void borrarSelecciones(){
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
    public void eliminarImagenLista(int index){
        //Si la imagen a eliminar esta seleccionada, se elimina la seleccion antes
        if (items_seleccionados.get(index)){
            items_seleccionados.delete(index);
        }
        if (estado_seleccion){
            if (obtenerNumeroItemsSeleccionados() == 0){
                cambiarEstadoSeleccion();
            }
        }
        nombres_fotos.remove(index);
        notifyItemRemoved(index);
    }

    /**
     * Interface EventosAdaptadorListener: Por default, el RecyclerView no cuenta con
     * OnItemClickListener, por lo que nosotros lo creamos a traves del adaptador.
     */
    public interface EventosAdaptadorListener {
        void itemClick(View vista , int posicion);
        void inicioSeleccion();
        void actualizacionSeleccion();
        void finSeleccion();
        void itemSwipeArriba(View vista , int posicion);
    }

    /**
     * setOnItemClickListener: Inicializa el OnItemClickListener
     * @param listener onbjeto ImagenPulsadaListener que utilizaremos
     */
    public void setEventosAdaptadorListener(final EventosAdaptadorListener listener){
        this.listener = listener;
    }

    /**
     * Clase ContenedorFotos: Clase auxiliar que utilizamos como ViewHolder pero indicamos los
     * recursos que utilizaaremos (un ImageView para la foto).
     */
    public class ContenedorFotos extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener{

        //Contenedor para la foto
        private ImageView contenedor_imagen;

        public ContenedorFotos(View layout_foto) {
            super(layout_foto);

            contenedor_imagen = (ImageView)layout_foto.findViewById(R.id.foto_lista);
            layout_foto.setOnClickListener(this);
            layout_foto.setOnLongClickListener(this);
            layout_foto.setOnTouchListener(
                    new SwipeListener(activity.getApplicationContext()) {

                        @Override
                        public void swipeIzquierda(View v) {
                        }

                        @Override
                        public void swipeDerecha(View v) {
                        }

                        @Override
                        public void swipeArriba(View v) {
                            //El unico swipe que nos interesa detectar es hacia arriba
                            if (!estado_seleccion && listener != null) {
                                listener.itemSwipeArriba(v, getAdapterPosition());
                            }
                        }

                        @Override
                        public void swipeAbajo(View v) {
                        }

                    });
        }

        @Override
        public void onClick(View v) {
            if (estado_seleccion){
                cambiarEstadoSeleccionItem(getAdapterPosition());
            } else if (listener != null) {
                listener.itemClick(v, getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (estado_seleccion) {
                cambiarEstadoSeleccionItem(getAdapterPosition());
            } else {
                cambiarEstadoSeleccion();
                cambiarEstadoSeleccionItem(getAdapterPosition());
            }
            return true;
        }

        public ImageView obtenerContenedorImagen(){
            return contenedor_imagen;
        }

    }

}