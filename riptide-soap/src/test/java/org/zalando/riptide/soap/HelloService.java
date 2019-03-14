package org.zalando.riptide.soap;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

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
