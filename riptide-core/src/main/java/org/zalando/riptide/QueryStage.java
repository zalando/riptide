package org.zalando.riptide;

import com.google.common.collect.*;
import org.apiguardian.api.*;

import java.util.*;

import static org.apiguardian.api.API.Status.*;

@API(status = STABLE)
public abstract class QueryStage extends HeaderStage {
    public abstract QueryStage queryParam(String name, String value);
    public abstract QueryStage queryParams(Multimap<String, String> params);
    public abstract QueryStage queryParams(Map<String, Collection<String>> params);
}
