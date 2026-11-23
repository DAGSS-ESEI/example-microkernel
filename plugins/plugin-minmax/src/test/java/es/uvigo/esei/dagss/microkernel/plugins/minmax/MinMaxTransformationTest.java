package es.uvigo.esei.dagss.microkernel.plugins.minmax;

import es.uvigo.esei.dagss.microkernel.core.api.Matrix;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinMaxTransformationTest {

    private final MinMaxTransformation plugin = new MinMaxTransformation();

    @Test
    void escalaCadaColumnaAIntervaloCeroUno() {
        Matrix input = Matrix.of(new double[][]{
                {1, 10},
                {3, 20},
                {5, 30}});

        Matrix result = plugin.apply(input);

        assertMatrix(new double[][]{
                {0, 0},
                {0.5, 0.5},
                {1, 1}}, result);
    }

    @Test
    void admiteValoresNegativos() {
        Matrix result = plugin.apply(Matrix.of(new double[][]{{-10}, {10}}));

        assertMatrix(new double[][]{{0}, {1}}, result);
    }

    @Test
    void fallaConColumnaConstante() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> plugin.apply(Matrix.of(new double[][]{{1, 5}, {2, 5}})));

        assertTrue(error.getMessage().contains("constante"));
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
