package fr.efrei.finance.portfolio.config;

import fr.efrei.finance.quote.grpc.QuoteServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the gRPC channel and stub as Spring beans, so they can be injected (IoC).
 */
@Configuration
public class GrpcClientConfig {

    private static final Logger log = LoggerFactory.getLogger(GrpcClientConfig.class);

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel quoteServiceChannel(@Value("${quote-service.host}") String host,
                                              @Value("${quote-service.port}") int port) {
        log.info("gRPC client targeting quote server at {}:{}", host, port);
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()   // no TLS between internal services (demo)
                .build();
    }

    @Bean
    public QuoteServiceGrpc.QuoteServiceBlockingStub quoteServiceStub(ManagedChannel quoteServiceChannel) {
        return QuoteServiceGrpc.newBlockingStub(quoteServiceChannel);
    }
}
