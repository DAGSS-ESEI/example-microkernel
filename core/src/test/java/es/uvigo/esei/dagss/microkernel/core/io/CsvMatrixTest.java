package es.uvigo.esei.dagss.microkernel.core.io;

import es.uvigo.esei.dagss.microkernel.core.api.Matrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvMatrixTest {

    @Test
    void leeComasPuntoYComaYTabuladores() {
        Matrix matrix = CsvMatrixReader.parse(List.of(
                "# comentario",
                "   ",
                "1, 2;3\t4",
                "5,6;7\t8"));

        assertEquals(2, matrix.rows());
        assertEquals(4, matrix.columns());
        assertEquals(1, matrix.get(0, 0));
        assertEquals(8, matrix.get(1, 3));
    }

    @Test
    void ignoraElBomDelPrimerCaracter() {
        Matrix matrix = CsvMatrixReader.parse(List.of("\uFEFF1,2", "3,4"));

        assertEquals(1, matrix.get(0, 0));
        assertEquals(2, matrix.get(0, 1));
    }

    @Test
    void fallaSiLaMatrizNoEsRectangular() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> CsvMatrixReader.parse(List.of("1,2", "3")));

        assertTrue(error.getMessage().contains("rectangular"));
    }

    @Test
    void fallaConValorNoNumericoEIndicaLaPosicion() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> CsvMatrixReader.parse(List.of("1,2", "3,hola")));

        assertTrue(error.getMessage().contains("línea 2"));
        assertTrue(error.getMessage().contains("columna 2"));
        assertTrue(error.getMessage().contains("hola"));
    }

    @Test
    void fallaSiNoHayFilas() {
        assertThrows(IllegalArgumentException.class,
                () -> CsvMatrixReader.parse(List.of("# solo comentarios", "")));
    }

    @Test
    void escribeYVuelveALeer(@TempDir Path directory) throws IOException {
        Matrix matrix = Matrix.of(new double[][]{{1, 2.5}, {-3, 0}});
        Path file = directory.resolve("salida.csv");

        CsvMatrixWriter.write(matrix, file);
        Matrix read = CsvMatrixReader.read(file);

        assertEquals("1.0000,2.5000", Files.readAllLines(file, StandardCharsets.UTF_8).get(0));
        assertEquals("1.0000", String.format(java.util.Locale.ROOT, "%.4f", read.get(0, 0)));
        assertEquals(2.5, read.get(0, 1));
        assertEquals(-3, read.get(1, 0));
    }
}
