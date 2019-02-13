package org.zalando.riptide.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.google.common.base.Preconditions.checkElementIndex;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A special {@link AuthorizationProvider authorization provider} that built for Zalando's Platform IAM which provides
 * OAuth2 tokens as files in a mounted directory. Each token is represented as a set of two individual files:
 *
 * <dl>
 *     <dt>{name}-token-type</dt>
 *     <dd>Contains the token type, e.g. Bearer.</dd>
 *
 *     <dt>{name}-token-secret</dt>
 *     <dd>Contains the actual secret, e.g. a Json Web Token (JWT)</dd>
 * </dl>
 *
 * @see <a href="https://kubernetes-on-aws.readthedocs.io/en/latest/user-guide/zalando-iam.html">Zalando Platform IAM Integration</a>
 */
public final class PlatformCredentialsAuthorizationProvider implements AuthorizationProvider {

    private final String name;
    private final Path directory;

    public PlatformCredentialsAuthorizationProvider(final String name) {
        this(Paths.get("/meta/credentials"), name);
    }

    public PlatformCredentialsAuthorizationProvider(final Path directory, final String name) {
        this.name = name;
        this.directory = directory;
    }

    @Override
    public String get() throws IOException {
        return read("type") + " " + read("secret");
    }

    private String read(final String suffix) throws IOException {
        final Path path = directory.resolve(name + "-token-" + suffix);
        final List<String> lines = Files.readAllLines(path, UTF_8);
        checkElementIndex(0, lines.size(), "Expected at least one line in " + path);
        return lines.get(0);
    }

}
