package org.zalando.riptide.model;

import java.net.URI;

public record Problem(URI type, String title, int status, String detail) {
}
