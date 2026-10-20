package fr.efrei.finance.quoteserver.rpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

/**
 * Starts and stops the gRPC server together with the Spring context.
 * The server is stateless: every call is independent.
 */
@Component
public class GrpcServerLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerLifecycle.class);

    private final QuoteGrpcService quoteGrpcService;
    private final int port;
    private Server server;

    @Autowired
    public GrpcServerLifecycle(QuoteGrpcService quoteGrpcService,
                               @Value("${grpc.server.port:9090}") int port) {
        this.quoteGrpcService = quoteGrpcService;
        this.port = port;
    }

    @Override
    public void start() {
        try {
            HealthStatusManager health = new HealthStatusManager();
            server = ServerBuilder.forPort(port)
                    .addService(quoteGrpcService)
                    .addService(health.getHealthService())
                    .addService(ProtoReflectionService.newInstance())
                    .build()
                    .start();
            log.info("gRPC quote server started on port {}", port);

            // gRPC threads are daemon threads: keep the JVM alive while the server runs.
            Thread awaitThread = new Thread(this::awaitTermination, "grpc-await-termination");
            awaitThread.setDaemon(false);
            awaitThread.start();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot start gRPC server on port " + port, e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            log.info("Stopping gRPC quote server");
            server.shutdown();
            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
            } catch (InterruptedException e) {
                server.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void awaitTermination() {
        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return server != null && !server.isShutdown();
    }
}
