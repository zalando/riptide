package org.zalando.riptide.spring.testing;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.TestPropertySource;
import org.zalando.riptide.spring.RiptideAutoConfiguration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@RestClientTest
@AutoConfigureMockRestServiceServer(enabled = false) // see RiptideClientAutoConfiguration
@TestPropertySource(properties = {
        "riptide.mocked: true"
})
@ImportAutoConfiguration({
        RiptideMockRestServiceServerAutoConfiguration.class,
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
