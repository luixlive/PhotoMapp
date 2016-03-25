package com.photomapp.luisalfonso.photomapp.Activities;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.photomapp.luisalfonso.photomapp.AdaptadorListaFotos;
import com.photomapp.luisalfonso.photomapp.Animacion;
import com.photomapp.luisalfonso.photomapp.Fragments.DialogoNombreFoto;
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

    //Bandera que nos indica si el usuario da acceso a su ubicacion
    private boolean acceso_ubicacion = true;

    //Variable que indica cual fue la ultima imagen pulsada por el usuario
    private int imagen_seleccionada = IMAGEN_NO_SELECCIONADA;

    //Variables de apoyo para la seleccion de imagenes y el modo de context menu
    private AdaptadorListaFotos adaptador;
    private ActionMode modo_contextual;
    private boolean eliminar_pulsado = false;

    private LatLng ubicacion_actual;

    //Vistas de las leyendas del mapa para cada imagen
    private TextView fecha_imagen;
    private TextView ciudad_imagen;
    private TextView direccion_imagen;

    //Objeto Animacion para apoyar con las animaciones
    private Animacion animador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);
        animador = new Animacion(getResources().getInteger(android.R.integer.config_shortAnimTime));

        obtenerVistasLeyendas();
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
                //Quitamos las leyendas del layout, pues ya no estan actualizadas
                hacerLeyendasVisibles(false);
                modo_contextual.finish();
                return true;
            case R.id.editar:
                DialogoNombreFoto dialogo = DialogoNombreFoto.nuevoDialogo(Util.obtenerNombreImagen(
                        ids_fotos.get(adaptador.obtenerItemsSelecionados().get(0)), getContentResolver()));
                dialogo.show(getFragmentManager(), DialogoNombreFoto.class.getName());
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
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
    public void onBackPressed()
    {
        //Si existe un zoom, al presionar back se quita
        if (!animador.quitarZoomActual()){
            super.onBackPressed();
        }
    }

    /**
     * iniciarListaSiEsPosible: Si es posible la lectura al almacenamiento, inicia la lista con las fotos
     */
    private void iniciarListaSiEsPosible() {
        if (Util.obtenerLecturaPosible()) {
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
     * obtenerVistasLeyendas: Procedimiento para recuperar las vistas de las 3 leyendas del mapa
     */
    private void obtenerVistasLeyendas(){
        fecha_imagen = (TextView)findViewById(R.id.fecha_imagen);
        ciudad_imagen = (TextView)findViewById(R.id.ciudad_imagen);
        direccion_imagen = (TextView)findViewById(R.id.direccion_imagen);
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
                //Si hay un zoom simplemente lo quitamos
                if (animador.quitarZoomActual()){
                    return;
                }
                if (modo_contextual == null) {
                    //Si no esta en modo contextual, al hacer click en una imagen se muestra en el mapa donde se tomo, o
                    //si es la segunda vez que se pulsa, se muestra en un dialogo con su informacion
                    String[] projection = {
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
                        int pantalla_alto = getResources().getDisplayMetrics().heightPixels;
                        int tamano_imagen_ampliada = pantalla_alto/RELACION_IMAGEN_ZOOM_PANTALLA;

                        String ruta = Util.obtenerDirectorioFotos().getPath() + File.separator +
                                Util.obtenerNombreImagen(ids_fotos.get(position), getContentResolver()) +
                                Util.EXTENSION_ARCHIVO_FOTO;
                        animador.hacerZoomImagenLista(ruta, view.findViewById(R.id.foto),
                                findViewById(R.id.imagen_ampliada), tamano_imagen_ampliada);
                        cursor.close();
                    } else {
                        //Si es la primera vez que se pulsa la imagen, actualiza la posicion del mapa con la ubicacion obtenida
                        imagen_seleccionada = position;
                        cursor.moveToFirst();
                        ubicacion_actual = new LatLng(cursor.getDouble(cursor.getColumnIndex(
                                ContratoPhotoMapp.Fotos.COLUMNA_LATITUD)),
                                cursor.getDouble(cursor.getColumnIndex(ContratoPhotoMapp.Fotos.COLUMNA_LONGITUD)));
                        actualizarUbicacion(ubicacion_actual);
                        actualizarLeyendasMapa(
                                Util.obtenerDireccion(getApplicationContext(), ubicacion_actual),
                                cursor.getString(cursor.getColumnIndex(ContratoPhotoMapp.Fotos.COLUMNA_FECHA)));
                        cursor.close();
                    }
                } else {
                    //Si esta en modo contextual, el click solo cambia el estado de seleccion
                    ActivityMapa.this.cambiarEstadoSeleccion(position);
                }
            }

            @Override
            public void itemLongClick(View view, int position){
                //Si hay zoom quitamos el zoom e ignoramoe el click
                if (animador.quitarZoomActual()){
                    return;
                }
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
     * cambiarEstadoSeleccion: cambia el estado de seleccion de la imagen de la posicion
     * establecida, y si el numero de imagenes seleccionadas es cero, cierra el modo contextual.
     * Si el numero de imagenes es exactamente uno,muestra el boton editar en el menu contextual
     * para editar el nombre de la imagen.
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
            mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, ZOOM_MAPA),
                    TIEMPO_ANIMACION_MAPA, null);
    }

    /**
     * actualizarLeyendasMapa: Pone los datos de direccion y fecha especificados en las leyendas
     * sobre el mapa.
     * @param direccion Address con la direccion nueva
     * @param fecha String con la fecha nueva
     */
    private void actualizarLeyendasMapa(Address direccion, String fecha){
        if (fecha_imagen.getVisibility() == View.INVISIBLE ||
                ciudad_imagen.getVisibility() == View.INVISIBLE ||
                direccion_imagen.getVisibility() == View.INVISIBLE){
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
    private void hacerLeyendasVisibles(boolean visibles){
        ImageButton boton_direcciones = (ImageButton)findViewById(R.id.ic_direcciones);
        if (visibles){
            animador.disolverAparecer(fecha_imagen);
            animador.disolverAparecer(ciudad_imagen);
            animador.disolverAparecer(direccion_imagen);
            animador.disolverAparecer(boton_direcciones);
        } else{
            animador.disolverDesaparecer(fecha_imagen);
            animador.disolverDesaparecer(ciudad_imagen);
            animador.disolverDesaparecer(direccion_imagen);
            animador.disolverDesaparecer(boton_direcciones);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISO_ACCESO_UBICACION: {
                if (!(grantResults.length > 0 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    acceso_ubicacion = false;
                    Toast.makeText(this, getString(R.string.permiso_ubicacion_denegado),
                            Toast.LENGTH_LONG).show();
                } else{
                    acceso_ubicacion = true;
                }
                break;
            }
        }
    }

    /**
     * mostrarDirecciones: Crea un intent para mostrar la direccion actual en otro mapa.
     * @param view boton mostrar direcciones
     */
    public void mostrarDirecciones(View view){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("geo:" + ubicacion_actual.latitude + "," +
                ubicacion_actual.longitude + "?q=" + ubicacion_actual.latitude + "," +
                ubicacion_actual.longitude + "()"));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

}