package io.phasetwo.service.model;

import io.phasetwo.service.model.profile.OrganizationProfileContext;
import io.phasetwo.service.model.profile.attribute.OrganizationAttributes;
import io.phasetwo.service.model.profile.exception.ValidationException;

/**
 * <p>An interface that serves an entry point for managing organizations and their attributes.
 *
 * <p>A {@code OrganizationProfile} provides methods for creating, and updating organizations.
 * All its operations are based the {@link OrganizationProfileContext}. By taking the context into
 * account, the state and behavior of {@link OrganizationProfile} instances depend on the context
 * they are associated with where creating, updating, and validating set of an organization is based
 * on the constraints associated with a given context.
 *
 * <p>The {@link OrganizationProfileContext} represents the different areas in Phasetwo where
 * organizations, and their
 * attributes are managed.
 *
 * <p>A {@code OrganizationProfile} instance can be obtained through the
 * {@link OrganizationProfileProvider}:
 *
 * <pre> {@code
 * // obtain the user profile provider
 * OrganizationProfileProvider provider = session.getProvider(OrganizationProfileProvider.class);
 * // create a instance for managing the user profile through the USER_API context
 * OrganizationProfile profile = provider.create(UPDATE_METADATA, organization);
 * }</pre>
 *
 * <p>The {@link OrganizationProfileProvider} provides different methods for creating
 * {@link OrganizationProfile} instances,
 * each one target for a specific scenario such as creating a new organization, updating an existing
 * one.
 *
 * @see OrganizationProfileContext
 * @see OrganizationProfileProvider
 */
public interface OrganizationProfile {

  /**
   * Validates the attributes associated with this instance.
   *
   * @throws ValidationException in case
   */
  void validate() throws ValidationException;

  /**
   * <p>Updates the {@link OrganizationModel} associated with this instance. If no
   * {@link OrganizationModel} is associated
   * with this instance, this operation has no effect.
   *
   * <p>Before updating the {@link OrganizationModel}, this method first checks whether the
   * {@link #validate()} method was
   * previously invoked. If not, the validation step is performed prior to updating the model.
   *
   * @param removeAttributes if attributes should be removed from the {@link OrganizationModel} if
   *                         they are not among the attributes associated with this instance.
   * @throws ValidationException in case of any validation error
   */
  void update(boolean removeAttributes) throws ValidationException;

  /**
   * <p>The same as {@link #update(boolean)}} but forcing the removal of attributes.
   *
   * @throws ValidationException in case of any validation error
   */
  default void update() throws ValidationException {
    update(true);
  }

  /**
   * Returns the attributes associated with this instance. Note that the attributes returned by this
   * method are not necessarily the same from the {@link OrganizationModel} as they are based on the
   * context this instance is based on.
   *
   * @return the attributes associated with this instance.
   */
  OrganizationAttributes getAttributes();
}
