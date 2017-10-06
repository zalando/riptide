package org.zalando.riptide.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Component
public class AsyncListenableTaskExecutorTest {

    @Configuration
    @Import(DefaultTestConfiguration.class)
    public static class TestConfiguration {

    }

    @Autowired
    @Qualifier("example")
    private AsyncListenableTaskExecutor unit;

    @Test
    public void shouldAutowire() {
        assertThat(unit.getClass(), is(ConcurrentTaskExecutor.class));
    }
}
