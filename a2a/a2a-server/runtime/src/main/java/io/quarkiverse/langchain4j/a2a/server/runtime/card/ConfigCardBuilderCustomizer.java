package io.quarkiverse.langchain4j.a2a.server.runtime.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;

import io.quarkiverse.langchain4j.a2a.server.runtime.config.A2AServerRuntimeConfiguration;
import io.quarkiverse.langchain4j.a2a.server.runtime.config.A2AServerRuntimeConfiguration.TenantConfiguration;

/**
 * Fills a card with what the configuration says about the agent: its name, description and version, and the URL its
 * interfaces are reached at. Every agent card is filled this way before anything else contributes to it.
 */
@ApplicationScoped
public class ConfigCardBuilderCustomizer {

    private final A2AServerRuntimeConfiguration configuration;
    private final DetectedTransports detectedTransports;
    private final ServerAddress serverAddress;

    public ConfigCardBuilderCustomizer(A2AServerRuntimeConfiguration configuration,
            DetectedTransports detectedTransports, ServerAddress serverAddress) {
        this.configuration = configuration;
        this.detectedTransports = detectedTransports;
        this.serverAddress = serverAddress;
    }

    /**
     * @param tenant the tenant the agent is exposed under, or {@code null} for the agent served to requests that name
     *        none
     */
    public void customize(AgentCard.Builder cardBuilder, String tenant) {
        TenantConfiguration tenantConfiguration = tenant != null ? configuration.tenants().get(tenant) : null;
        populateSupportedInterfaces(cardBuilder, tenant, tenantConfiguration);
        cardBuilder
                .name(firstPresent(tenantConfiguration != null ? tenantConfiguration.name() : Optional.empty(),
                        configuration.name()))
                .description(firstPresent(
                        tenantConfiguration != null ? tenantConfiguration.description() : Optional.empty(),
                        configuration.description().orElse("")))
                .version(firstPresent(tenantConfiguration != null ? tenantConfiguration.version() : Optional.empty(),
                        configuration.version()));
    }

    private static String firstPresent(Optional<String> tenantValue, String serverValue) {
        return tenantValue.isPresent() ? tenantValue.get() : serverValue;
    }

    /**
     * Each interface names the tenant too, which is how a client learns what to put in the {@code tenant} field of
     * the requests it sends to the agent.
     */
    private void populateSupportedInterfaces(AgentCard.Builder cardBuilder, String tenant,
            TenantConfiguration tenantConfiguration) {
        String url = determineUrl(tenantConfiguration);
        List<AgentInterface> interfaces = new ArrayList<>();
        for (String protocolBinding : detectedTransports.getProtocolBindings()) {
            interfaces.add(new AgentInterface(protocolBinding, url, tenant));
        }
        cardBuilder.supportedInterfaces(interfaces);
    }

    /**
     * A URL from the configuration names the endpoint as clients reach it, so it is used as it is, the tenant's first.
     * The detected one is the server's address followed by the HTTP root path, which is where the A2A routes are
     * served.
     */
    private String determineUrl(TenantConfiguration tenantConfiguration) {
        if (tenantConfiguration != null && tenantConfiguration.url().isPresent()) {
            return tenantConfiguration.url().get();
        }
        if (configuration.url().isPresent()) {
            return configuration.url().get();
        }
        return serverAddress.url() + detectedTransports.getRootPath();
    }
}
