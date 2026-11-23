package es.uvigo.esei.dagss.microkernel.core.api;

import java.util.Collection;

/**
 * Se lanza cuando el usuario pide un plugin que el registro no ha descubierto.
 * El mensaje incluye el catálogo disponible para que el error sea accionable
 * (y para que la demo en clase no muera en un stack trace).
 */
public class PluginNotFoundException extends RuntimeException {

    private final String pluginId;

    public PluginNotFoundException(String pluginId, Collection<String> available) {
        super("No existe el plugin '" + pluginId + "'. Disponibles: "
                + (available.isEmpty() ? "(ninguno)" : String.join(", ", available)));
        this.pluginId = pluginId;
    }

    public String pluginId() {
        return pluginId;
    }
}
