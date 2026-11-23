package es.uvigo.esei.dagss.microkernel.core;

import es.uvigo.esei.dagss.microkernel.core.api.Matrix;
import es.uvigo.esei.dagss.microkernel.core.api.PluginNotFoundException;
import es.uvigo.esei.dagss.microkernel.core.support.FakeTransformation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineTest {

    private static final Matrix ONE = Matrix.of(new double[][]{{1, 2}});

    private final PluginRegistry registry = new PluginRegistry(false);

    private static Matrix addOne(Matrix matrix) {
        double[][] values = matrix.toArray();
        for (int row = 0; row < values.length; row++) {
            for (int column = 0; column < values[row].length; column++) {
                values[row][column] += 1;
            }
        }
        return Matrix.of(values);
    }

    private static Matrix timesTen(Matrix matrix) {
        double[][] values = matrix.toArray();
        for (int row = 0; row < values.length; row++) {
            for (int column = 0; column < values[row].length; column++) {
                values[row][column] *= 10;
            }
        }
        return Matrix.of(values);
    }

    @Test
    void aplicaPasosEnOrden() {
        registry.register(new FakeTransformation("add-one", PipelineTest::addOne));
        registry.register(new FakeTransformation("times-ten", PipelineTest::timesTen));

        Matrix first = Pipeline.of(registry, "add-one,times-ten").apply(ONE);
        Matrix second = Pipeline.of(registry, "times-ten,add-one").apply(ONE);

        assertEquals(Matrix.of(new double[][]{{20, 30}}), first);
        assertEquals(Matrix.of(new double[][]{{11, 21}}), second);
    }

    @Test
    void ignoraIdsVaciosPeroExigeAlgunPaso() {
        registry.register(new FakeTransformation("add-one", PipelineTest::addOne));

        Matrix result = Pipeline.of(registry, " add-one , , ").apply(ONE);
        assertEquals(Matrix.of(new double[][]{{2, 3}}), result);

        assertThrows(IllegalArgumentException.class,
                () -> Pipeline.of(registry, " , "));
    }

    @Test
    void fallaSiElPluginNoExiste() {
        assertThrows(PluginNotFoundException.class,
                () -> Pipeline.of(registry, "add-one"));
    }

    @Test
    void noModificaLaMatrizDeEntrada() {
        registry.register(new FakeTransformation("times-ten", PipelineTest::timesTen));

        Pipeline.of(registry, "times-ten").apply(ONE);

        assertEquals(Matrix.of(new double[][]{{1, 2}}), ONE);
    }

    @Test
    void fallaSiUnPluginDevuelveNull() {
        registry.register(new FakeTransformation("roto", matrix -> null));

        NullPointerException error = assertThrows(NullPointerException.class,
                () -> Pipeline.of(registry, "roto").apply(ONE));

        assertTrue(error.getMessage().contains("roto"));
    }

    @Test
    void describeElPipeline() {
        registry.register(new FakeTransformation("add-one", PipelineTest::addOne));
        registry.register(new FakeTransformation("times-ten", PipelineTest::timesTen));

        assertEquals("add-one -> times-ten",
                Pipeline.of(registry, "add-one,times-ten").description());
    }
}
