package es.uvigo.esei.dagss.microkernel.core;

import es.uvigo.esei.dagss.microkernel.core.api.PluginNotFoundException;
import es.uvigo.esei.dagss.microkernel.core.support.FakeTransformation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginRegistryTest {

    private final PluginRegistry registry = new PluginRegistry(false);

    @Test
    void registraYListaOrdenadoPorId() {
        registry.register(new FakeTransformation("zeta", matrix -> matrix));
        registry.register(new FakeTransformation("alfa", matrix -> matrix));

        assertEquals(2, registry.size());
        assertEquals(List.of("alfa", "zeta"), List.copyOf(registry.ids()));
        assertEquals("alfa", registry.all().get(0).id());
        assertEquals("zeta", registry.all().get(1).id());
    }

    @Test
    void findDevuelveVacioSiNoExiste() {
        registry.register(new FakeTransformation("alfa", matrix -> matrix));

        assertTrue(registry.find("no-existe").isEmpty());
        assertTrue(registry.find("alfa").isPresent());
    }

    @Test
    void requireFallaConCatalogoDisponible() {
        registry.register(new FakeTransformation("alfa", matrix -> matrix));
        registry.register(new FakeTransformation("beta", matrix -> matrix));

        PluginNotFoundException error = assertThrows(
                PluginNotFoundException.class,
                () -> registry.require("gamma"));

        assertTrue(error.getMessage().contains("gamma"));
        assertTrue(error.getMessage().contains("alfa, beta"));
        assertEquals("gamma", error.pluginId());
    }

    @Test
    void rechazaIdDuplicado() {
        registry.register(new FakeTransformation("alfa", matrix -> matrix));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> registry.register(new FakeTransformation("alfa", matrix -> matrix)));

        assertTrue(error.getMessage().contains("alfa"));
    }

    @Test
    void rechazaIdVacio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register(new FakeTransformation("  ", matrix -> matrix)));
    }

    @Test
    void devuelveLaMismaInstanciaQueSeRegistro() {
        FakeTransformation plugin = new FakeTransformation("alfa", matrix -> matrix);
        registry.register(plugin);

        assertSame(plugin, registry.require("alfa"));
    }
}
