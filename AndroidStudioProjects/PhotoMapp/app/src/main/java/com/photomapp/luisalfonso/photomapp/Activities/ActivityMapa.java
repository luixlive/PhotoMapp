package com.photomapp.luisalfonso.photomapp.Activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.photomapp.luisalfonso.photomapp.AdaptadorListaFotos;
import com.photomapp.luisalfonso.photomapp.R;
import com.photomapp.luisalfonso.photomapp.data.ContratoPhotoMapp;

import java.io.File;
import java.util.ArrayList;

/**
 * Clase ActivityMapa: muestra un mapa y las fotos que el usuraio a tomado. Al pulsar una foto el mapa muestra
 * donde se la tomó. Implementa la interfaz OnMapReadyCallback para obtener eventos del GoogleMap.
 */
public class ActivityMapa extends AppCompatActivity implements OnMapReadyCallback {

    //Etiqueta para la escritura al LOG
    private static final String LOG_TAG = "ACTIVITY MAPA";

    //Macros para solicitar permisos, y del mapa: altura del zoom y tiempo de la animacion de movimiento
    private static final int PERMISO_ACCESO_UBICACION = 1;
    private static final float ZOOM_MAPA = 18.0f;
    private static final int TIEMPO_ANIMACION_MAPA = 1000 * 3;

    //Mapa y lista con los ids de las fotos para inicizliar el adaptador del RecyclerView
    private GoogleMap mapa;
    private ArrayList<Integer> ids_fotos;

    //Bandera que nos indica si se puede leer en el almacenamiento externo y si el usuario da acceso a su ubicacion
    private boolean lectura_posible;
    private boolean acceso_ubicacion = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        iniciarListaSiEsPosible();
        iniciarMapa();
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

                File archivo_foto = new File(ruta_fotos + File.separator + ActivityPrincipal.NOMBRE_ALBUM_FOTOS +
                        File.separator + nombre_foto + ActivityPrincipal.EXTENSION_ARCHIVO_FOTO);
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
        AdaptadorListaFotos adaptador = new AdaptadorListaFotos(this, arreglo_nombres_imagenes);
        lista_fotos.setAdapter(adaptador);
        //Si se da click en una imagen, se actualiza la ubicacion del mapa de acuerdo a los datos de la base de datos
        adaptador.setOnItemClickListener(new AdaptadorListaFotos.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String[] projection = {
                        ContratoPhotoMapp.Fotos.COLUMNA_LATITUD,
                        ContratoPhotoMapp.Fotos.COLUMNA_LONGITUD,
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
                } else {
                    //Se actualiza la posicion del mapa con la ubicacion obtenida
                    cursor.moveToFirst();
                    LatLng ubicacion_foto = new LatLng(cursor.getDouble(cursor.getColumnIndex(
                            ContratoPhotoMapp.Fotos.COLUMNA_LATITUD)),
                            cursor.getDouble(cursor.getColumnIndex(ContratoPhotoMapp.Fotos.COLUMNA_LONGITUD)));
                    actualizarUbicacion(ubicacion_foto);
                    cursor.close();
                }
            }
        });
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

}