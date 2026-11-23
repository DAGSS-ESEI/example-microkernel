package es.uvigo.esei.dagss.microkernel.core.support;

import es.uvigo.esei.dagss.microkernel.core.api.Matrix;
import es.uvigo.esei.dagss.microkernel.core.api.Transformation;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Plugin falso para tests del core: permite probar el registro y el pipeline sin
 * arrastrar los módulos reales de plugins (que, además, crearían un ciclo de
 * dependencias core -> plugins -> core).
 */
public final class FakeTransformation implements Transformation {

    private final String id;
    private final UnaryOperator<Matrix> behaviour;

    public FakeTransformation(String id, UnaryOperator<Matrix> behaviour) {
        this.id = Objects.requireNonNull(id, "id");
        this.behaviour = Objects.requireNonNull(behaviour, "behaviour");
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return "Fake " + id;
    }

    @Override
    public String description() {
        return "Plugin de prueba " + id;
    }

    @Override
    public Matrix apply(Matrix input) {
        return behaviour.apply(input);
    }
}
