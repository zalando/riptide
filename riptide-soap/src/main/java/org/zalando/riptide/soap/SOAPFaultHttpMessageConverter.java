package org.zalando.riptide.soap;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.soap.SOAPMessage;
import java.io.IOException;

import static java.lang.ThreadLocal.withInitial;
import static jakarta.xml.soap.SOAPConstants.SOAP_1_1_PROTOCOL;
import static org.springframework.http.MediaType.TEXT_XML;
import static org.zalando.fauxpas.FauxPas.throwingSupplier;

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
