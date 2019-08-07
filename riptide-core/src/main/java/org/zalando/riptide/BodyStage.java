package org.zalando.riptide;

import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments.Entity;

import javax.annotation.Nullable;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public abstract class BodyStage extends DispatchStage {
    public abstract DispatchStage body(@Nullable Entity entity);
    public abstract <T> DispatchStage body(@Nullable T body);
}
