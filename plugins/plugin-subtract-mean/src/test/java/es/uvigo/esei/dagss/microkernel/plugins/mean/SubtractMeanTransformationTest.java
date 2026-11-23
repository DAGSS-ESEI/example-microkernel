package es.uvigo.esei.dagss.microkernel.plugins.mean;

import es.uvigo.esei.dagss.microkernel.core.api.Matrix;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SubtractMeanTransformationTest {

    private final SubtractMeanTransformation plugin = new SubtractMeanTransformation();

    @Test
    void declaraSusMetadatos() {
        assertEquals("subtract-mean", plugin.id());
        assertFalse(plugin.name().isBlank());
        assertFalse(plugin.description().isBlank());
    }

    @Test
    void restaLaMediaDeCadaColumna() {
        Matrix input = Matrix.of(new double[][]{
                {1, 10},
                {3, 20},
                {5, 30}});

        Matrix result = plugin.apply(input);

        assertMatrix(new double[][]{
                {-2, -10},
                {0, 0},
                {2, 10}}, result);
    }

    @Test
    void noModificaLaEntrada() {
        Matrix input = Matrix.of(new double[][]{{1, 2}, {3, 4}});

        plugin.apply(input);

        assertMatrix(new double[][]{{1, 2}, {3, 4}}, input);
    }

    @Test
    void funcionaConUnaSolaFila() {
        Matrix result = plugin.apply(Matrix.of(new double[][]{{7, 9}}));

        assertMatrix(new double[][]{{0, 0}}, result);
    }

    private static void assertMatrix(double[][] expected, Matrix actual) {
        assertEquals(expected.length, actual.rows());
        assertEquals(expected[0].length, actual.columns());
        for (int row = 0; row < expected.length; row++) {
            for (int column = 0; column < expected[row].length; column++) {
                assertEquals(expected[row][column], actual.get(row, column), 1e-9,
                        "Celda (" + row + ", " + column + ")");
            }
        }
    }
}
