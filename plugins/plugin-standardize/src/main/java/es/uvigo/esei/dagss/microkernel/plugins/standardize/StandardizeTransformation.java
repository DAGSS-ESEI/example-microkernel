package es.uvigo.esei.dagss.microkernel.plugins.standardize;

import es.uvigo.esei.dagss.microkernel.core.api.Matrix;
import es.uvigo.esei.dagss.microkernel.core.api.Transformation;

/**
 * Estandariza cada columna: media 0 y desviación típica 1
 * ({@code z = (x - media) / desviación}). La desviación se calcula con la
 * población completa (dividiendo entre {@code n}), igual que {@code np.std}.
 */
public final class StandardizeTransformation implements Transformation {

    @Override
    public String id() {
        return "standardize";
    }

    @Override
    public String name() {
        return "Estandariza (z-score)";
    }

    @Override
    public String description() {
        return "Deja cada columna con media 0 y desviación típica 1";
    }

    @Override
    public Matrix apply(Matrix input) {
        double[][] result = input.toArray();

        for (int column = 0; column < input.columns(); column++) {
            double[] values = input.column(column);
            double mean = mean(values);
            double deviation = deviation(values, mean);

            if (deviation == 0) {
                throw new IllegalArgumentException(
                        "La columna " + column + " no tiene varianza (desviación típica 0): "
                                + "la estandarización no está definida");
            }

            for (int row = 0; row < input.rows(); row++) {
                result[row][column] = (values[row] - mean) / deviation;
            }
        }
        return Matrix.of(result);
    }

    private static double mean(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private static double deviation(double[] values, double mean) {
        double sum = 0;
        for (double value : values) {
            double difference = value - mean;
            sum += difference * difference;
        }
        return Math.sqrt(sum / values.length);
    }
}
