package org.zalando.riptide.micrometer.tag;

import org.apiguardian.api.API;

import java.util.ServiceLoader;

import static java.util.ServiceLoader.load;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.micrometer.tag.TagGenerator.composite;

/**
 * @see ServiceLoader
 */
@API(status = EXPERIMENTAL)
public final class ServiceLoaderTagGenerator extends ForwardingTagGenerator {

    public ServiceLoaderTagGenerator() {
        super(composite(load(TagGenerator.class)));
    }

}
