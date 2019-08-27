package org.zalando.riptide.autoconfigure.testing;

import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.zalando.riptide.*;

import static org.springframework.http.HttpStatus.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;

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
