package es.uvigo.esei.dagss.microkernel.core;

import es.uvigo.esei.dagss.microkernel.core.api.Matrix;
import es.uvigo.esei.dagss.microkernel.core.api.PluginNotFoundException;
import es.uvigo.esei.dagss.microkernel.core.api.Transformation;
import es.uvigo.esei.dagss.microkernel.core.io.CsvMatrixReader;
import es.uvigo.esei.dagss.microkernel.core.io.CsvMatrixWriter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.List;

/**
 * Punto de entrada del sistema. Es la "cáscara" del microkernel: descubre
 * plugins, los lista y ejecuta un pipeline. No contiene ninguna lógica de
 * transformación.
 */
public final class Main {

    private static final String CONTRACT = Transformation.class.getName();
    private static final List<String> VERBOSE_FLAGS = List.of("-v", "--verbose");

    private Main() {
    }

    public static void main(String[] args) {
        try {
            System.exit(run(args));
        } catch (PluginNotFoundException | IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
        } catch (IOException e) {
            System.err.println("Error de E/S: " + e.getMessage());
            System.exit(1);
        }
    }

    static int run(String[] args) throws IOException {
        if (args.length == 0) {
            printUsage(System.out);
            return 0;
        }

        return switch (args[0]) {
            case "-h", "--help" -> {
                printUsage(System.out);
                yield 0;
            }
            case "--list" -> list(args.length > 1 && VERBOSE_FLAGS.contains(args[1]));
            case "--run" -> runPipeline(args);
            default -> {
                System.err.println("Opción desconocida: " + args[0]);
                System.err.println();
                printUsage(System.err);
                yield 2;
            }
        };
    }

    private static int runPipeline(String[] args) throws IOException {
        if (args.length < 3 || args.length > 4) {
            System.err.println("Uso: --run <id1,id2,...> <entrada.csv> [salida.csv]");
            return 2;
        }

        PluginRegistry registry = new PluginRegistry();
        Pipeline pipeline = Pipeline.of(registry, args[1]);

        Matrix input = CsvMatrixReader.read(Path.of(args[2]));
        Matrix output = pipeline.apply(input);

        if (args.length == 4) {
            CsvMatrixWriter.write(output, Path.of(args[3]));
            System.out.println("Pipeline : " + pipeline.description());
            System.out.println("Entrada  : " + args[2]
                    + " (" + input.rows() + "x" + input.columns() + ")");
            System.out.println("Salida   : " + args[3]
                    + " (" + output.rows() + "x" + output.columns() + ")");
        } else {
            CsvMatrixWriter.write(output, System.out);
        }
        return 0;
    }

    private static int list(boolean verbose) {
        PluginRegistry registry = new PluginRegistry();

        System.out.println("Plugins descubiertos: " + registry.size());
        System.out.println();

        if (registry.size() == 0) {
            System.out.println("No se ha encontrado ningún plugin en el classpath.");
            System.out.println("Comprueba que los JAR están en plugins/*/target/*.jar y que");
            System.out.println("declaran su implementación en META-INF/services/" + CONTRACT);
            return 0;
        }

        System.out.printf("%-18s %-32s %s%n",
                "ID", "NOMBRE", verbose ? "ORIGEN" : "DESCRIPCIÓN");
        System.out.printf("%-18s %-32s %s%n",
                "--", "------", verbose ? "------" : "-----------");

        for (Transformation plugin : registry.all()) {
            String thirdColumn = verbose
                    ? originOf(plugin)
                    : plugin.description();
            System.out.printf("%-18s %-32s %s%n",
                    plugin.id(), truncate(plugin.name(), 30), thirdColumn);
        }

        System.out.println();
        System.out.println(verbose
                ? "Cada plugin se carga desde un JAR distinto: el core no los conoce al compilar."
                : "Usa --list -v para ver de qué JAR se carga cada plugin.");
        return 0;
    }

    /**
     * Ruta (solo el nombre del fichero) del JAR desde el que se cargó la clase.
     * Es la prueba visual de que cada plugin es un artefacto independiente.
     */
    private static String originOf(Transformation plugin) {
        try {
            CodeSource codeSource = plugin.getClass().getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return "(desconocido)";
            }
            return Path.of(codeSource.getLocation().toURI()).getFileName().toString();
        } catch (Exception e) {
            return "(desconocido)";
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength
                ? text
                : text.substring(0, maxLength - 1) + "…";
    }

    private static void printUsage(PrintStream out) {
        out.println("Uso:");
        out.println("  run.sh --list [-v]                             Lista los plugins descubiertos");
        out.println("  run.sh --run <ids> <entrada.csv> [salida.csv]  Aplica un pipeline de plugins");
        out.println();
        out.println("<ids> es una lista de identificadores separados por comas, en orden de ejecución.");
        out.println();
        out.println("Ejemplos:");
        out.println("  ./run.sh --list");
        out.println("  ./run.sh --list -v");
        out.println("  ./run.sh --run subtract-mean,standardize datos/ejemplo.csv");
        out.println("  ./run.sh --run subtract-mean,standardize datos/ejemplo.csv /tmp/salida.csv");
    }
}
