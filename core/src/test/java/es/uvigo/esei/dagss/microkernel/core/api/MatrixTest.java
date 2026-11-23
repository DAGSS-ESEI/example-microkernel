package es.uvigo.esei.dagss.microkernel.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatrixTest {

    @Test
    void copiaElArrayDeOrigen() {
        double[][] source = {{1, 2}, {3, 4}};
        Matrix matrix = Matrix.of(source);

        source[0][0] = 99;

        assertEquals(1, matrix.get(0, 0));
    }

    @Test
    void toArrayDevuelveUnaCopiaProfunda() {
        Matrix matrix = Matrix.of(new double[][]{{1, 2}, {3, 4}});

        double[][] first = matrix.toArray();
        double[][] second = matrix.toArray();

        assertNotSame(first, second);
        assertNotSame(first[0], second[0]);

        first[0][0] = 99;
        assertEquals(1, matrix.get(0, 0));
    }

    @Test
    void columnDevuelveUnaCopia() {
        Matrix matrix = Matrix.of(new double[][]{{1, 2}, {3, 4}});

        double[] column = matrix.column(1);
        column[0] = 99;

        assertArrayEquals(new double[]{2, 4}, matrix.column(1));
        assertEquals(2, matrix.get(0, 1));
    }

    @Test
    void dimensionesYAcceso() {
        Matrix matrix = Matrix.of(new double[][]{{1, 2, 3}, {4, 5, 6}});

        assertEquals(2, matrix.rows());
        assertEquals(3, matrix.columns());
        assertEquals(6, matrix.get(1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.get(2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.column(3));
    }

    @Test
    void rechazaMatricesNoValidas() {
        assertThrows(IllegalArgumentException.class, () -> Matrix.of(new double[][]{}));
        assertThrows(IllegalArgumentException.class, () -> Matrix.of(new double[][]{{}}));
        assertThrows(IllegalArgumentException.class,
                () -> Matrix.of(new double[][]{{1, 2}, {3}}));
        assertThrows(NullPointerException.class,
                () -> Matrix.of(new double[][]{{1, 2}, null}));
    }

    @Test
    void igualdadPorValores() {
        Matrix first = Matrix.of(new double[][]{{1, 2}, {3, 4}});
        Matrix second = Matrix.of(new double[][]{{1, 2}, {3, 4}});
        Matrix other = Matrix.of(new double[][]{{1, 2}, {3, 5}});

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotSame(first, second);
        org.junit.jupiter.api.Assertions.assertNotEquals(first, other);
    }
}
