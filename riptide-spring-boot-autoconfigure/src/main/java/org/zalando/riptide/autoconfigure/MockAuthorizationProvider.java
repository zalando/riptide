package org.zalando.riptide.autoconfigure;

import org.zalando.riptide.auth.AuthorizationProvider;

final class MockAuthorizationProvider implements AuthorizationProvider {

    @Override
    public String get() {
        return "Bearer fake-test-token";
    }

}
