package io.phasetwo.service.model.profile.attribute;

import io.phasetwo.service.model.OrganizationModel;
import io.phasetwo.service.model.profile.OrganizationProfileContext;
import org.keycloak.models.KeycloakSession;

import java.util.List;
import java.util.Map;

public class OrganizationAttributeContext {

  private final KeycloakSession session;
  private final Map.Entry<String, List<String>> attribute;
  private final OrganizationModel organization;
  private final OrganizationAttributeMetadata metadata;
  private final OrganizationAttributes attributes;
  private OrganizationProfileContext context;

  public OrganizationAttributeContext(OrganizationProfileContext context, KeycloakSession session,
      Map.Entry<String, List<String>> attribute, OrganizationModel organization,
      OrganizationAttributeMetadata metadata,
      OrganizationAttributes attributes) {
    this.context = context;
    this.session = session;
    this.attribute = attribute;
    this.organization = organization;
    this.metadata = metadata;
    this.attributes = attributes;
  }

  public KeycloakSession getSession() {
    return session;
  }

  public Map.Entry<String, List<String>> getAttribute() {
    return attribute;
  }

  public OrganizationModel getOrganization() {
    return organization;
  }

  public OrganizationProfileContext getContext() {
    return context;
  }

  public OrganizationAttributeMetadata getMetadata() {
    return metadata;
  }

  public OrganizationAttributes getAttributes() {
    return attributes;
  }
}
