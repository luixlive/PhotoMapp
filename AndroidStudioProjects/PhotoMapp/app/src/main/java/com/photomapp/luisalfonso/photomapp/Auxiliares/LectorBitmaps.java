package com.photomapp.luisalfonso.photomapp.Auxiliares;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.LruCache;
import android.widget.ImageView;

import com.photomapp.luisalfonso.photomapp.R;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Clase LectorBitmaps: Ayuda a extraer imagenes como bitmaps del almacenamiento externo con un
 * optimo uso de recursos. Las buenas practicas usadas aqui se extrajeron de:
 * http://developer.android.com/intl/es/training/displaying-bitmaps/index.html
 */
public class LectorBitmaps {

    //Modificar junto con el layout de la activity mapa para tener la relacion de la RecycleView
    //respecto al parent
    public static final int RELACION_PANTALLA_LISTA = 4;

    //Macros
    private static final String RUTA_AUN_NO_INDICADA = "NULO";
    private static final int KILO_BYTE = 1024;
    private static final int FRACCION_MEMORIA_CACHE = 12;

    //Variable que almacena la longitud del contenedor de las imagenes
    private int longitud_lado_img_lista;

    //Imagen cargando
    private Bitmap imagen_cargando;

    //Variable que representa nuestra memoria cache para almacenar las imagenes
    private LruCache<String, Bitmap> memoria_cache;

    //Set de referencias de los bitmaps que se van desechando para reutilizarlos y ahorrar memoria
    private final Set<SoftReference<Bitmap>> bitmaps_desechados;

    /**
     * LectorBitmaps: Constructor. Obtiene el alto de la pantalla y el contenedor de la imagen, y
     * obtiene la imagen cargando que se muestra mientras se obtienen los bitmaps.
     */
    public LectorBitmaps(Activity activity) {
        int pantalla_alto = activity.getResources().getDisplayMetrics().heightPixels;
        longitud_lado_img_lista = pantalla_alto/RELACION_PANTALLA_LISTA;
        bitmaps_desechados = Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());

        obtenerImagenCargando(activity);
        obtenerMemoriaCache();
    }

    /**
     * obtenerImagenCargando: Obtiene el bitmap "cagando" que se situan en las posiciones de la
     * lista donde aun no cargan las imagenes.
     * @param activity contexto donde se ubica la lista.
     */
    private void obtenerImagenCargando(Activity activity) {
        //Obtenemos el icono
        Bitmap icono_cache =
                BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_guardando_cache);
        //Con librerias Canvas creamos un rectangulo y ponemos el icono justo en el centro
        imagen_cargando = Bitmap.createBitmap(longitud_lado_img_lista, longitud_lado_img_lista,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(imagen_cargando);
        canvas.drawColor(ContextCompat.getColor(activity, R.color.colorAccent));
        Rect rectangulo_posicion_icono = new Rect(
                (longitud_lado_img_lista-icono_cache.getWidth())/2,
                (longitud_lado_img_lista-icono_cache.getHeight())/2,
                longitud_lado_img_lista-((longitud_lado_img_lista-icono_cache.getWidth())/2),
                longitud_lado_img_lista-((longitud_lado_img_lista-icono_cache.getHeight())/2)
        );
        canvas.drawBitmap(icono_cache, null, rectangulo_posicion_icono, null);
    }

    /**
     * obtenerMemoriaCache: Obtiene una memoria cache de la memoria disponible para la app.
     */
    private void obtenerMemoriaCache() {
        //Obtenemos la memoria disponible para la app y asignamos un dieciseisavo para la cache
        final int memoria_maxima = (int) (Runtime.getRuntime().maxMemory() / KILO_BYTE);
        final int tamano_cache = memoria_maxima / FRACCION_MEMORIA_CACHE;
        memoria_cache = new LruCache<String, Bitmap>(tamano_cache) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / KILO_BYTE;
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap antiguo, Bitmap nuevo) {
                //Si se desecha un bitmap lo agregamos al conjunto de reutilizables
                bitmaps_desechados.add(new SoftReference<>(antiguo));
            }
        };
    }

    /**
     * extraerBitmapEscaladoAlmacenaiento: Metodo estatico que sirve para extraer cualquier bitmap
     * del almacenamiento externo escalado, sin embargo lo hace en el hilo actual.
     * @param ruta_archivo String con la ruta donde se ubica la imagen
     * @param longitud Longitud mas grande de los lados de la imagen para escalarla
     * @return Bitmap imagen escalada.
     */
    public static Bitmap extraerBitmapEscaladoAlmacenaiento(String ruta_archivo, int longitud){
        final BitmapFactory.Options opciones_imagen = new BitmapFactory.Options();
        opciones_imagen.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(ruta_archivo, opciones_imagen);
        opciones_imagen.inSampleSize = calcularFactorEscala(opciones_imagen, longitud, longitud);
        opciones_imagen.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(ruta_archivo, opciones_imagen);
    }

    public static Bitmap extraerBitmapEscaladoBytes(byte[] bytes, int longitud){
        final BitmapFactory.Options opciones_imagen = new BitmapFactory.Options();
        opciones_imagen.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opciones_imagen);
        opciones_imagen.inSampleSize = calcularFactorEscala(opciones_imagen, longitud, longitud);
        opciones_imagen.inJustDecodeBounds = false;

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opciones_imagen);
    }

    /**
     * extraerImagenEn: Extrae el bitmap de la ruta especificada con el tamano necesario para
     * situarlo en el contenedor que se especifique, de forma optima para el ahorro de recursos.
     * @param activity contexto donde se ubica el contenedor.
     * @param contenedor_imagen ImageView que contendra a la imagen una vez cargada.
     * @param ruta String con la ruta del almacenamiento externo donde se ubica el bitmap.
     */
    public void extraerImagenListaEn(Activity activity, ImageView contenedor_imagen, String ruta){
        if (cancelarExtractorInnecesario(ruta, contenedor_imagen)) {
            final Bitmap bitmap_cache = memoria_cache.get(ruta);
            if (bitmap_cache != null) {
                contenedor_imagen.setImageBitmap(bitmap_cache);
            } else {
                //si ya se libero el extractor innecesario se inicia uno nuevo con la nueva imagen
                HiloExtractorBitmap extractor = new HiloExtractorBitmap(contenedor_imagen);
                contenedor_imagen.setImageDrawable(
                        new AsyncDrawable(activity.getResources(), imagen_cargando, extractor));

                String parametros[] = { ruta };
                extractor.execute(parametros);
            }
        }
    }

    /**
     * cancelarExtractorInnecesario: Checa si ya existe un extractor para dicha ruta de imagen, de
     * ser asi lo cancela.
     * @param ruta String de la ruta de la imagen
     * @param contenedor_imagen ImageView del contenedor de la imagen
     * @return false si ya existe un hilo extrayendo ese bitmap, true si no existe o se logro
     * cancelar un hilo innecesario
     */
    public static boolean cancelarExtractorInnecesario(String ruta, ImageView contenedor_imagen) {
        //Obtenemos el hilo linkeado a este contenedor, si el contenedor ya se reciclo, no
        //coincidiran las rutas y lo cancelamos
        final HiloExtractorBitmap extractor = obtenerHiloExtractorBitmap(contenedor_imagen);

        if (extractor != null) {
            if (extractor.ruta.equals(RUTA_AUN_NO_INDICADA) || !extractor.ruta.equals(ruta)) {
                extractor.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * obtenerHiloExtractorBitmap: Recupera el hilo extractor linkeado con este ImageView.
     * @param contenedor_imagen ImageView contenedor del que se quiere obtener el hilo extractor.
     * @return HiloExtractorBitmap linkeado a este contenedor, o null si no existe
     */
    private static HiloExtractorBitmap obtenerHiloExtractorBitmap(ImageView contenedor_imagen) {
        if (contenedor_imagen != null) {
            final Drawable drawable = contenedor_imagen.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                return ((AsyncDrawable)drawable).obtenerHiloExtractorBitmap();
            }
        }
        return null;
    }

    /**
     * extraerBitmapEscalado: Extrae un bitmap del almacenamiento externo escalado de forma optima
     * para ahorrar memoria.
     * @return Bitmap con la imagen escalada
     */
    private Bitmap extraerBitmapListaEscaladoAlmacenamiento(String ruta_archivo) {
        final BitmapFactory.Options opciones_imagen = new BitmapFactory.Options();
        opciones_imagen.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(ruta_archivo, opciones_imagen);
        opciones_imagen.inSampleSize = calcularFactorEscala(opciones_imagen,
                longitud_lado_img_lista, longitud_lado_img_lista);
        opciones_imagen.inJustDecodeBounds = false;
        //Si existe un bitmap desechado que se pueda reutilizar activamos la opcion inBitmap para
        //reciclar
        ponerOpcionInBitmap(opciones_imagen);

        return BitmapFactory.decodeFile(ruta_archivo, opciones_imagen);
    }

    /**
     * calcularFactorEscala: calcula de acuerdo al tamano necesario de la imagen, el factor
     * conveniente de escalado de la imagen para ahorrar memoria.
     * @param opciones_imagen BitmapFactory.Options de la imagen a extraer.
     * @param ancho_requerido int del ancho necesario de la imagen
     * @param alto_requerido int del alto necesario
     * @return entero con el factor de escala
     */
    private static int calcularFactorEscala(BitmapFactory.Options opciones_imagen,
                                            int ancho_requerido, int alto_requerido) {
        final int alto_imagen = opciones_imagen.outHeight;
        final int ancho_imagen = opciones_imagen.outWidth;
        int tamano_escala = 1;

        if (alto_imagen > alto_requerido || ancho_imagen > ancho_requerido) {
            final int mitad_alto = alto_imagen / 2;
            final int mitad_ancho = ancho_imagen / 2;
            while ((mitad_alto / tamano_escala) > alto_requerido &&
                    (mitad_ancho / tamano_escala) > ancho_requerido) {
                tamano_escala *= 2;
            }
        }
        return tamano_escala;
    }

    /**
     * ponerOpcionInBitmap: Checa si es posible reutilizar un bitmap desechado de acuerdo a los
     * requerimientos de esta imagen en particular, de ser asi activa la opcion inBitmap.
     * @param opciones BitmapFactory.Options de la imagen nueva.
     */
    private void ponerOpcionInBitmap(BitmapFactory.Options opciones) {
        //Tiene que ser mutable
        opciones.inMutable = true;
        Bitmap bitmap_reutilizable = obtenerBitmapDesechado(opciones);
        //Activa inBitmap en las opciones con el bitmap encontrado
        if (bitmap_reutilizable != null) {
            opciones.inBitmap = bitmap_reutilizable;
        }
    }

    /**
     * obtenerBitmapDesechado: Itera por todos los bitmaps desechados y buscando uno que se pueda
     * reutilizar.
     * @param opciones BitmapFactory.Options de la imagen nueva.
     * @return bitmap que se puede reutilizar o nulo si no existe
     */
    protected Bitmap obtenerBitmapDesechado(BitmapFactory.Options opciones) {
        Bitmap bitmap_reutilizable = null;

        if (!bitmaps_desechados.isEmpty()) {
            synchronized (bitmaps_desechados) {
                final Iterator<SoftReference<Bitmap>> iterador = bitmaps_desechados.iterator();
                Bitmap desechado;

                while (iterador.hasNext()) {
                    desechado = iterador.next().get();
                    if (null != desechado && desechado.isMutable()) {
                        if (reutilizableParaInBitmap(desechado, opciones)) {
                            bitmap_reutilizable = desechado;
                            iterador.remove();
                            break;
                        }
                    } else {
                        iterador.remove();
                    }
                }
            }
        }
        return bitmap_reutilizable;
    }

    /**
     * reutilizableParaInBitmap: Checa si un bitmap se puede reutilizar de acuerdo a las opciones
     * especificadas.
     * @param candidato Bitmap que se va a investigar
     * @param opciones BitmapFactory.Options que el candidato debe satisfacer
     * @return true si se puede reutilizar este candidato, false de otro modo.
     */
    static boolean reutilizableParaInBitmap(Bitmap candidato, BitmapFactory.Options opciones) {
        int ancho = opciones.outWidth / opciones.inSampleSize;
        int alto = opciones.outHeight / opciones.inSampleSize;
        Bitmap.Config configuracion = candidato.getConfig();
        int bytes_por_pixel;
        //Checa cada configuracion
        if (configuracion == Bitmap.Config.ARGB_8888) {
            bytes_por_pixel = 4;
        } else if (configuracion == Bitmap.Config.RGB_565) {
            bytes_por_pixel = 2;
        } else if (configuracion == Bitmap.Config.ARGB_4444) {
            bytes_por_pixel = 2;
        } else {
            bytes_por_pixel = 1;
        }
        int conteo_bytes = ancho * alto * bytes_por_pixel;
        return conteo_bytes <= candidato.getAllocationByteCount();
    }

    private class HiloExtractorBitmap extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> referencia_contenedor_imagen;
        //Se hace global para utilizarlo como referencia al momento de preguntar si el extractor
        //sigue siendo necesario o si el RecylcerView ya reciclo la vista
        public String ruta = RUTA_AUN_NO_INDICADA;

        public HiloExtractorBitmap(ImageView contenedor_imagen) {
            referencia_contenedor_imagen = new WeakReference<>(contenedor_imagen);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            //En background extraemos el bitmap y lo agregamos a cache
            ruta = params[0];
            Bitmap imagen = extraerBitmapListaEscaladoAlmacenamiento(ruta);
            if (imagen == null){
                return null;
            }
            if (memoria_cache.get(ruta) == null) {
                memoria_cache.put(ruta, imagen);
            }
            return imagen;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            //Si se cancelo el hilo
            if (isCancelled()) {
                bitmap = null;
            }

            //Si no se cancelo, ponemos el bitmap en su contenedor
            if (referencia_contenedor_imagen != null && bitmap != null) {
                final ImageView contenedor_imagen = referencia_contenedor_imagen.get();
                final HiloExtractorBitmap extractor = obtenerHiloExtractorBitmap(contenedor_imagen);
                if (this == extractor && contenedor_imagen != null) {
                    contenedor_imagen.setImageBitmap(bitmap);
                }
            }
        }
    }

    /**
     * Clase AsyncDrawable: Clase de apoyo que almacena el hilo extractor del bitmap de cada
     * imageview que se va generando en el adaptador. Esto es necesario debido a que el
     * recyclerview, asi como listview y otros, ahorra recursos reutilizando las vistas
     * y es necesario saber si la vista se reutiliza o destruye antes de terminar de leerla del
     * almacenamiento externo.
     * Mas en: http://developer.android.com/intl/es/training/displaying-bitmaps/process-bitmap.html
     */
    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<HiloExtractorBitmap> extractor_bitmap_referencia;

        /**
         * AsyncDrawable:Constructor
         */
        public AsyncDrawable(Resources res, Bitmap bitmap, HiloExtractorBitmap extractor_bitmap) {
            super(res, bitmap);
            extractor_bitmap_referencia = new WeakReference<>(extractor_bitmap);
        }

        /**
         * HiloExtractorBitmap: nos entrega el hilo que se almaceno en la referencia.
         * @return HiloExtractorBitmap almacenado en la referencia actual.
         */
        public HiloExtractorBitmap obtenerHiloExtractorBitmap() {
            return extractor_bitmap_referencia.get();
        }
    }

}