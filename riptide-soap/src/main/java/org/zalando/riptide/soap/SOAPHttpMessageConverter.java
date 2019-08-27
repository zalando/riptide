package org.zalando.riptide.soap;

import com.google.common.cache.*;
import org.springframework.http.*;
import org.springframework.http.converter.*;
import org.w3c.dom.*;

import javax.annotation.*;
import javax.xml.bind.*;
import javax.xml.bind.annotation.*;
import javax.xml.parsers.*;
import javax.xml.soap.*;
import java.io.*;

import static java.lang.ThreadLocal.*;
import static javax.xml.soap.SOAPConstants.*;
import static org.springframework.http.MediaType.*;
import static org.zalando.fauxpas.FauxPas.*;

public final class SOAPHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    private final LoadingCache<Class<?>, JAXBContext> contexts = CacheBuilder.newBuilder()
            .build(new CacheLoader<Class<?>, JAXBContext>() {
                @Override
                public JAXBContext load(final Class<?> type) throws Exception {
                    return JAXBContext.newInstance(type);
                }
            });

    private final ThreadLocal<MessageFactory> messageFactory;
    private final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

    public SOAPHttpMessageConverter() {
        this(SOAP_1_1_PROTOCOL);
    }

    public SOAPHttpMessageConverter(final String protocol) {
        super(TEXT_XML);
        this.messageFactory = withInitial(throwingSupplier(() -> MessageFactory.newInstance(protocol)));
    }

    @Override
    protected boolean supports(final Class<?> type) {
        return type.isAnnotationPresent(XmlRootElement.class) || type.isAnnotationPresent(XmlType.class);
    }

    @Nonnull
    @Override
    protected Object readInternal(final Class<?> type,
            final HttpInputMessage message) throws IOException, HttpMessageNotReadableException {

        try {
            final SOAPMessage soapMessage = messageFactory.get().createMessage(null, message.getBody());
            final Document document = soapMessage.getSOAPBody().extractContentAsDocument();
            final Unmarshaller unmarshaller = contexts.getUnchecked(type).createUnmarshaller();
            return unmarshaller.unmarshal(document);
        } catch (final SOAPException | JAXBException e) {
            throw new HttpMessageNotReadableException(e.getMessage(), e, message);
        }
    }

    @Override
    protected void writeInternal(final Object value,
            final HttpOutputMessage message) throws IOException, HttpMessageNotWritableException {

        try {
            final Document document = builderFactory.newDocumentBuilder().newDocument();
            final Marshaller marshaller = contexts.getUnchecked(value.getClass()).createMarshaller();
            marshaller.marshal(value, document);
            final SOAPMessage soapMessage = messageFactory.get().createMessage();
            soapMessage.getSOAPBody().addDocument(document);

            soapMessage.writeTo(message.getBody());
        } catch (final SOAPException | JAXBException | ParserConfigurationException e) {
            throw new HttpMessageNotWritableException(e.getMessage(), e);
        }
    }

}
