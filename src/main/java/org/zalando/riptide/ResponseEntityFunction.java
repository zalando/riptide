package org.zalando.riptide;

import org.springframework.http.ResponseEntity;

import java.util.function.Function;

public interface ResponseEntityFunction<F, T> extends Function<ResponseEntity<F>, T> {

}
