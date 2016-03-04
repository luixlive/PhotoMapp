package com.photomapp.luisalfonso.photomapp.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by LUIS ALFONSO on 13/02/2016.
 * Clase ActivitySplash: su unica tarea es mostrar el logo de la app al abrir la aplicacion, para dar unos
 * segundos a que la activity inicial cargue que el usuario no tenga que esperar viendo una pantalla blanca.
 * La pantalla de Splash se define en splash_background en la carpeta drawable y se agrega a la activity en el manifest.
 */
public class ActivitySplash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Lo unico que hace es iniciar la ActivityPrincipal
        startActivity(new Intent(this, ActivityPrincipal.class));
        finish();
    }

}
