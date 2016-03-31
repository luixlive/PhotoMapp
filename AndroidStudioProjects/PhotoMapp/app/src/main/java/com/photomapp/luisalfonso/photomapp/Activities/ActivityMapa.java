package com.photomapp.luisalfonso.photomapp.Activities;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.photomapp.luisalfonso.photomapp.Auxiliares.AdaptadorListaFotos;
import com.photomapp.luisalfonso.photomapp.Auxiliares.Animacion;
import com.photomapp.luisalfonso.photomapp.Auxiliares.ImagenPhotoMapp;
import com.photomapp.luisalfonso.photomapp.Auxiliares.ManejadorPermisos;
import com.photomapp.luisalfonso.photomapp.Auxiliares.ManejadorUbicacion;
import com.photomapp.luisalfonso.photomapp.Fragments.DialogoNombreFoto;
import com.photomapp.luisalfonso.photomapp.Auxiliares.ManejadorCPImagenes;
import com.photomapp.luisalfonso.photomapp.R;
import com.photomapp.luisalfonso.photomapp.Auxiliares.Util;
import com.photomapp.luisalfonso.photomapp.data.ContratoPhotoMapp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase ActivityMapa: muestra un mapa y las fotos que el usuraio ha tomado.
 */
public class ActivityMapa extends AppCompatActivity implements OnMapReadyCallback,
        DialogoNombreFoto.NombreSeleccionadoListener {

    private static final String LOG_TAG = "ACTIVITY MAPA";

    //Macros
    private static final int PERMISO_INICIAR_LISTA = 0;
    private static final float ZOOM_MAPA = 18.0f;
    private static final int TIEMPO_ANIMACION_MAPA = 1000 * 3;
    private static final int RELACION_IMAGEN_ZOOM_PANTALLA = 2;

    //Mapa
    private GoogleMap mapa;

    //Lista con los ids de l as fotos
    private ArrayList<Integer> ids_fotos;

    //Variable que indica cual fue la ultima imagen pulsada por el usuario
    private ImagenPhotoMapp imagen_seleccionada = null;

    //Adaptador para el RecyclerView
    private AdaptadorListaFotos adaptador;

    //Modos contextuales
    private ActionMode modo_contextual_eliminar, modo_contextual_foto_seleccionada;

    //Vistas de las leyendas del mapa para cada imagen
    private TextView fecha_imagen;
    private TextView ciudad_imagen;
    private TextView direccion_imagen;

    //Objeto Animacion para apoyar con las animaciones
    private Animacion animador;

    //Listener para las vistas del adaptador
    AdaptadorListaFotos.EventosAdaptadorListener listener_click_fotos =
            new AdaptadorListaFotos.EventosAdaptadorListener() {

                @Override
                public void itemClick(View vista, int posicion) {
                    //Si hay un zoom simplemente lo quitamos, si esta en seleccion no hacemos nada
                    if (adaptador.obtenerEstadoSeleccion()) {
                        return;
                    }
                    animador.quitarZoomActual();

                    //Se obtiene la imagen seleccionada
                    imagen_seleccionada =
                            Util.obtenerImagenPorId(ids_fotos.get(posicion), getContentResolver());

                    //Se actualizan las leyendas
                    actualizarUbicacion(imagen_seleccionada.getLatLng());
                    actualizarLeyendasMapa(
                            imagen_seleccionada.obtenerDireccion(getApplicationContext()),
                            imagen_seleccionada.getFecha());
                }

                @Override
                public void inicioSeleccion() {
                    animador.quitarZoomActual();
                    mostrarMenuEliminar();
                }

                @Override
                public void actualizacionSeleccion() {}

                @Override
                public void finSeleccion() {
                    modo_contextual_eliminar.finish();
                }

                @Override
                public void itemSwipeArriba(View vista, int posicion) {
                    if (adaptador.obtenerEstadoSeleccion()) {
                        return;
                    }
                    animador.quitarZoomActual();

                    //Obtenemos la imagen
                    imagen_seleccionada = Util.obtenerImagenPorId(ids_fotos.get(posicion),
                            getContentResolver());

                    //Al ampliar un zoom hacemos las leyendas invisibles
                    hacerLeyendasVisibles(false);

                    //Hacemos el zoom
                    animador.hacerZoomImagenLista(
                            Util.obtenerRutaArchivoImagen(imagen_seleccionada.getNombre()),
                            vista.findViewById(R.id.foto_lista),
                            findViewById(R.id.imagen_ampliada),
                            findViewById(R.id.contenedor).getHeight() /
                                    RELACION_IMAGEN_ZOOM_PANTALLA);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        iniciarMapa();
        //Tenemos que checar primero si el usuario dio acceso a su almacenamiento externo
        String[] permisos_necesarios = { ManejadorPermisos.PERMISO_ACCESO_ALMACENAMIENTO_EXTERNO };
        String mensaje_dialogo = getString(R.string.mensaje_dialogo_permiso_mostrar_lista);
        if (ManejadorPermisos.checarPermisos(permisos_necesarios, this, 0, mensaje_dialogo)) {
            crearAnimador();
            obtenerVistasLeyendas();
            iniciarListaSiEsPosible();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Obtenemos el mapa
        mapa = googleMap;
        if (ManejadorPermisos.checarPermisoUbicacion(this)) {
            situarMapaPosicionActual();
        }
    }

    @Override
    public void nombreSeleccionado(String nombre_nuevo) {
        if (Util.obtenerEscrituraPosible()) {
            //Se actualiza el nombre en el archivo
            String ruta_foto_vieja  =
                    Util.obtenerRutaArchivoImagen(imagen_seleccionada.getNombre());
            File archivo_foto = new File(ruta_foto_vieja);
            if (archivo_foto.exists()) {
                if (ruta_foto_vieja != null) {
                    if (!archivo_foto.renameTo(
                            new File(Util.obtenerRutaArchivoImagen(nombre_nuevo)))) {
                        return;
                    }
                }
            }

            //Se actualiza el nombre en la base de datos
            String clause = ContratoPhotoMapp.Fotos._ID + " = ?";
            String[] args = {String.valueOf(imagen_seleccionada.getId_bd())};
            ContentValues nuevos_valores = new ContentValues();
            nuevos_valores.put(ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE, nombre_nuevo);
            int registros_actualizados = getContentResolver().update(
                    ContratoPhotoMapp.Fotos.CONTENT_URI,
                    nuevos_valores,
                    clause,
                    args
            );
            if (registros_actualizados == 1) {
                Toast.makeText(getApplicationContext(), getString(R.string.actualizado),
                        Toast.LENGTH_SHORT).show();
                Log.i(LOG_TAG, "Se cambió el nombre de la imagen exitosamente");
            } else if (registros_actualizados == 0) {
                Log.e(LOG_TAG, "No se cambió el nombre de la imagen exitosamente");
            } else {
                Log.wtf(LOG_TAG, "Se actualizaron varios registros ¿?");
            }

            //Se actualiza el nombre en el ContentProvider de la galeria
            ManejadorCPImagenes.actualizarImagen(
                    ruta_foto_vieja,
                    Util.obtenerRutaArchivoImagen(nombre_nuevo),
                    getContentResolver()
            );
        }
    }

    @Override
    public void nombreCancelado() {
        //Si el usuario cancela el dialogo para cambiar el nombre, no hay necesidad de hacer nada
    }

    @Override
    public void onBackPressed() {
        //Si existe un zoom, al presionar back se quita
        if (animador == null || !animador.quitarZoomActual()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {

            case PERMISO_INICIAR_LISTA:
                if (ManejadorPermisos.checarPermisoAlmacenamiento(this)) {
                    crearAnimador();
                    obtenerVistasLeyendas();
                    iniciarListaSiEsPosible();
                }
                break;

        }
    }

    /**
     * crearAnimador: Obtiene un objeto de tipo animacion y crea los callbacks necesarios para los
     * eventos de animacion.
     */
    private void crearAnimador() {
        animador = new Animacion(getResources().getInteger(android.R.integer.config_shortAnimTime));
        animador.setAnimacionListener(new Animacion.AnimacionListener() {
            @Override
            public void zoomAmpliado(View v) {
                //Al hacer zoom se abre el menu contextual foto seleccionada
                mostrarMenuImagenSeleccionada();
            }

            @Override
            public void zoomReducido(View v) {
                //Volvemos a hacer visibles las leyendas y terminamos el menu
                hacerLeyendasVisibles(true);
                if (modo_contextual_foto_seleccionada != null) {
                    modo_contextual_foto_seleccionada.finish();
                }
            }

            @Override
            public void vistaAparecida(View v) {
            }

            @Override
            public void vistaDesaparecida(View v) {
            }

        });
    }

    /**
     * obtenerVistasLeyendas: Procedimiento para recuperar las vistas de las 3 leyendas del mapa
     */
    private void obtenerVistasLeyendas() {
        fecha_imagen = (TextView) findViewById(R.id.fecha_imagen);
        ciudad_imagen = (TextView) findViewById(R.id.ciudad_imagen);
        direccion_imagen = (TextView) findViewById(R.id.direccion_imagen);
    }

    /**
     * iniciarListaSiEsPosible: Si es posible, inicia la lista con las fotos
     */
    private void iniciarListaSiEsPosible() {
        //Checamos que sea posible utilizar su almacenamiento externo
        if (Util.obtenerLecturaPosible()) {
            mostrarListaFotos();
        }
    }

    /**
     * iniciarMapa: obtiene el fragment del mapa e inicia el listener para saber cuando este listo
     */
    private void iniciarMapa() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * mostrarListaFotos: captura el RecyclerView, lo configura y le pone un AdaptadorListaFotos
     * para mostrar las fotos.
     */
    private void mostrarListaFotos() {
        //Utilizamo un objeto RecyclerView porque se le puede configurar un Layout horizontal para
        // que muestre la lista de forma horizontal.
        LinearLayoutManager manejador_layout_horizontal =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        RecyclerView lista_fotos = (RecyclerView) findViewById(R.id.lista_fotos);
        lista_fotos.setVisibility(View.VISIBLE);
        lista_fotos.setLayoutManager(manejador_layout_horizontal);
        lista_fotos.setHasFixedSize(true);

        //Obtenemos los nombres de las fotos y sus IDs de la base de datos
        ids_fotos = Util.obtenerIdsImagenes(getContentResolver());
        ArrayList<String> nombres_imagenes = new ArrayList<>();
        ArrayList<Integer> imagenes_no_encontradas = new ArrayList<>();
        for (Integer id: ids_fotos){
            String nombre_foto = Util.obtenerNombrePorId(id, getContentResolver());
            File archivo_foto = new File(Util.obtenerRutaArchivoImagen(nombre_foto));
            if (archivo_foto.exists()) {
                nombres_imagenes.add(nombre_foto);
            } else {
                imagenes_no_encontradas.add(id);
            }
        }

        //Borramos los registros del Content Provider de las imagenes no ecnontradas
        for (Integer id_borrar: imagenes_no_encontradas) {
            String clause_eliminar = ContratoPhotoMapp.Fotos._ID + " = ?";
            String[] args_eliminar = { id_borrar.toString() };
            int registros_eliminados = getContentResolver().delete(
                    ContratoPhotoMapp.Fotos.CONTENT_URI,
                    clause_eliminar,
                    args_eliminar
            );
            if (registros_eliminados != 1) {
                Log.w(LOG_TAG, "No se elimino la foto inexistente con ID: " + id_borrar);
            }
        }

        //Le pasamos al adaptador los nombres para que busque las imagenes y las ponga en la lista
        adaptador = new AdaptadorListaFotos(this, nombres_imagenes);
        adaptador.setEventosAdaptadorListener(listener_click_fotos);
        lista_fotos.setAdapter(adaptador);
    }

    /**
     * situarMapaPosicionActual
     */
    private void situarMapaPosicionActual(){
        //Llevamos el mapa a la ultima ubicacion conocida del usuario hay permiso de usar ubicacion
        if (mapa != null && ActivityCompat.
                checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            Location ubicacion;
            //Obtenemos la preferencia del usuario para usar GPS
            SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(this);
            if (preferencias.getBoolean(ActivityPreferencias.PREFERENCIA_UTILIZAR_GPS_KEY, false)) {
                ubicacion = ManejadorUbicacion.obtenerUltimaUbicacionConocida(this,
                        LocationManager.GPS_PROVIDER);
            } else {
                ubicacion = ManejadorUbicacion.obtenerUltimaUbicacionConocida(this,
                        LocationManager.NETWORK_PROVIDER);
            }
            if (ubicacion != null) {
                actualizarUbicacion(new LatLng(ubicacion.getLatitude(), ubicacion.getLongitude()));
            }
        }
    }

    /**
     * actualizarUbicacion: muestra la ubicacion referida en el mapa.
     * @param ubicacion: ubicacion a la que se desea llevar en el mapa.
     */
    private void actualizarUbicacion(LatLng ubicacion) {
        if (ubicacion != null)
            mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, ZOOM_MAPA),
                    TIEMPO_ANIMACION_MAPA, null);
    }

    /**
     * actualizarLeyendasMapa: Pone los datos de direccion y fecha especificados en las leyendas
     * sobre el mapa.
     * @param direccion Address con la direccion nueva
     * @param fecha String con la fecha nueva
     */
    private void actualizarLeyendasMapa(Address direccion, String fecha) {
        if (fecha_imagen.getVisibility() == View.INVISIBLE ||
                fecha_imagen.getVisibility() == View.GONE ||
                ciudad_imagen.getVisibility() == View.INVISIBLE ||
                ciudad_imagen.getVisibility() == View.GONE ||
                direccion_imagen.getVisibility() == View.INVISIBLE ||
                direccion_imagen.getVisibility() == View.GONE) {
            hacerLeyendasVisibles(true);
        }
        fecha_imagen.setText(fecha);
        String ciudad_pais = direccion.getLocality() + ", " + direccion.getCountryName();
        ciudad_imagen.setText(ciudad_pais);
        direccion_imagen.setText(direccion.getAddressLine(0));
    }

    /**
     * hacerLeyendasInvisibles: Hace las leyendas visibles o invisibles
     * @param visibles boolean indicando si se hacen visibles o invisibles
     */
    private void hacerLeyendasVisibles(boolean visibles) {
        ImageButton boton_direcciones = (ImageButton) findViewById(R.id.ic_direcciones);
        if (visibles) {
            animador.disolverAparecer(fecha_imagen);
            animador.disolverAparecer(ciudad_imagen);
            animador.disolverAparecer(direccion_imagen);
            animador.disolverAparecer(boton_direcciones);
        } else {
            animador.disolverDesaparecer(fecha_imagen);
            animador.disolverDesaparecer(ciudad_imagen);
            animador.disolverDesaparecer(direccion_imagen);
            animador.disolverDesaparecer(boton_direcciones);
        }
    }

    private void mostrarMenuEliminar() {
        modo_contextual_eliminar = startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                //Inicio del modo contextual
                getMenuInflater().inflate(R.menu.menu_contextual_eliminar_activity_mapa, menu);
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
                        //Si se pulso borrar, se eliminan las imagenes de la base de datos
                        // y de la lista
                        String clause_remove = ContratoPhotoMapp.Fotos._ID + " = ?";
                        String[] args_remove = new String[1];
                        List<Integer> posiciones_items_seleccionados =
                                adaptador.obtenerItemsSelecionados();
                        String nombres[] =
                                new String[posiciones_items_seleccionados.size()];

                        for (int i = posiciones_items_seleccionados.size() - 1;
                             i >= 0; i--) {
                            nombres[i] = Util.obtenerImagenPorId(ids_fotos.get(
                                            posiciones_items_seleccionados.get(i)),
                                    getContentResolver()).getNombre();
                            adaptador.eliminarImagenLista(
                                    posiciones_items_seleccionados.get(i));
                            args_remove[0] = String.valueOf(
                                    ids_fotos.get(posiciones_items_seleccionados.get(i)));
                            getContentResolver().delete(
                                    ContratoPhotoMapp.Fotos.CONTENT_URI,
                                    clause_remove,
                                    args_remove
                            );

                            //Borramos la foto del ContentProvider de la galeria de imagenes
                            if (!ManejadorCPImagenes.eliminarImagen(
                                    Util.obtenerRutaArchivoImagen(nombres[i]),
                                    getContentResolver())) {
                                Log.w(LOG_TAG, "No se pudo eliminar la imagen del Content " +
                                        "Provider de la galeria de imagenes");
                            }
                            ids_fotos.remove((int) posiciones_items_seleccionados.get(i));
                        }

                        Util.eliminarImagenesAlmacenamiento(nombres);
                        imagen_seleccionada = null;

                        //Quitamos las leyendas del layout, pues ya no estan actualizadas
                        hacerLeyendasVisibles(false);
                        mode.finish();
                        return true;

                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                adaptador.terminarSeleccion();
                modo_contextual_eliminar = null;
            }

        });
    }

    private void mostrarMenuImagenSeleccionada(){
        modo_contextual_foto_seleccionada = startActionMode(new ActionMode.Callback() {

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                //Inicio del modo contextual
                getMenuInflater().inflate(R.menu.menu_contextual_editar_activity_mapa,
                        menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch ((item.getItemId())) {

                    case R.id.editar:
                        //Si pulsa editar mostramos el dialogo para editar el nombre
                        DialogoNombreFoto dialogo =
                                DialogoNombreFoto.nuevoDialogo(imagen_seleccionada.getNombre());
                        dialogo.show(getFragmentManager(),
                                DialogoNombreFoto.class.getName());
                        return true;

                    case R.id.galeria:
                        //Obtenemos el id que le coloco el ContentProvider de la galeria
                        //a la imagen
                        String id = ManejadorCPImagenes.obtenerIdImagen(
                                Util.obtenerRutaArchivoImagen(imagen_seleccionada.getNombre()),
                                getContentResolver());

                        //Si el id es valido, lo usamos para construir el uri que nos
                        //permitira iniciar un intent para mostrar la foto en la galeria
                        if (id != null) {
                            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.
                                    buildUpon().appendPath(id).build();
                            startActivity(new Intent(Intent.ACTION_VIEW, uri));
                        } else {
                            Log.e(LOG_TAG, "No se obtuvo un id valido del Content " +
                                    "Provider de la galeria");
                        }
                        return true;

                    case R.id.mapas:
                        mostrarDirecciones(findViewById(R.id.mapas));
                        return true;

                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                animador.quitarZoomActual();
                modo_contextual_foto_seleccionada = null;
            }

        });
    }

    /**
     * mostrarDirecciones: Crea un intent para mostrar la direccion actual en otro mapa.
     * @param view boton mostrar direcciones
     */
    public void mostrarDirecciones(View view){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("geo:" + imagen_seleccionada.getLatitud() + "," +
                imagen_seleccionada.getLongitud() + "?q=" + imagen_seleccionada.getLatitud() + "," +
                imagen_seleccionada.getLongitud() + "()"));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

}