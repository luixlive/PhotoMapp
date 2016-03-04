package com.photomapp.luisalfonso.photomapp.Activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
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

/**
 * Clase ActivityMapa: muestra un mapa y las fotos que el usuraio a tomado. Al pulsar una foto el mapa muestra
 * donde se la tomó. Implementa la interfaz OnMapReadyCallback para obtener eventos del GoogleMap.
 */
public class ActivityMapa extends AppCompatActivity implements OnMapReadyCallback {

    private static final String LOG_TAG = ActivityMapa.class.getName();

    //Macros
    private static final int PERMISO_ACCESO_UBICACION = 1;
    private static final float ZOOM_MAPA = 16.0f;

    //Mapa y cursor del resultado de un query al ContentProvider
    private GoogleMap mapa;
    private Cursor cursor;
    //Banderas que nos indican si es posible accesar al almacenamiento externo
    private boolean escritura_posible;
    private boolean lectura_posible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        obtenerDatosIntent();
        iniciarMapa();
        if (lectura_posible) {
            mostrarListaFotos();
        }
    }

    /**
     * obtenerDatosIntent: Recupera los datos enviados desde la otra activite.
     */
    private void obtenerDatosIntent() {
        Intent intent = getIntent();
        //Datos sobre el almacenamiento externo y su acceso
        if (intent.hasExtra(ActivityPrincipal.EXTRA_ESCRITURA_POSIBLE))
            escritura_posible = intent.getBooleanExtra(ActivityPrincipal.EXTRA_ESCRITURA_POSIBLE, false);
        if (intent.hasExtra(ActivityPrincipal.EXTRA_LECTURA_POSIBLE))
            lectura_posible = intent.getBooleanExtra(ActivityPrincipal.EXTRA_LECTURA_POSIBLE, false);
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
        LinearLayoutManager manejador_layout_horizontal = new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL, false);
        //Utilizamo un objeto RecyclerView porque se le puede configurar un Layout horizontal para que muestre la lista
        //de forma horizontal.
        RecyclerView lista_fotos = (RecyclerView) findViewById(R.id.lista_fotos);
        lista_fotos.setLayoutManager(manejador_layout_horizontal);
        lista_fotos.setHasFixedSize(true);

        /////////////////////////CODIGO NO FINAL
        Cursor cursor_contador = getContentResolver().query(ContratoPhotoMapp.Fotos.CONTENT_URI, new String[]{"count(*) AS count"},
                null, null, null);
        cursor_contador.moveToFirst();
        int numero_fotos_base_datos = cursor_contador.getInt(0);
        cursor_contador.close();

        String nombres_imagenes[] = new String[numero_fotos_base_datos];
        String[] projection = {ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE};
        String clause = ContratoPhotoMapp.Fotos._ID + " <= ?";
        String[] args = {String.valueOf(numero_fotos_base_datos)};
        cursor = getContentResolver().query(ContratoPhotoMapp.Fotos.CONTENT_URI, projection, clause, args, null);
        if (cursor == null){
            Log.e(LOG_TAG, "Se retorno un cursor nulo del query al ContentProvider");
        } else if (cursor.getCount() < 1){
            Log.wtf(LOG_TAG, "No se encontro la foto con el ContentProvider");
        } else{
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++){
                nombres_imagenes[i] = cursor.getString(cursor.getColumnIndex(ContratoPhotoMapp.Fotos.COLUMNA_NOMBRE));
                cursor.moveToNext();
            }
        }
        AdaptadorListaFotos adaptador = new AdaptadorListaFotos(this, nombres_imagenes);
        lista_fotos.setAdapter(adaptador);
        adaptador.setOnItemClickListener(new AdaptadorListaFotos.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String[] projection = {
                        ContratoPhotoMapp.Fotos.COLUMNA_LATITUD,
                        ContratoPhotoMapp.Fotos.COLUMNA_LONGITUD,
                };
                String clause = ContratoPhotoMapp.Fotos._ID + " = ?";
                String[] args = {String.valueOf(position)};
                cursor = getContentResolver().query(ContratoPhotoMapp.Fotos.CONTENT_URI, projection, clause, args, null);
                if (cursor == null){
                    Log.e(LOG_TAG, "Se retorno un cursor nulo del query al ContentProvider");
                } else if (cursor.getCount() < 1){
                    Log.wtf(LOG_TAG, "No se encontro la foto con el ContentProvider");
                } else{
                    cursor.moveToFirst();
                    LatLng ubicacion_foto = new LatLng(cursor.getDouble(cursor.getColumnIndex(ContratoPhotoMapp.Fotos.COLUMNA_LATITUD)),
                            cursor.getDouble(cursor.getColumnIndex(ContratoPhotoMapp.Fotos.COLUMNA_LONGITUD)));
                    actualizarUbicacion(ubicacion_foto);
                }
            }
        });
        ////////////////////////////////////////////////////
    }

    /**
     * actualizarUbicacion: muestra la ubicacion referida en el mapa.
     * @param ubicacion: ubicacion a la que se desea llevar en el mapa.
     */
    private void actualizarUbicacion(LatLng ubicacion) {
        if (ubicacion != null)
            mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, ZOOM_MAPA));
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
        } else {
            //Llevamos el mapa a la ultima ubicacion conocida del usuario
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
                    Toast.makeText(this, getString(R.string.permiso_ubicacion_denegado), Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

}