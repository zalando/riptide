package org.zalando.riptide.autoconfigure;

import org.apiguardian.api.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.test.autoconfigure.web.client.*;
import org.springframework.core.annotation.*;

import java.lang.annotation.*;

import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@RestClientTest
@AutoConfigureMockRestServiceServer(enabled = false) // will be registered via RiptideTestAutoConfiguration
@ImportAutoConfiguration({
        RiptideTestAutoConfiguration.class,
        RiptideAutoConfiguration.class
})
public @interface RiptideClientTest {

    /**
     * Specifies the components to test. May be left blank if components will be manually
     * imported or created directly.
     *
     * @return the components to test
     */
    @AliasFor(annotation = RestClientTest.class, attribute = "value")
    Class<?>[] value() default {};

}
