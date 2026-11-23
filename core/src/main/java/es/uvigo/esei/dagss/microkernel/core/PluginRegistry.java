package es.uvigo.esei.dagss.microkernel.core;

import es.uvigo.esei.dagss.microkernel.core.api.PluginNotFoundException;
import es.uvigo.esei.dagss.microkernel.core.api.Transformation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Registro de plugins: el corazón del microkernel.
 *
 * <p>En este ejemplo el descubrimiento lo hace {@link ServiceLoader}, el mecanismo
 * estándar del JDK (SPI): recorre el classpath buscando ficheros
 * {@code META-INF/services/<nombre-del-contrato>} y crea una instancia de cada
 * implementación declarada. No hay dependencias externas ni reflexión casera.</p>
 *
 * <p>{@link #register(Transformation)} es público a propósito: permite registrar
 * plugins a mano (útil en tests y para integraciones programáticas) y expresa el
 * concepto de "registro" sin esconderlo detrás del framework.</p>
 */
public final class PluginRegistry {

    /** SortedMap: el catálogo se lista siempre en el mismo orden (útil para demos y tests). */
    private final SortedMap<String, Transformation> plugins = new TreeMap<>();

    /**
     * Crea el registro y descubre los plugins presentes en el classpath.
     */
    public PluginRegistry() {
        this(true);
    }

    /**
     * @param discover si es {@code false}, no se usa {@link ServiceLoader}; el
     *                 registro arranca vacío y se puebla con {@link #register}.
     */
    public PluginRegistry(boolean discover) {
        if (discover) {
            ServiceLoader.load(Transformation.class).forEach(this::register);
        }
    }

    /**
     * Registra un plugin. Un id duplicado se considera un error de configuración
     * (dos JARs que reclaman el mismo identificador), no algo que resolver en
     * silencio: el comportamiento del sistema sería ambiguo.
     *
     * @throws IllegalArgumentException si el id está vacío
     * @throws IllegalStateException    si ya hay un plugin con ese id
     */
    public void register(Transformation plugin) {
        Objects.requireNonNull(plugin, "plugin");

        String id = plugin.id();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "El plugin " + plugin.getClass().getName() + " devuelve un id vacío");
        }

        Transformation previous = plugins.putIfAbsent(id, plugin);
        if (previous != null) {
            throw new IllegalStateException(
                    "Id duplicado '" + id + "': lo declaran "
                            + previous.getClass().getName() + " y "
                            + plugin.getClass().getName());
        }
    }

    public Optional<Transformation> find(String id) {
        return Optional.ofNullable(plugins.get(id));
    }

    /**
     * Igual que {@link #find(String)} pero falla con un mensaje que incluye el
     * catálogo disponible.
     */
    public Transformation require(String id) {
        return find(id).orElseThrow(
                () -> new PluginNotFoundException(id, plugins.keySet()));
    }

    /**
     * Catálogo completo, ordenado por id.
     */
    public List<Transformation> all() {
        return List.copyOf(plugins.values());
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(plugins.keySet()));
    }

    public int size() {
        return plugins.size();
    }
}
