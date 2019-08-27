package org.zalando.riptide;

import org.apiguardian.api.*;
import org.zalando.riptide.RequestArguments.*;

import javax.annotation.*;

import static org.apiguardian.api.API.Status.*;

@API(status = STABLE)
public abstract class BodyStage extends DispatchStage {
    public abstract DispatchStage body(@Nullable Entity entity);
    public abstract <T> DispatchStage body(@Nullable T body);
}
