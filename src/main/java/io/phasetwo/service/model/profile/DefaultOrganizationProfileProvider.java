package io.phasetwo.service.model.profile;

import io.phasetwo.service.model.OrganizationModel;
import io.phasetwo.service.model.OrganizationProfile;
import io.phasetwo.service.model.OrganizationProfileProvider;
import io.phasetwo.service.model.profile.attribute.OrganizationAttributes;
import io.phasetwo.service.model.profile.attribute.DefaultOrganizationAttributes;
import org.keycloak.models.KeycloakSession;

import java.util.Map;

public class DefaultOrganizationProfileProvider implements OrganizationProfileProvider {

  private final KeycloakSession session;
  private final Map<OrganizationProfileContext, OrganizationProfileMetadata> contextualMetadataRegistry;

  public DefaultOrganizationProfileProvider(KeycloakSession session,
      DefaultOrganizationProfileProviderFactory factory) {
    this.session = session;
    this.contextualMetadataRegistry = factory.getContextualMetadataRegistry();
  }

  protected OrganizationAttributes createAttributes(OrganizationProfileContext context,
      Map<String, ?> attributes,
      OrganizationModel organization, OrganizationProfileMetadata metadata) {
    return new DefaultOrganizationAttributes(context, attributes, organization, metadata, session);
  }

  @Override
  public OrganizationProfile create(OrganizationProfileContext context, Map<String, ?> attributes,
      OrganizationModel organization) {
    return createOrganizationProfile(context, attributes, organization);
  }

  private OrganizationProfile createOrganizationProfile(OrganizationProfileContext context,
      Map<String, ?> attributes,
      OrganizationModel organization) {
    OrganizationProfileMetadata metadata = contextualMetadataRegistry.get(context);

    if (metadata == null) {
      // some contexts (and their metadata) are available enabled when the corresponding feature is enabled
      throw new RuntimeException("No metadata is bound to the " + context + " context");
    }

    OrganizationAttributes profileAttributes = createAttributes(context, attributes, organization,
        metadata);
    return new DefaultOrganizationProfile(profileAttributes, organization);
  }

  @Override
  public void close() {
  }
}
