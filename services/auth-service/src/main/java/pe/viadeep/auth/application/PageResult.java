package pe.viadeep.auth.application;

import java.util.List;
import java.util.function.Function;

/** Página de resultados, con la misma forma que los listados del servicio de incidencias. */
public record PageResult<T>(List<T> items, int page, int size, long totalItems) {

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, size, totalItems);
    }
}
