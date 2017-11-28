package org.zalando.riptide.spring.testing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.zalando.riptide.Http;

import static org.springframework.http.HttpStatus.OK;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.PassRoute.pass;

@Component
public class TestService {

    private final Http http;

    @Autowired
    public TestService(@Qualifier("example") final Http http) {
        this.http = http;
    }

    void callViaHttp() {
        http.get("/bar").dispatch(status(), on(OK).call(pass())).join();
    }

}
