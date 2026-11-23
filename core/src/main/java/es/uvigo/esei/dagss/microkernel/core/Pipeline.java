package es.uvigo.esei.dagss.microkernel.core;

import es.uvigo.esei.dagss.microkernel.core.api.Matrix;
import es.uvigo.esei.dagss.microkernel.core.api.Transformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Secuencia ordenada de plugins que se aplica sobre una matriz.
 *
 * <p>El pipeline no conoce las implementaciones: pide al registro los plugins por
 * id y los ejecuta en orden. Se puede componer en la línea de comandos, por
 * ejemplo {@code subtract-mean,standardize}.</p>
 */
public final class Pipeline {

    private final List<Transformation> steps;

    private Pipeline(List<Transformation> steps) {
        this.steps = List.copyOf(steps);
    }

    public static Pipeline of(PluginRegistry registry, String commaSeparatedIds) {
        Objects.requireNonNull(commaSeparatedIds, "ids");
        return of(registry, List.of(commaSeparatedIds.split(",")));
    }

    public static Pipeline of(PluginRegistry registry, List<String> ids) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(ids, "ids");

        List<Transformation> steps = new ArrayList<>();
        for (String raw : ids) {
            String id = raw == null ? "" : raw.trim();
            if (id.isEmpty()) {
                continue;
            }
            steps.add(registry.require(id));
        }

        if (steps.isEmpty()) {
            throw new IllegalArgumentException(
                    "El pipeline necesita al menos un plugin (por ejemplo: --run subtract-mean entrada.csv)");
        }
        return new Pipeline(steps);
    }

    /**
     * Aplica cada paso en orden. La matriz de entrada nunca se modifica.
     */
    public Matrix apply(Matrix input) {
        Objects.requireNonNull(input, "input");

        Matrix current = input;
        for (Transformation step : steps) {
            current = Objects.requireNonNull(
                    step.apply(current),
                    "El plugin '" + step.id() + "' devolvió null");
        }
        return current;
    }

    public List<Transformation> steps() {
        return steps;
    }

    /**
     * Representación legible del pipeline, por ejemplo {@code subtract-mean -> standardize}.
     */
    public String description() {
        return steps.stream()
                .map(Transformation::id)
                .collect(Collectors.joining(" -> "));
    }
}
