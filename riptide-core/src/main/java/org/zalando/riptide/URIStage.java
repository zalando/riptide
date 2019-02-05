package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.HttpMethod;

import java.net.URI;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public interface URIStage {

    QueryStage get(String uriTemplate, Object... urlVariables);
    QueryStage get(URI uri);
    QueryStage get();

    QueryStage head(String uriTemplate, Object... urlVariables);
    QueryStage head(URI uri);
    QueryStage head();

    QueryStage post(String uriTemplate, Object... urlVariables);
    QueryStage post(URI uri);
    QueryStage post();

    QueryStage put(String uriTemplate, Object... urlVariables);
    QueryStage put(URI uri);
    QueryStage put();

    QueryStage patch(String uriTemplate, Object... urlVariables);
    QueryStage patch(URI uri);
    QueryStage patch();

    QueryStage delete(String uriTemplate, Object... urlVariables);
    QueryStage delete(URI uri);
    QueryStage delete();

    QueryStage options(String uriTemplate, Object... urlVariables);
    QueryStage options(URI uri);
    QueryStage options();

    QueryStage trace(String uriTemplate, Object... urlVariables);
    QueryStage trace(URI uri);
    QueryStage trace();

    QueryStage execute(HttpMethod method, String uriTemplate, Object... uriVariables);
    QueryStage execute(HttpMethod method, URI uri);
    QueryStage execute(HttpMethod method);

}
