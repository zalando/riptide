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

import static javax.xml.soap.SOAPConstants.SOAP_1_1_PROTOCOL;
import static org.springframework.http.MediaType.TEXT_XML;

public final class SOAPFaultHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    private final String protocol;

    public SOAPFaultHttpMessageConverter() {
        this(SOAP_1_1_PROTOCOL);
    }

    public SOAPFaultHttpMessageConverter(final String protocol) {
        super(TEXT_XML);
        this.protocol = protocol;
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
            final MessageFactory factory = MessageFactory.newInstance(protocol);
            final SOAPMessage soapMessage = factory.createMessage(null, message.getBody());
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
