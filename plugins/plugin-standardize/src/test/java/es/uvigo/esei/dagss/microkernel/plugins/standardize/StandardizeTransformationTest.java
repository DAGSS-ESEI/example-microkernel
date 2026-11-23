package es.uvigo.esei.dagss.microkernel.plugins.standardize;

import es.uvigo.esei.dagss.microkernel.core.api.Matrix;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardizeTransformationTest {

    private final StandardizeTransformation plugin = new StandardizeTransformation();

    @Test
    void dejaMediaCeroYDesviacionUno() {
        Matrix input = Matrix.of(new double[][]{
                {1, 100},
                {3, 300},
                {5, 500}});

        Matrix result = plugin.apply(input);

        assertMatrix(new double[][]{
                {-1.224744871, -1.224744871},
                {0, 0},
                {1.224744871, 1.224744871}}, result);

        Matrix summary = meanAndDeviation(result);
        assertEquals(0, summary.get(0, 0), 1e-9, "La media debe quedar a 0");
        assertEquals(1, summary.get(0, 1), 1e-9, "La desviación debe quedar a 1");
    }

    @Test
    void fallaConColumnaConstante() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> plugin.apply(Matrix.of(new double[][]{{2, 1}, {2, 3}})));

        assertTrue(error.getMessage().contains("varianza"));
        assertTrue(error.getMessage().contains("columna 0"));
    }

    @Test
    void noModificaLaEntrada() {
        Matrix input = Matrix.of(new double[][]{{1, 2}, {3, 4}});

        plugin.apply(input);

        assertMatrix(new double[][]{{1, 2}, {3, 4}}, input);
    }

    /** Devuelve en una matriz 1x2: [media, desviación] de la primera columna. */
    private static Matrix meanAndDeviation(Matrix matrix) {
        double[] values = matrix.column(0);
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        double mean = sum / values.length;
        double squared = 0;
        for (double value : values) {
            squared += (value - mean) * (value - mean);
        }
        double deviation = Math.sqrt(squared / values.length);
        return Matrix.of(new double[][]{{mean, deviation}});
    }

    private static void assertMatrix(double[][] expected, Matrix actual) {
        assertEquals(expected.length, actual.rows());
        assertEquals(expected[0].length, actual.columns());
        for (int row = 0; row < expected.length; row++) {
            for (int column = 0; column < expected[row].length; column++) {
                assertEquals(expected[row][column], actual.get(row, column), 1e-6,
                        "Celda (" + row + ", " + column + ")");
            }
        }
    }
}
