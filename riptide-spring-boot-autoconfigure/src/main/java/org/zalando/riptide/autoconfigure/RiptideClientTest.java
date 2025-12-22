package org.zalando.riptide.autoconfigure;

import org.apiguardian.api.API;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import  org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import  org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

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
