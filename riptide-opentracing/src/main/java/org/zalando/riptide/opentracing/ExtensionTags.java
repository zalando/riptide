package org.zalando.riptide.opentracing;

import io.opentracing.tag.BooleanTag;
import io.opentracing.tag.StringTag;
import io.opentracing.tag.Tag;

public final class ExtensionTags {

    public static final Tag<String> HTTP_PATH = new StringTag("http.path");

    /**
     * When present on a client span, they represent a span that wraps a retried RPC. If missing no interpretation can
     * be made. An explicit value of false would explicitly mean it is a first RPC attempt.
     */
    public static final Tag<Boolean> RETRY = new BooleanTag("retry");

    /**
     * The tag should contain an alias or name that allows users to identify the logical location (infrastructure account)
     * where the operation took place. This can be the AWS account, or any other cloud provider account.
     * E.g., {@code account=aws:zalando-zmon}, {@code account=gcp:zalando-foobar}
     */
    public static final Tag<String> ACCOUNT = new StringTag("account");

    /**
     * The tag should contain some information that allows users to associate the physical location of the system where
     * the operation took place (i.e. the datacenter).
     * E.g., {@code zone=aws:eu-central-1a}, {@code zone=gcp:europe-west3-b}, {@code zone=dc:gth}.
     */
    public static final Tag<String> ZONE = new StringTag("zone");

    /**
     * Oauth2 client ids have a certain cardinality but are well known or possible to get using different means. It
     * could be helpful for server spans to identify the client making the call. E.g., {@code client_id=cognac}
     */
    public static final Tag<String> CLIENT_ID = new StringTag("client_id");

    /**
     * The flow_id tag should contain the request flow ID, typically found in the ingress requests HTTP header X-Flow-ID.
     *
     * <a href="https://opensource.zalando.com/restful-api-guidelines/#233">X-Flow-ID Guidelines</a>
     */
    public static final Tag<String> FLOW_ID = new StringTag("flow_id");

    /**
     * The tag should contain the artifact version of the running application generating the spans.
     * This is, usually, the docker image tag.
     */
    public static final Tag<String> ARTIFACT_VERSION = new StringTag("artifact_version");

    /**
     * The tag should contain the unique identifier of the deployment that resulted in the operation of the running
     * application generating the spans. This is, usually, the STUPS stack version or the Kubernetes deployment id.
     * A deployment is the combination of a given artifact_version and the environment, usually its configuration.
     */
    public static final Tag<String> DEPLOYMENT_ID = new StringTag("deployment_id");

    private ExtensionTags() {

    }

}
