package io.phasetwo.service.model.profile;

import io.phasetwo.service.model.profile.attribute.OrganizationAttributeContext;
import io.phasetwo.service.model.profile.attribute.OrganizationAttributeMetadata;
import io.phasetwo.service.model.profile.attribute.OrganizationAttributeValidatorMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OrganizationProfileMetadata {

  private final OrganizationProfileContext context;
  private List<OrganizationAttributeMetadata> attributes;

  public OrganizationProfileMetadata(OrganizationProfileContext context) {
    this.context = context;
  }

  public List<OrganizationAttributeMetadata> getAttributes() {
    return attributes;
  }

  public void addAttributes(List<OrganizationAttributeMetadata> metadata) {
    if (attributes == null) {
      attributes = new ArrayList<>();
    }
    attributes.addAll(metadata);
  }

  public OrganizationAttributeMetadata addAttribute(OrganizationAttributeMetadata metadata) {
    addAttributes(Arrays.asList(metadata));
    return metadata;
  }

  public OrganizationAttributeMetadata addAttribute(String name,
      List<OrganizationAttributeValidatorMetadata> validators) {
    return addAttribute(new OrganizationAttributeMetadata(name).addValidators(validators));
  }

  public OrganizationAttributeMetadata addAttribute(String name,
      Predicate<OrganizationAttributeContext> writeAllowed,
      List<OrganizationAttributeValidatorMetadata> validators) {
    return addAttribute(
        new OrganizationAttributeMetadata(name, writeAllowed).addValidators(validators));
  }

  public List<OrganizationAttributeMetadata> getAttribute(String name) {
      if (attributes == null) {
          return Collections.emptyList();
      }
    return attributes.stream().filter((c) -> name.equals(c.getName())).collect(Collectors.toList());

  }

  public OrganizationProfileContext getContext() {
    return context;
  }
}
