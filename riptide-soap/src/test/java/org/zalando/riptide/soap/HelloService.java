package org.zalando.riptide.soap;

import javax.jws.*;

@WebService
public class HelloService {

    @WebMethod
    public String sayHello(@WebParam(name = "name") final String name) {
        if ("Error".equals(name)) {
            throw new IllegalArgumentException("Error is not supported");
        }

        return "Hello, " + name + ".";
    }

}
