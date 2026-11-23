package es.uvigo.esei.dagss.microkernel.core.api;

/**
 * Contrato (extension point) que implementan todos los plugins del sistema.
 *
 * <p>El core solo conoce esta interfaz: nunca importa una clase concreta de un
 * plugin. Cualquier JAR que incluya una implementación y la declare en
 * {@code META-INF/services/} pasa a formar parte de la aplicación sin recompilar
 * ni tocar el core.</p>
 *
 * <p><b>Reglas del contrato</b></p>
 * <ul>
 *   <li>{@link #id()}: identificador estable y único. Es lo que el usuario escribe
 *       en la línea de comandos, así que cambiarlo rompe a los clientes.</li>
 *   <li>Las implementaciones deben ser <b>sin estado</b> y, preferiblemente,
 *       seguras para uso concurrente.</li>
 *   <li>Deben tener un constructor público sin argumentos: es lo que exige
 *       {@link java.util.ServiceLoader}.</li>
 *   <li>{@link #apply(Matrix)} no debe modificar la matriz recibida. La clase
 *       {@link Matrix} es inmutable, así que el contrato se cumple "gratis".</li>
 * </ul>
 */
public interface Transformation {

    /**
     * Identificador estable y único del plugin, en minúsculas y con guiones
     * (por ejemplo {@code "subtract-mean"}).
     */
    String id();

    /**
     * Nombre legible para mostrar al usuario.
     */
    String name();

    /**
     * Descripción breve de lo que hace el plugin.
     */
    String description();

    /**
     * Aplica la transformación y devuelve una matriz nueva. Nunca modifica
     * {@code input} ni devuelve {@code null}.
     */
    Matrix apply(Matrix input);
}
