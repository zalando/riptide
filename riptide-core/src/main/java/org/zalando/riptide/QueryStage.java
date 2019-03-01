package org.zalando.riptide;

import com.google.common.collect.Multimap;
import org.apiguardian.api.API;

import java.util.Collection;
import java.util.Map;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public abstract class QueryStage extends HeaderStage {
    public abstract QueryStage queryParam(String name, String value);
    public abstract QueryStage queryParams(Multimap<String, String> params);
    public abstract QueryStage queryParams(Map<String, Collection<String>> params);
}
