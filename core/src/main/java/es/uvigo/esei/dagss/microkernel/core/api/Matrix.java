package es.uvigo.esei.dagss.microkernel.core.api;

import java.util.Arrays;
import java.util.Objects;

/**
 * Matriz numérica inmutable.
 *
 * <p>Es el tipo que atraviesa el pipeline. Al ser inmutable, un plugin no puede
 * corromper la entrada de los siguientes pasos: los errores por efectos
 * secundarios desaparecen por diseño, no por disciplina.</p>
 */
public final class Matrix {

    private final double[][] values;
    private final int rows;
    private final int columns;

    private Matrix(double[][] values, int rows, int columns) {
        this.values = values;
        this.rows = rows;
        this.columns = columns;
    }

    /**
     * Crea una matriz a partir de un array rectangular. El array se copia: cambios
     * posteriores en el original no afectan a la matriz.
     *
     * @throws IllegalArgumentException si está vacía, no es rectangular o alguna
     *                                  fila es {@code null}
     */
    public static Matrix of(double[][] values) {
        Objects.requireNonNull(values, "values");

        if (values.length == 0) {
            throw new IllegalArgumentException("La matriz no puede estar vacía");
        }
        if (values[0] == null || values[0].length == 0) {
            throw new IllegalArgumentException("La matriz debe tener al menos una columna");
        }

        int columns = values[0].length;
        double[][] copy = new double[values.length][];
        for (int row = 0; row < values.length; row++) {
            double[] source = Objects.requireNonNull(values[row], "Fila " + row);
            if (source.length != columns) {
                throw new IllegalArgumentException(
                        "La fila " + row + " tiene " + source.length
                                + " columnas; se esperaban " + columns);
            }
            copy[row] = source.clone();
        }
        return new Matrix(copy, copy.length, columns);
    }

    public int rows() {
        return rows;
    }

    public int columns() {
        return columns;
    }

    public double get(int row, int column) {
        checkIndex(row, column);
        return values[row][column];
    }

    /**
     * Devuelve una copia de la columna indicada.
     */
    public double[] column(int column) {
        if (column < 0 || column >= columns) {
            throw new IndexOutOfBoundsException(
                    "Columna " + column + " fuera de rango [0, " + columns + ")");
        }
        double[] result = new double[rows];
        for (int row = 0; row < rows; row++) {
            result[row] = values[row][column];
        }
        return result;
    }

    /**
     * Devuelve una copia profunda de los valores. Es la forma prevista de construir
     * el resultado de un plugin: se copia, se modifica la copia y se envuelve con
     * {@link #of(double[][])}.
     */
    public double[][] toArray() {
        double[][] copy = new double[rows][];
        for (int row = 0; row < rows; row++) {
            copy[row] = values[row].clone();
        }
        return copy;
    }

    private void checkIndex(int row, int column) {
        if (row < 0 || row >= rows || column < 0 || column >= columns) {
            throw new IndexOutOfBoundsException(
                    "Posición (" + row + ", " + column + ") fuera de rango ["
                            + rows + " x " + columns + "]");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Matrix matrix)) {
            return false;
        }
        return Arrays.deepEquals(values, matrix.values);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(values);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Matrix ")
                .append(rows).append('x').append(columns);
        for (double[] row : values) {
            result.append(System.lineSeparator()).append(Arrays.toString(row));
        }
        return result.toString();
    }
}
