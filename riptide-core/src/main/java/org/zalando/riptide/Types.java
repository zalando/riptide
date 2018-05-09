package org.zalando.riptide;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import org.apiguardian.api.API;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public final class Types {

    private Types() {

    }

    public static <T> TypeToken<List<T>> listOf(final Class<T> entityType) {
        return listOf(TypeToken.of(entityType));
    }

    @SuppressWarnings("serial")
    public static <T> TypeToken<List<T>> listOf(final TypeToken<T> entityType) {
        final TypeToken<List<T>> listType = new TypeToken<List<T>>() {
            // nothing to implement!
        };

        final TypeParameter<T> elementType = new TypeParameter<T>() {
            // nothing to implement!
        };

        return listType.where(elementType, entityType);
    }

    public static <T> TypeToken<ResponseEntity<T>> responseEntityOf(final Class<T> entityType) {
        return responseEntityOf(TypeToken.of(entityType));
    }

    public static <T> TypeToken<ResponseEntity<T>> responseEntityOf(final TypeToken<T> entityType) {
        final TypeToken<ResponseEntity<T>> responseEntityType = new TypeToken<ResponseEntity<T>>() {
            // nothing to implement!
        };

        final TypeParameter<T> elementType = new TypeParameter<T>() {
            // nothing to implement!
        };

        return responseEntityType.where(elementType, entityType);
    }

}
