package org.zalando.riptide;

import org.apiguardian.api.*;

import java.util.*;

import static org.apiguardian.api.API.Status.*;

/**
 * Plugins allow to modify {@link RequestExecution executions of requests} in order to inject specific behaviour.
 *
 * The chronological order of phases is
 *
 * <dl>
 *     <dt>Async</dt>
 *     <dd>Phases afterwards are executed asynchronously.</dd>
 *     <dt>Dispatch</dt>
 *     <dd>Performs the response routing onto the supplied routing tree upon receiving a response.</dd>
 *     <dt>Serialization</dt>
 *     <dd>Serialization of the request body.</dd>
 *     <dt>Network</dt>
 *     <dd>The actual network communication.</dd>
 * </dl>
 *
 * The nature of {@link RequestExecution request executions} allows plugins to inject behavior before, after and even
 * during the execution on a request.
 *
 * @see OriginalStackTracePlugin
 * @see <a href="https://docs.google.com/drawings/d/1zJC6533at3XzHvxsoUUqyyd4G8cZxmlJ9HFxfhBEcbg/edit">Plugin Phases</a>
 */
@API(status = MAINTAINED)
public interface Plugin {

    /**
     * The given execution will be executed in a different thread and therefore be properly asynchronous. This phase is
     * useful for plugins which either need to perform some task in the calling thread or (more commonly) may trigger
     * other asynchronous operations concurrently to the request.
     *
     * @param execution the execution that includes the thread switch as well as the dispatch, serialization and network phases
     * @return the new, potentially modified execution
     */
    default RequestExecution aroundAsync(final RequestExecution execution) {
        return execution;
    }

    /**
     * The given execution will have the response already being dispatched onto the given {@link Route}. Any
     * exceptions that were produced from the response will be observable in this stage.
     *
     * @param execution the execution that includes the dispatch, serialization and network phase
     * @return the new, potentially modified execution
     */
    default RequestExecution aroundDispatch(final RequestExecution execution) {
        return execution;
    }

    /**
     * The given execution will have the request body already serialized into bytes.
     *
     * @param execution the execution that includes serialization and network phase
     * @return the new, potentially modified execution
     */
    default RequestExecution aroundSerialization(final RequestExecution execution) {
        return execution;
    }

    /**
     * The given execution will include the pure network communication. Any exceptions that were the result of writing
     * to and reading from the TCP socket will be observable in this stage.
     *
     * @param execution the execution that includes the network communication
     * @return the new, potentially modified execution
     */
    default RequestExecution aroundNetwork(final RequestExecution execution) {
        return execution;
    }

    static Plugin composite(final Plugin... plugins) {
        return composite(Arrays.asList(plugins));
    }

    static Plugin composite(final List<Plugin> plugins) {
        return new CompositePlugin(plugins);
    }

}
