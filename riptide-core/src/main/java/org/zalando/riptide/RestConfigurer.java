package org.zalando.riptide;

import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.DEPRECATED;

/**
 * @see HttpConfigurer
 */
@API(status = DEPRECATED, since = "2.5.0")
@Deprecated//(since = "2.5.0", forRemoval = true)
public interface RestConfigurer extends HttpConfigurer {

}
