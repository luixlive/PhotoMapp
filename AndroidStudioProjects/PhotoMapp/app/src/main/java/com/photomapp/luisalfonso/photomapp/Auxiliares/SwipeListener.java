package com.photomapp.luisalfonso.photomapp.Auxiliares;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Clase SwipeListener: Clase que funciona como implementacion de OnTouchListener y que detecta
 * Swipes. Si se implementa a una vista, se dejaran de escuchar el resto de eventos de gestos, esta
 * clase solo cubre el click, y el longclick. Más en:
 * http://developer.android.com/intl/es/training/gestures/detector.html
 */
public abstract class SwipeListener implements View.OnTouchListener{

    //Detector de gestos al que pasaremos todos los eventos para que se encargue de catalogarlos
    private final GestureDetector detector_gestos;

    //Vista que se presiona en cada evento
    private View vista_tocada;

    public SwipeListener(Context context) {
        detector_gestos = new GestureDetector(context, new GestosListener());
    }

    //Metodos abstractos que hay que implementar para recibir los eventos
    public abstract void swipeIzquierda(View v);
    public abstract void swipeDerecha(View v);
    public abstract void swipeArriba(View v);
    public abstract void swipeAbajo(View v);

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        vista_tocada = v;

        //Mientras dure pulsada una vista se pone la bandera pressed para que cambie su background
        if (event.getAction() == MotionEvent.ACTION_DOWN){
            v.setPressed(true);
        } else if (event.getAction() == MotionEvent.ACTION_UP){
            v.setPressed(false);
        }

        //Le pasamos los eventos al detector para que lance sus eventos
        return detector_gestos.onTouchEvent(event);
    }

    /**
     * Clase GestosListener: Implementacion de SimpleOnGestureListener para capturar los swipes y
     * llamar los metodos click y longclick, ya que al subscribirse al onTouch se dejan de escuchar
     * los demas gestos por default
     */
    private final class GestosListener extends GestureDetector.SimpleOnGestureListener {

        //Distancia minima para considerarse swipe
        private final static int LONGITUD_MINIMA_SWIPE = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            //Llamamos el click manualmente
            vista_tocada.performClick();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            //Lamamos el longclick manualmente
            vista_tocada.performLongClick();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            //Se calculan las distancias del gesto
            float distancia_x = e2.getX() - e1.getX();
            float distancia_y = e2.getY() - e1.getY();

            //De acuerdo a la distancia mayor se determina la direccion del swipe
            if (Math.abs(distancia_x) > Math.abs(distancia_y) &&
                    Math.abs(distancia_x) > LONGITUD_MINIMA_SWIPE) {
                if (distancia_x > 0) {
                    swipeDerecha(vista_tocada);
                }
                else {
                    swipeIzquierda(vista_tocada);
                }
                return true;
            } else if (Math.abs(distancia_y) > LONGITUD_MINIMA_SWIPE){
                if (distancia_y > 0) {
                    swipeAbajo(vista_tocada);
                }
                else {
                    swipeArriba(vista_tocada);
                }
                return true;
            }

            return false;
        }
    }
}
