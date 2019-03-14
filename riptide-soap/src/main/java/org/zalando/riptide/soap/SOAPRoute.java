package org.zalando.riptide.soap;

import org.zalando.fauxpas.ThrowingConsumer;
import org.zalando.riptide.Route;

import javax.xml.soap.SOAPFault;
import javax.xml.ws.soap.SOAPFaultException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.RoutingTree.dispatch;

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
