package org.zalando.riptide;

import org.apiguardian.api.*;

import static org.apiguardian.api.API.Status.*;

@API(status = STABLE)
public abstract class AttributeStage extends QueryStage {
    public abstract <T> AttributeStage attribute(Attribute<T> attribute, T value);
}
