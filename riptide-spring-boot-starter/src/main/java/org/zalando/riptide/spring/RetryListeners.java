package org.zalando.riptide.spring;

import org.zalando.riptide.failsafe.RetryListener;

final class RetryListeners {

    private RetryListeners() {

    }

    public static RetryListener getDefault() {
        return RetryListener.DEFAULT;
    }

}
