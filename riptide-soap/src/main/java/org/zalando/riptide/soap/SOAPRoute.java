package org.zalando.riptide.soap;

import org.zalando.fauxpas.*;
import org.zalando.riptide.*;

import javax.xml.soap.*;
import javax.xml.ws.soap.*;

import static org.springframework.http.HttpStatus.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.RoutingTree.*;

public final class SOAPRoute {

    private SOAPRoute() {

    }

    public static <T> Route soap(final Class<T> type, final ThrowingConsumer<T, ? extends Exception> consumer) {
        return dispatch(status(),
                on(OK).call(type, consumer),
                on(INTERNAL_SERVER_ERROR).call(SOAPFault.class, fault -> {
                    throw new SOAPFaultException(fault);
                }));
    }

}
