package es.uvigo.esei.dagss.microkernel.plugins.mean;

import es.uvigo.esei.dagss.microkernel.core.api.Matrix;
import es.uvigo.esei.dagss.microkernel.core.api.Transformation;

/**
 * Resta a cada columna su valor medio (centra los datos en 0).
 */
public final class SubtractMeanTransformation implements Transformation {

    @Override
    public String id() {
        return "subtract-mean";
    }

    @Override
    public String name() {
        return "Resta la media";
    }

    @Override
    public String description() {
        return "Resta a cada columna su valor medio (centra los datos en 0)";
    }

    @Override
    public Matrix apply(Matrix input) {
        double[][] result = input.toArray();

        for (int column = 0; column < input.columns(); column++) {
            double mean = mean(input.column(column));
            for (int row = 0; row < input.rows(); row++) {
                result[row][column] -= mean;
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
}
