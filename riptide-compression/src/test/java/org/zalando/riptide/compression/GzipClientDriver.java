package org.zalando.riptide.compression;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.DefaultRequestMatcher;
import com.github.restdriver.clientdriver.exception.ClientDriverSetupException;
import com.github.restdriver.clientdriver.jetty.ClientDriverJettyHandler;
import com.github.restdriver.clientdriver.jetty.DefaultClientDriverJettyHandler;
import com.google.gag.annotation.remark.Hack;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;

@Hack
final class GzipClientDriver extends ClientDriver {

    private int port;

    static ClientDriver create() {
        return new GzipClientDriver(new DefaultClientDriverJettyHandler(new DefaultRequestMatcher()));
    }

    private GzipClientDriver(ClientDriverJettyHandler handler) {
        super(handler);
    }

    @Override
    protected Server createAndStartJetty(int port) {
        final Server jetty = new Server();
        jetty.setHandler(handler);
        final GzipHandler gzip = new GzipHandler();
        gzip.setInflateBufferSize(1024);
        jetty.insertHandler(gzip);
        final ServerConnector connector = createConnector(jetty, port);
        jetty.addConnector(connector);
        try {
            jetty.start();
        } catch (Exception e) {
            throw new ClientDriverSetupException("Error starting jetty on port " + port, e);
        }
        this.port = connector.getLocalPort();
        return jetty;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Override
    protected void replaceConnector(ServerConnector newConnector, Server jetty) {
        throw new UnsupportedOperationException();
    }
}
