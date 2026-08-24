package com.payment.transaction.config;

import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hosts the message broker settlement/reporting/notification-service
 * consume from (see TransactionProcessingService.publishCompletionEvent()
 * and each of those three services' TransactionCompletedListener). Spring
 * Boot's own "embedded Artemis" mode (spring.artemis.mode: embedded) only
 * opens an in-VM connector reachable from this process alone, which isn't
 * enough here - three other JVMs need to connect to it - so this builds the
 * broker directly with Artemis's own embedding API instead, adding a real
 * TCP acceptor. transaction-service then talks to it as a plain "native"
 * JMS client too (see application.yml), exactly like the other three
 * services do, rather than getting special in-process treatment.
 *
 * persistence/security are both off deliberately: this is a local learning
 * demo with nothing worth persisting across restarts or authenticating -
 * a real deployment runs a real broker cluster with both turned on.
 */
@Configuration
public class EmbeddedBrokerConfig {

    @Bean(destroyMethod = "stop")
    public EmbeddedActiveMQ embeddedActiveMQ() throws Exception {
        String dataDir = System.getProperty("java.io.tmpdir") + "/payment-system-artemis";

        var config = new ConfigurationImpl();
        config.setName("transaction-service-broker");
        config.setPersistenceEnabled(false);
        config.setSecurityEnabled(false);
        config.setJournalDirectory(dataDir + "/journal");
        config.setBindingsDirectory(dataDir + "/bindings");
        config.setLargeMessagesDirectory(dataDir + "/large-messages");
        config.setPagingDirectory(dataDir + "/paging");
        config.addAcceptorConfiguration("netty-acceptor", "tcp://0.0.0.0:61616");

        EmbeddedActiveMQ embedded = new EmbeddedActiveMQ();
        embedded.setConfiguration(config);
        embedded.start();
        return embedded;
    }
}
