package com.photomapp.luisalfonso.photomapp.Activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.photomapp.luisalfonso.photomapp.AdaptadorListaFotos;
import com.photomapp.luisalfonso.photomapp.Fragments.DialogoNombreFoto;
import com.photomapp.luisalfonso.photomapp.LectorBitmaps;
import com.photomapp.luisalfonso.photomapp.R;
import com.photomapp.luisalfonso.photomapp.Util;
import com.photomapp.luisalfonso.photomapp.data.ContratoPhotoMapp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase ActivityMapa: muestra un mapa y las fotos que el usuraio a tomado. Al pulsar una foto el mapa muestra
 * donde se la tomó. Implementa la interfaz OnMapReadyCallback para obtener eventos del GoogleMap.
 */
public class ActivityMapa extends AppCompatActivity implements OnMapReadyCallback, ActionMode.Callback,
        DialogoNombreFoto.NombreSeleccionadoListener {

    //Etiqueta para la escritura al LOG
    private static final String LOG_TAG = "ACTIVITY MAPA";

    //Macros para solicitar permisos, y del mapa: altura del zoom y tiempo de la animacion de movimiento
    private static final int PERMISO_ACCESO_UBICACION = 1;
    private static final float ZOOM_MAPA = 18.0f;
    private static final int TIEMPO_ANIMACION_MAPA = 1000 * 3;
    private static final int IMAGEN_NO_SELECCIONADA = -1;
    private static final int RELACION_IMAGEN_ZOOM_PANTALLA = 2;

    //Mapa y lista con los ids de las fotos para inicizliar el adaptador del RecyclerView
    private GoogleMap mapa;
    private ArrayList<Integer> ids_fotos;

    //Bandera que nos indica si se puede leer en el almacenamiento externo y si el usuario da acceso a su ubicacion
    private boolean lectura_posible;
    private boolean acceso_ubicacion = true;

    //Variable que indica cual fue la ultima imagen pulsada por el usuario
    private int imagen_seleccionada = IMAGEN_NO_SELECCIONADA;

    //Variables de apoyo para la seleccion de imagenes y el modo de context menu
    private AdaptadorListaFotos adaptador;
    private ActionMode modo_contextual;
    private boolean eliminar_pulsado = false;

    //Referencia del animador, sirve guardarla por si es necesario cancelarlo
    private static Animator animador_actual;
    //Duracion de la animacion en milisegundos
    private static int duracion_animacion = 0;
    //Variables necesarias para hacer y quitar zoom
    private ImageView imagen_ampliada;
    private Rect contornos_iniciales;
    private float escala_final_inicial;
    private View contenedor_pequeno;
    private boolean hay_zoom = false;
    private int tamano_imagen_ampliada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pantalla_alto = getResources().getDisplayMetrics().heightPixels;
        tamano_imagen_ampliada = pantalla_alto/RELACION_IMAGEN_ZOOM_PANTALLA;
        setContentView(R.layout.activity_mapa);

        iniciarListaSiEsPosible();
        iniciarMapa();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        //Inicio del modo contextual
        getMenuInflater().inflate(R.menu.menu_contextual_activity_mapa, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.borrar:
                //Si se pulso borrar, se eliminan las imagenes de la base de datos y de la lista
                String clause_remove = ContratoPhotoMapp.Fotos._ID + " = ?";
                String[] args_remove = new String[1];
                List<Integer> posiciones_items_seleccionados = adaptador.obtenerItemsSelecionados();
                for (int i = posiciones_items_seleccionados.size() - 1; i >= 0; i--) {
                    adaptador.eliminarImagenesLista(posiciones_items_seleccionados.get(i));
                    args_remove[0] = String.valueOf(ids_fotos.get(posiciones_items_seleccionados.get(i)));
                    getContentResolver().delete(
                            ContratoPhotoMapp.Fotos.CONTENT_URI,
                            clause_remove,
                            args_remove
                    );
                    ids_fotos.remove((int) posiciones_items_seleccionados.get(i));
                }
                eliminar_pulsado = true;

                modo_contextual.finish();
                return true;
            case R.id.editar:
                String projection[] = {
                        ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE
                };
                String clause_update = ContratoPhotoMapp.Fotos._ID + " = ?";
                String[] args_update = {String.valueOf(ids_fotos.get((adaptador.obtenerItemsSelecionados().get(0))))};
                Cursor cursor = getContentResolver().query(
                        ContratoPhotoMapp.Fotos.CONTENT_URI,
                        projection,
                        clause_update,
                        args_update,
                        null
                );
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        DialogoNombreFoto dialogo = DialogoNombreFoto.nuevoDialogo(cursor.getString(
                                cursor.getColumnIndex(ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE)));
                        dialogo.show(getFragmentManager(), DialogoNombreFoto.class.getName());
                    }
                    cursor.close();
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        //Cierre del modo contextual
        this.modo_contextual = null;
        //Si se pulso eliminar, no limpiamos las selecciones, pues ya estan limpias
        if (!eliminar_pulsado) {
            adaptador.borrarSelecciones();
        } else{
            eliminar_pulsado = false;
        }
    }

    @Override
    public void nombreSeleccionado(String nombre) {
        if (Util.obtenerEscrituraPosible()) {
            //Se actualiza el nombre en el archivo
            String nombre_viejo = Util.obtenerNombreImagen(ids_fotos.get((adaptador.obtenerItemsSelecionados().get(0))),
                    getContentResolver());
            if (nombre_viejo != null){
                File carpeta_fotos = Util.obtenerDirectorioFotos();
                if (carpeta_fotos != null) {
                    String ruta = carpeta_fotos.getPath();
                    File foto_actualizada = new File(ruta + File.separator + nombre_viejo + Util.EXTENSION_ARCHIVO_FOTO);
                    if (!foto_actualizada.renameTo(new File(ruta + File.separator + nombre + Util.EXTENSION_ARCHIVO_FOTO))){
                        return;
                    }
                }
            }

            //Se actualiza el nombre en la base de datos
            String clause = ContratoPhotoMapp.Fotos._ID + " = ?";
            String[] args = {String.valueOf(ids_fotos.get((adaptador.obtenerItemsSelecionados().get(0))))};
            ContentValues nuevos_valores = new ContentValues();
            nuevos_valores.put(ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE, nombre);
            int registros_actualizados = getContentResolver().update(
                    ContratoPhotoMapp.Fotos.CONTENT_URI,
                    nuevos_valores,
                    clause,
                    args
            );
            if (registros_actualizados == 1) {
                Toast.makeText(getApplicationContext(), getString(R.string.actualizado), Toast.LENGTH_SHORT).show();
                Log.i(LOG_TAG, "Se cambió el nombre de la imagen exitosamente");
            } else if (registros_actualizados == 0) {
                Log.e(LOG_TAG, "No se cambió el nombre de la imagen exitosamente");
            } else {
                Log.wtf(LOG_TAG, "Se actualizaron varios registros ¿?");
            }
        }
    }

    @Override
    public void nombreCancelado() {
        //Si el usuario cancela el dialogo para cambiar el nombre, no hay necesidad de hacer nada
    }

    /**
     * iniciarListaSiEsPosible: Si es posible la lectura al almacenamiento, inicia la lista con las fotos
     */
    private void iniciarListaSiEsPosible() {
        //Recuperamos el intent de la ActivityPrincipa
        Intent intent = getIntent();
        //Si la lectura es posible mostramos la lista con fotos
        if (intent.hasExtra(ActivityPrincipal.EXTRA_LECTURA_POSIBLE)) {
            lectura_posible = intent.getBooleanExtra(ActivityPrincipal.EXTRA_LECTURA_POSIBLE, false);
        }
        if (lectura_posible) {
            mostrarListaFotos();
        }
    }

    /**
     * iniciarMapa: obtiene el fragment del mapa e inicia el listener para saber cuando este listo
     */
    private void iniciarMapa() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * mostrarListaFotos: captura el RecyclerView, lo configura y le pone un AdaptadorListaFotos para mostrar las fotos.
     */
    private void mostrarListaFotos() {
        LinearLayoutManager manejador_layout_horizontal = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        //Utilizamo un objeto RecyclerView porque se le puede configurar un Layout horizontal para que muestre la lista
        //de forma horizontal.
        RecyclerView lista_fotos = (RecyclerView) findViewById(R.id.lista_fotos);
        lista_fotos.setLayoutManager(manejador_layout_horizontal);
        lista_fotos.setHasFixedSize(true);

        //Obtenemos los nombres de las fotos y sus IDs de la base de datos
        ArrayList<String> nombres_imagenes = new ArrayList<>();
        ids_fotos = new ArrayList<>();
        String[] projection = {
                ContratoPhotoMapp.Fotos._ID,
                ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE
        };
        //Hacemos el query
        Cursor cursor = getContentResolver().query(ContratoPhotoMapp.Fotos.CONTENT_URI, projection, null, null, null);
        if (cursor == null) {
            Log.e(LOG_TAG, "Se retorno un cursor nulo del query al ContentProvider");
        } else if (cursor.getCount() < 1) {
            Log.wtf(LOG_TAG, "No se encontro la foto con el ContentProvider");
            cursor.close();
        } else {
            boolean fotos_no_encontradas = false;
            ArrayList<String> ids_fotos_no_encontradas = new ArrayList<>();
            File ruta_fotos = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

            //Iteramos sobre cada elemento del cursor, analizamos si la foto existe en el almacenamiento y de no ser asi
            //capturamos los ids de las fotos no encontradas
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                String nombre_foto = cursor.getString(cursor.getColumnIndex(ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE));
                int id_foto = cursor.getInt(cursor.getColumnIndex(ContratoPhotoMapp.Fotos._ID));

                File archivo_foto = new File(ruta_fotos + File.separator + Util.NOMBRE_ALBUM_FOTOS +
                        File.separator + nombre_foto + Util.EXTENSION_ARCHIVO_FOTO);
                if (archivo_foto.exists()) {
                    nombres_imagenes.add(nombre_foto);
                    ids_fotos.add(id_foto);
                } else{
                    fotos_no_encontradas = true;
                    ids_fotos_no_encontradas.add(String.valueOf(id_foto));
                }

                cursor.moveToNext();
            }
            cursor.close();

            //Eliminamos las fotos no encontradas de la base de datos (en caso de haber)
            if (fotos_no_encontradas){
                int numero_fotos_eliminar = ids_fotos_no_encontradas.size();
                for (int i = 0; i < numero_fotos_eliminar; i++) {
                    String clause_eliminar = ContratoPhotoMapp.Fotos._ID + " = ?";
                    String[] args_eliminar = { ids_fotos_no_encontradas.get(i) };
                    int registros_eliminados = getContentResolver().delete(ContratoPhotoMapp.Fotos.CONTENT_URI,
                            clause_eliminar, args_eliminar);
                    if (registros_eliminados != 1) {
                        Log.w(LOG_TAG, "No se elimino la foto inexistente con ID: " + ids_fotos_no_encontradas.get(i));
                    }
                }
                Log.i(LOG_TAG, "Se eliminaron fotos inexistentes de la base de datos");
            }
        }

        String arreglo_nombres_imagenes[] = new String[nombres_imagenes.size()];
        for (int i = 0; i < nombres_imagenes.size(); i++){
            arreglo_nombres_imagenes[i] = nombres_imagenes.get(i);
        }
        //Le pasamos al adaptador los nombres para que vaya y busque las imagenes y las ponga en la lista de fotos
        adaptador = new AdaptadorListaFotos(this, arreglo_nombres_imagenes);
        lista_fotos.setAdapter(adaptador);
        //Si se da click en una imagen, se actualiza la ubicacion del mapa de acuerdo a los datos de la base de datos
        adaptador.setOnItemClickListener(new AdaptadorListaFotos.EventosAdaptadorListener() {
            @Override
            public void itemClick(View view, int position) {
                if (modo_contextual == null) {
                    //Si no esta en modo contextual, al hacer click en una imagen se muestra en el mapa donde se tomo, o
                    //si es la segunda vez que se pulsa, se muestra en un dialogo con su informacion
                    String[] projection = {
                            ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE,
                            ContratoPhotoMapp.Fotos.COLUMNA_FECHA,
                            ContratoPhotoMapp.Fotos.COLUMNA_LATITUD,
                            ContratoPhotoMapp.Fotos.COLUMNA_LONGITUD
                    };
                    String clause = ContratoPhotoMapp.Fotos._ID + " = ?";

                    //Se obtiene el id de la foto de esa posicion y se hace el query para obtener latitud y longitud
                    String[] args = {String.valueOf(ids_fotos.get(position))};
                    Cursor cursor = getContentResolver().query(ContratoPhotoMapp.Fotos.CONTENT_URI, projection, clause, args, null);
                    if (cursor == null) {
                        Log.e(LOG_TAG, "Se retorno un cursor nulo del query al ContentProvider");
                    } else if (cursor.getCount() < 1) {
                        cursor.close();
                        Log.wtf(LOG_TAG, "No se encontro la foto con el ContentProvider");
                    } else if (imagen_seleccionada == position) {
                        //Si es la segunda vez que se pulsa la imagen, se muestra el dialogo de la imagen, fecha y ubicacion
                        imagen_seleccionada = IMAGEN_NO_SELECCIONADA;
                        hacerZoomImagenLista(position, view.findViewById(R.id.foto));
                        cursor.close();
                    } else {
                        //Si es la primera vez que se pulsa la imagen, actualiza la posicion del mapa con la ubicacion obtenida
                        imagen_seleccionada = position;
                        cursor.moveToFirst();
                        LatLng ubicacion_foto = new LatLng(cursor.getDouble(cursor.getColumnIndex(
                                ContratoPhotoMapp.Fotos.COLUMNA_LATITUD)),
                                cursor.getDouble(cursor.getColumnIndex(ContratoPhotoMapp.Fotos.COLUMNA_LONGITUD)));
                        actualizarUbicacion(ubicacion_foto);
                        cursor.close();
                        cursor.close();
                    }
                } else {
                    //Si esta en modo contextual, el click solo cambia el estado de seleccion de la imagen
                    ActivityMapa.this.cambiarEstadoSeleccion(position);
                }
            }

            @Override
            public void itemLongClick(View view, int position){
                //Si el modo contextual no se ha iniciado, se comienza
                if (modo_contextual != null) {
                    cambiarEstadoSeleccion(position);
                    return;
                }
                modo_contextual = startActionMode(ActivityMapa.this);
                cambiarEstadoSeleccion(position);
            }
        });
    }

    /**
     * cambiarEstadoSeleccion: cambia el estado de seleccion de la imagen de la posicion establecida, y si el numero
     * de imagenes seleccionadas es cero, cierra el modo contextual. Si el numero de imagenes es exactamente uno,
     * muestra el boton editar en el menu contextual para editar el nombre de la imagen.
     * @param posicion int del index de la imagen
     */
    private void cambiarEstadoSeleccion(int posicion) {
        adaptador.cambiarEstadoSeleccion(posicion);
        int numero_selecciones = adaptador.obtenerNumeroItemsSeleccionados();
        if (numero_selecciones == 0) {
            modo_contextual.finish();
        } else if( numero_selecciones == 1){
            modo_contextual.getMenu().findItem(R.id.editar).setVisible(true);
        } else {
            modo_contextual.getMenu().findItem(R.id.editar).setVisible(false);
        }
    }

    /**
     * actualizarUbicacion: muestra la ubicacion referida en el mapa.
     * @param ubicacion: ubicacion a la que se desea llevar en el mapa.
     */
    private void actualizarUbicacion(LatLng ubicacion) {
        if (ubicacion != null)
            mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, ZOOM_MAPA), TIEMPO_ANIMACION_MAPA, null);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Obtenemos el mapa
        mapa = googleMap;

        //Verificamos que el usuario nos de permiso de acceder a su ubicacion y llevamos el mapa a su ultima ubicacion conocida
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISO_ACCESO_UBICACION);
        }
        //Llevamos el mapa a la ultima ubicacion conocida del usuario
        if (acceso_ubicacion) {
            LocationManager administrador_ubicacion = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            Location ubicacion = administrador_ubicacion.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            actualizarUbicacion(new LatLng(ubicacion.getLatitude(), ubicacion.getLongitude()));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISO_ACCESO_UBICACION: {
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    acceso_ubicacion = false;
                    Toast.makeText(this, getString(R.string.permiso_ubicacion_denegado), Toast.LENGTH_LONG).show();
                } else{
                    acceso_ubicacion = true;
                }
                break;
            }
        }
    }

    /**
     * hacerZoomImagenLista: Hace un zoom animado a una imagen de la lista.
     * @param posicion_imagen_lista int posicion en la lista de la imagen
     */
    private void hacerZoomImagenLista(int posicion_imagen_lista, final View contenedor_pequeno) {
        if(hay_zoom){
            quitarZoomActual();
            return;
        }
        hay_zoom = true;
        //Si hay una animacion corriendo, se cancela
        if (animador_actual != null) {
            animador_actual.cancel();
        }
        //Si aun no se define la duracion de la animacion, se inicializa con los valores del SO
        if (duracion_animacion == 0){
            duracion_animacion = getResources().getInteger(android.R.integer.config_shortAnimTime);
        }
        this.contenedor_pequeno = contenedor_pequeno;

        //Cargamos la imagen en alta definicion
        File carpeta_fotos = Util.obtenerDirectorioFotos();
        if (carpeta_fotos == null) {
            Log.w(LOG_TAG, "No se encontro el directorio de fotos");
            return;
        }
        String ruta = carpeta_fotos.getPath() + File.separator +
                Util.obtenerNombreImagen(ids_fotos.get(posicion_imagen_lista), getContentResolver()) + Util.EXTENSION_ARCHIVO_FOTO;
        imagen_ampliada = (ImageView)findViewById(R.id.imagen_ampliada);
        imagen_ampliada.setImageBitmap(LectorBitmaps.extraerBitmapEscaladoAlmacenaiento(ruta, tamano_imagen_ampliada));

        //Se calculan los contornos iniciales y finales de la imagen
        contornos_iniciales = new Rect();
        final Rect contornos_finales = new Rect();
        final Point offset = new Point();

        //El contorno inicial es el rectangulo del contenedor de la imagen pequena,
        //el contorno final es el de la imagen grande y el offset es el origen del contenedor grande
        contenedor_pequeno.getGlobalVisibleRect(contornos_iniciales);
        findViewById(R.id.contenedor).getGlobalVisibleRect(contornos_finales, offset);
        contornos_iniciales.offset(-offset.x, -offset.y);
        contornos_finales.offset(-offset.x, -offset.y);

        //Se ajustan los bordes para que se mantenga la imagen igual (no se deforme)
        float escala_inicial;
        if ((float) contornos_finales.width() / contornos_finales.height()
                > (float) contornos_iniciales.width() / contornos_iniciales.height()) {
            //Se extienden los bordes horizontalmente
            escala_inicial = (float) contornos_iniciales.height() / contornos_finales.height();
            float ancho_inicial = escala_inicial * contornos_finales.width();
            float ancho_diferencia = (ancho_inicial - contornos_iniciales.width()) / 2;
            contornos_iniciales.left -= ancho_diferencia;
            contornos_iniciales.right += ancho_diferencia;
        } else {
            //Se extienden los bordes verticalmente
            escala_inicial = (float) contornos_iniciales.width() / contornos_finales.width();
            float alto_inicial = escala_inicial * contornos_finales.height();
            float alto_diferencia = (alto_inicial - contornos_iniciales.height()) / 2;
            contornos_iniciales.top -= alto_diferencia;
            contornos_iniciales.bottom += alto_diferencia;
        }

        //Escondemos el contenedor pequeno y mostramos el contenedor grande
        contenedor_pequeno.setAlpha(0f);
        imagen_ampliada.setVisibility(View.VISIBLE);

        //Ponemos los pivotes de la imagen ampliada en la esquina superior izquierda
        imagen_ampliada.setPivotX(0f);
        imagen_ampliada.setPivotY(0f);

        //Construimos y corremos la animacion tanto de expansion como de posicion
        AnimatorSet set_animacion = new AnimatorSet();
        set_animacion
                .play(ObjectAnimator.ofFloat(imagen_ampliada, View.X,
                        contornos_iniciales.left, contornos_finales.left))
                .with(ObjectAnimator.ofFloat(imagen_ampliada, View.Y,
                        contornos_iniciales.top, contornos_finales.top))
                .with(ObjectAnimator.ofFloat(imagen_ampliada, View.SCALE_X, escala_inicial, 1f))
                .with(ObjectAnimator.ofFloat(imagen_ampliada, View.SCALE_Y, escala_inicial, 1f));
        set_animacion.setDuration(duracion_animacion);
        set_animacion.setInterpolator(new DecelerateInterpolator());
        set_animacion.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animador_actual = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animador_actual = null;
            }
        });
        set_animacion.start();
        animador_actual = set_animacion;

        // Al hacer click en la imagen, se regresa el zoom
        escala_final_inicial = escala_inicial;
        imagen_ampliada.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                quitarZoomActual();
            }
        });
    }

    /**
     * quitarZoomActual: Hace una animacion para remover el zoom a la imagen de la lista donde se aplico.
     */
    private void quitarZoomActual(){
        if (!hay_zoom){
            return;
        }
        hay_zoom = false;
        if (animador_actual != null) {
            animador_actual.cancel();
        }

        //Creamos la animacion
        AnimatorSet set_animacion = new AnimatorSet();
        set_animacion
                .play(ObjectAnimator.ofFloat(imagen_ampliada, View.X, contornos_iniciales.left))
                .with(ObjectAnimator.ofFloat(imagen_ampliada, View.Y, contornos_iniciales.top))
                .with(ObjectAnimator.ofFloat(imagen_ampliada, View.SCALE_X, escala_final_inicial))
                .with(ObjectAnimator.ofFloat(imagen_ampliada, View.SCALE_Y, escala_final_inicial));
        set_animacion.setDuration(duracion_animacion);
        set_animacion.setInterpolator(new DecelerateInterpolator());
        set_animacion.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                contenedor_pequeno.setAlpha(1f);
                imagen_ampliada.setVisibility(View.GONE);
                animador_actual = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                contenedor_pequeno.setAlpha(1f);
                imagen_ampliada.setVisibility(View.GONE);
                animador_actual = null;
            }
        });
        set_animacion.start();
        animador_actual = set_animacion;
    }

    @Override
    public void onBackPressed()
    {
        //Si existe un zoom, al presionar back se quita
        if (hay_zoom){
            quitarZoomActual();
        } else {
            super.onBackPressed();
        }
    }

}