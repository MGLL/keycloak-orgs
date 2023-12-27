package io.phasetwo.service.model.profile;

import io.phasetwo.service.model.OrganizationModel;
import io.phasetwo.service.model.OrganizationProfile;
import io.phasetwo.service.model.profile.attribute.OrganizationAttributes;
import io.phasetwo.service.model.profile.exception.ValidationException;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.utils.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultOrganizationProfile implements OrganizationProfile {

  private final OrganizationAttributes attributes;
  private boolean validated;
  private final OrganizationModel organization;

  public DefaultOrganizationProfile(OrganizationAttributes attributes,
      OrganizationModel organization) {
    this.attributes = attributes;
    this.organization = organization;
  }

  @Override
  public void validate() throws ValidationException {
    ValidationException validationException = new ValidationException();

    for (String attributeName : attributes.nameSet()) {
      this.attributes.validate(attributeName, validationException);
    }

    if (validationException.hasError()) {
      throw validationException;
    }

    validated = true;
  }

  @Override
  public void update(boolean removeAttributes) throws ValidationException {
    if (!validated) {
      validate();
    }

    updateInternal(organization, removeAttributes);
  }

  private void updateInternal(OrganizationModel organization, boolean removeAttributes) {
    if (organization == null) {
      throw new RuntimeException("No organization model provided for persisting changes");
    }

    try {
      Map<String, List<String>> writable = new HashMap<>(attributes.getWritable());

      for (Map.Entry<String, List<String>> attribute : writable.entrySet()) {
        String name = attribute.getKey();
        List<String> currentValue = organization.getAttributesStream(name).filter(Objects::nonNull)
            .collect(Collectors.toList());
        List<String> updatedValue = attribute.getValue().stream().filter(StringUtil::isNotBlank)
            .collect(Collectors.toList());

        if (CollectionUtil.collectionEquals(currentValue, updatedValue)) {
          continue;
        }

        boolean ignoreEmptyValue = !removeAttributes && updatedValue.isEmpty();

        // to avoid writing NAME into attribute, can be done in a separate method if multiple
        if (OrganizationModel.NAME.equals(name)) {
          continue;
        }

        organization.setAttribute(name, updatedValue);
      }

      if (removeAttributes) {
        Set<String> attrsToRemove = new HashSet<>(organization.getAttributes().keySet());
        attrsToRemove.removeAll(attributes.nameSet());

        for (String name : attrsToRemove) {
          if (attributes.isReadOnly(name)) {
            continue;
          }

          if (!OrganizationModel.NAME.equals(name)) {
            organization.removeAttribute(name);
          }
        }
      }
    } catch (Exception cause) {
      throw new RuntimeException("Unexpected error when persisting organization profile", cause);
    }
  }

  @Override
  public OrganizationAttributes getAttributes() {
    return attributes;
  }
}
