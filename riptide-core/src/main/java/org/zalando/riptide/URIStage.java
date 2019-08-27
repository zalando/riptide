package org.zalando.riptide;

import org.apiguardian.api.*;
import org.springframework.http.*;

import java.net.*;

import static org.apiguardian.api.API.Status.*;

@API(status = STABLE)
public interface URIStage {

    AttributeStage get(String uriTemplate, Object... urlVariables);
    AttributeStage get(URI uri);
    AttributeStage get();

    AttributeStage head(String uriTemplate, Object... urlVariables);
    AttributeStage head(URI uri);
    AttributeStage head();

    AttributeStage post(String uriTemplate, Object... urlVariables);
    AttributeStage post(URI uri);
    AttributeStage post();

    AttributeStage put(String uriTemplate, Object... urlVariables);
    AttributeStage put(URI uri);
    AttributeStage put();

    AttributeStage patch(String uriTemplate, Object... urlVariables);
    AttributeStage patch(URI uri);
    AttributeStage patch();

    AttributeStage delete(String uriTemplate, Object... urlVariables);
    AttributeStage delete(URI uri);
    AttributeStage delete();

    AttributeStage options(String uriTemplate, Object... urlVariables);
    AttributeStage options(URI uri);
    AttributeStage options();

    AttributeStage trace(String uriTemplate, Object... urlVariables);
    AttributeStage trace(URI uri);
    AttributeStage trace();

    AttributeStage execute(HttpMethod method, String uriTemplate, Object... uriVariables);
    AttributeStage execute(HttpMethod method, URI uri);
    AttributeStage execute(HttpMethod method);

}
