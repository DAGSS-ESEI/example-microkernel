package es.uvigo.esei.dagss.microkernel.plugins.minmax;

import es.uvigo.esei.dagss.microkernel.core.api.Matrix;
import es.uvigo.esei.dagss.microkernel.core.api.Transformation;

/**
 * Escala cada columna al intervalo [0, 1]:
 * {@code (x - min) / (max - min)}.
 */
public final class MinMaxTransformation implements Transformation {

    @Override
    public String id() {
        return "minmax";
    }

    @Override
    public String name() {
        return "Escala min-max";
    }

    @Override
    public String description() {
        return "Escala cada columna al intervalo [0, 1]";
    }

    @Override
    public Matrix apply(Matrix input) {
        double[][] result = input.toArray();

        for (int column = 0; column < input.columns(); column++) {
            double[] values = input.column(column);
            double min = min(values);
            double max = max(values);
            double range = max - min;

            if (range == 0) {
                throw new IllegalArgumentException(
                        "La columna " + column + " es constante (min = max = " + min
                                + "): la escala min-max no está definida");
            }

            for (int row = 0; row < input.rows(); row++) {
                result[row][column] = (values[row] - min) / range;
            }
        }
        return Matrix.of(result);
    }

    private static double min(double[] values) {
        double min = values[0];
        for (double value : values) {
            min = Math.min(min, value);
        }
        return min;
    }

    private static double max(double[] values) {
        double max = values[0];
        for (double value : values) {
            max = Math.max(max, value);
        }
        return max;
    }
}
