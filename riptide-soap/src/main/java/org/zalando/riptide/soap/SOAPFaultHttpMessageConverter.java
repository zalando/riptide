package org.zalando.riptide.soap;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;

import static java.lang.ThreadLocal.withInitial;
import static javax.xml.soap.SOAPConstants.SOAP_1_1_PROTOCOL;
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
            // TODO should ideally pass message when running against Spring 5
            throw new HttpMessageNotReadableException(e.getMessage(), e);
        }
    }

    @Override
    protected void writeInternal(final Object o, final HttpOutputMessage message) {
        throw new UnsupportedOperationException();
    }

}
