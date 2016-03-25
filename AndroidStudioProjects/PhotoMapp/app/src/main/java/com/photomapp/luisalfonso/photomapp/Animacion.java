package com.photomapp.luisalfonso.photomapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import java.io.File;

/**
 * Clase Animacion: Objeto animador que puede realizar multiples animaciones sobre objetos View.
 */
public class Animacion {

    private static final String LOG_TAG = "ANIMACION";

    //Referencia del animador, sirve guardarla por si es necesario cancelarlo
    private Animator animador_actual;

    //Duracion de la animacion en milisegundos
    private int duracion_animacion;

    //Variables necesarias para hacer y quitar zoom
    private Rect contornos_iniciales;
    private float escala_final_inicial;
    private View contenedor_pequeno;
    private View contenedor_grande;

    //Bandera para determinar si actualmente existe un zoom a una imagen
    private boolean hay_zoom = false;

    public Animacion(int duracion_animacion){
        this.duracion_animacion = duracion_animacion;
    }

    public void hacerZoomImagenLista(String ruta_imagen, final View contenedor_pequeno,
                                      final View contenedor_grande, int tamano_imagen_ampliada) {
        if(hay_zoom){
            quitarZoomActual();
            return;
        }
        //Si hay una animacion corriendo, se cancela
        if (animador_actual != null) {
            animador_actual.cancel();
        }
        this.contenedor_pequeno = contenedor_pequeno;
        this.contenedor_grande = contenedor_grande;

        //Cargamos la imagen en alta definicion
        File carpeta_fotos = Util.obtenerDirectorioFotos();
        if (carpeta_fotos == null) {
            Log.w(LOG_TAG, "No se encontro el directorio de fotos");
            return;
        }
        ((ImageView)contenedor_grande).setImageBitmap(
                LectorBitmaps.extraerBitmapEscaladoAlmacenaiento(ruta_imagen, tamano_imagen_ampliada));

        //Se calculan los contornos iniciales y finales de la imagen
        contornos_iniciales = new Rect();
        final Rect contornos_finales = new Rect();
        final Point offset = new Point();

        //El contorno inicial es el rectangulo del contenedor de la imagen pequena,
        //el contorno final es el de la imagen grande y el offset es el origen del contenedor grande
        contenedor_pequeno.getGlobalVisibleRect(contornos_iniciales);
        ((View)contenedor_grande.getParent()).getGlobalVisibleRect(contornos_finales, offset);
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
        contenedor_grande.setVisibility(View.VISIBLE);

        //Ponemos los pivotes de la imagen ampliada en la esquina superior izquierda
        contenedor_grande.setPivotX(0f);
        contenedor_grande.setPivotY(0f);

        //Construimos y corremos la animacion tanto de expansion como de posicion
        AnimatorSet set_animacion = new AnimatorSet();
        set_animacion
                .play(ObjectAnimator.ofFloat(contenedor_grande, View.X,
                        contornos_iniciales.left, contornos_finales.left))
                .with(ObjectAnimator.ofFloat(contenedor_grande, View.Y,
                        contornos_iniciales.top, contornos_finales.top))
                .with(ObjectAnimator.ofFloat(contenedor_grande, View.SCALE_X, escala_inicial, 1f))
                .with(ObjectAnimator.ofFloat(contenedor_grande, View.SCALE_Y, escala_inicial, 1f));
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
        contenedor_grande.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                quitarZoomActual();
            }
        });

        hay_zoom = true;
    }

    /**
     * quitarZoomActual: Hace una animacion para remover el zoom a la imagen de la lista donde se
     * aplico.
     */
    public boolean quitarZoomActual(){
        if (!hay_zoom){
            return false;
        }
        if (animador_actual != null) {
            animador_actual.cancel();
        }

        //Creamos la animacion
        AnimatorSet set_animacion = new AnimatorSet();
        set_animacion
                .play(ObjectAnimator.ofFloat(contenedor_grande, View.X, contornos_iniciales.left))
                .with(ObjectAnimator.ofFloat(contenedor_grande, View.Y, contornos_iniciales.top))
                .with(ObjectAnimator.ofFloat(contenedor_grande, View.SCALE_X, escala_final_inicial))
                .with(ObjectAnimator.ofFloat(contenedor_grande, View.SCALE_Y, escala_final_inicial));
        set_animacion.setDuration(duracion_animacion);
        set_animacion.setInterpolator(new DecelerateInterpolator());
        set_animacion.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                contenedor_pequeno.setAlpha(1f);
                contenedor_grande.setVisibility(View.GONE);
                animador_actual = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                contenedor_pequeno.setAlpha(1f);
                contenedor_grande.setVisibility(View.GONE);
                animador_actual = null;
            }
        });
        set_animacion.start();
        animador_actual = set_animacion;

        hay_zoom = false;
        return true;
    }

    public void disolverAparecer(View aparecer_vista) {
        //Hacemos la vista visible pero transparente
        aparecer_vista.setAlpha(0f);
        aparecer_vista.setVisibility(View.VISIBLE);

        //Animamos la vista para que se vuelva 100% opaca
        aparecer_vista.animate()
                .alpha(1f)
                .setDuration(duracion_animacion)
                .setListener(null);
    }

    public void disolverDesaparecer(final View desaparecer_vista){
        //Animamos la vista para que se vuelva transparente, al final las hacemos invisibles
        desaparecer_vista.animate()
                .alpha(0f)
                .setDuration(duracion_animacion)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        desaparecer_vista.setVisibility(View.INVISIBLE);
                    }
                });
    }

}
