package es.uvigo.esei.dagss.microkernel.core.io;

import es.uvigo.esei.dagss.microkernel.core.api.Matrix;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Lector de matrices en texto plano.
 *
 * <p>Formato: una fila por línea, valores separados por coma, punto y coma o
 * tabulador. Se ignoran las líneas vacías y las que empiezan por {@code #}.</p>
 */
public final class CsvMatrixReader {

    private static final Pattern SEPARATOR = Pattern.compile("[,;\\t]");

    private CsvMatrixReader() {
    }

    public static Matrix read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        return parse(lines);
    }

    static Matrix parse(List<String> lines) {
        List<double[]> rows = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (index == 0) {
                // Algunos editores añaden un BOM al principio del fichero.
                line = line.replace("\uFEFF", "");
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            String[] cells = SEPARATOR.split(trimmed, -1);
            double[] row = new double[cells.length];
            for (int column = 0; column < cells.length; column++) {
                String cell = cells[column].trim();
                try {
                    row[column] = Double.parseDouble(cell);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Valor no numérico en la línea " + (index + 1)
                                    + ", columna " + (column + 1) + ": '" + cell + "'", e);
                }
            }
            rows.add(row);
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("El fichero no contiene ninguna fila de datos");
        }

        int expectedColumns = rows.get(0).length;
        for (int row = 1; row < rows.size(); row++) {
            if (rows.get(row).length != expectedColumns) {
                throw new IllegalArgumentException(
                        "La matriz no es rectangular: la primera fila tiene "
                                + expectedColumns + " columnas y la fila de datos "
                                + (row + 1) + " tiene " + rows.get(row).length);
            }
        }

        return Matrix.of(rows.toArray(double[][]::new));
    }
}
