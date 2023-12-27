package io.phasetwo.service.model;

import io.phasetwo.service.model.profile.OrganizationProfileContext;
import org.keycloak.provider.Provider;

import java.util.Map;

/**
 * <p>The provider responsible for creating {@link OrganizationProfile} instances.
 *
 * @see OrganizationProfile
 */
public interface OrganizationProfileProvider extends Provider {

  /**
   * <p>Creates a new {@link OrganizationProfile} instance for a given {@code context} and
   * {@code attributes} for update
   * purposes.
   *
   * <p>Instances created from this method are going to run validations and updates based on the
   * given {@code organization}.
   * This might be useful when updating an existing organization.
   *
   * @param context      the context
   * @param attributes   the attributes to associate with the instance returned from this method
   * @param organization the organization to eventually update with the given {@code attributes}
   * @return the organization profile instance
   */
  OrganizationProfile create(OrganizationProfileContext context, Map<String, ?> attributes,
      OrganizationModel organization);
}
