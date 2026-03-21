package io.springlens.spi;

import java.util.List;

public record ControllerMappingMetadata(
        String controllerClassName,
        String handlerMethodName,
        List<String> httpMethods,
        List<String> paths
) {

    public ControllerMappingMetadata {
        httpMethods = httpMethods == null ? List.of() : List.copyOf(httpMethods);
        paths = paths == null ? List.of() : List.copyOf(paths);
    }
}
