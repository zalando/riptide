package org.zalando.riptide;

import org.springframework.http.ResponseEntity;

import java.util.function.Consumer;

public interface ResponseEntityConsumer<T> extends Consumer<ResponseEntity<T>> {

}
