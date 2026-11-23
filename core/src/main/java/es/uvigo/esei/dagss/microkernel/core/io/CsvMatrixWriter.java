package es.uvigo.esei.dagss.microkernel.core.io;

import es.uvigo.esei.dagss.microkernel.core.api.Matrix;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Escritor de matrices en texto plano (CSV con coma).
 *
 * <p>El formato es determinista (siempre cuatro decimales y {@link Locale#ROOT}),
 * para que la salida se pueda comparar en un test de integración.</p>
 */
public final class CsvMatrixWriter {

    private static final String FORMAT = "%.4f";

    private CsvMatrixWriter() {
    }

    public static void write(Matrix matrix, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            write(matrix, writer);
        }
    }

    public static void write(Matrix matrix, PrintStream out) {
        for (int row = 0; row < matrix.rows(); row++) {
            out.println(formatRow(matrix, row));
        }
    }

    private static void write(Matrix matrix, Writer writer) throws IOException {
        for (int row = 0; row < matrix.rows(); row++) {
            writer.write(formatRow(matrix, row));
            writer.write(System.lineSeparator());
        }
    }

    private static String formatRow(Matrix matrix, int row) {
        StringBuilder result = new StringBuilder();
        for (int column = 0; column < matrix.columns(); column++) {
            if (column > 0) {
                result.append(',');
            }
            result.append(String.format(Locale.ROOT, FORMAT, matrix.get(row, column)));
        }
        return result.toString();
    }
}
