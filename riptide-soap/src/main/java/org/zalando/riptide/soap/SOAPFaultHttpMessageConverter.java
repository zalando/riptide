package org.zalando.riptide.soap;

import org.springframework.http.*;
import org.springframework.http.converter.*;

import javax.annotation.*;
import javax.xml.soap.*;
import java.io.*;

import static java.lang.ThreadLocal.*;
import static javax.xml.soap.SOAPConstants.*;
import static org.springframework.http.MediaType.*;
import static org.zalando.fauxpas.FauxPas.*;

public final class SOAPFaultHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    private final ThreadLocal<MessageFactory> messageFactory;

    public SOAPFaultHttpMessageConverter() {
        this(SOAP_1_1_PROTOCOL);
    }

    public SOAPFaultHttpMessageConverter(final String protocol) {
        super(TEXT_XML);
        this.messageFactory = withInitial(throwingSupplier(() -> MessageFactory.newInstance(protocol)));
    }

    @Override
    protected boolean supports(final Class<?> type) {
        return type == SOAPFault.class;
    }

    @Override
    public boolean canWrite(final Class<?> type, @Nullable final MediaType mediaType) {
        return false;
    }

    @Nonnull
    @Override
    protected Object readInternal(final Class<?> type, final HttpInputMessage message)
            throws IOException, HttpMessageNotReadableException {

        try {
            final SOAPMessage soapMessage = messageFactory.get().createMessage(null, message.getBody());
            return soapMessage.getSOAPBody().getFault();
        } catch (final SOAPException e) {
            throw new HttpMessageNotReadableException(e.getMessage(), e, message);
        }
    }

    @Override
    protected void writeInternal(final Object o, final HttpOutputMessage message) {
        throw new UnsupportedOperationException();
    }

}
