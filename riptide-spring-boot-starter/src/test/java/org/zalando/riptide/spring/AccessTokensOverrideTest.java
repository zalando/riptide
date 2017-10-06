package org.zalando.riptide.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.zalando.stups.tokens.AccessTokens;

import static org.junit.Assert.assertThat;
import static org.zalando.riptide.spring.Mocks.isMock;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DefaultTestConfiguration.class)
@Component
public final class AccessTokensOverrideTest {

    @Autowired
    private AccessTokens accessTokens;

    @Test
    public void shouldOverride() {
        assertThat(accessTokens, isMock());
    }

}
