package org.zalando.riptide.model;

import java.net.URI;

public record Error(String message, URI path) {
}
