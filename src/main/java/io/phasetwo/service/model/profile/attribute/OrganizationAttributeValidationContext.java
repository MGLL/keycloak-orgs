package io.phasetwo.service.model.profile.attribute;

import io.phasetwo.service.model.OrganizationModel;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.Validator;

import java.util.Map;

public class OrganizationAttributeValidationContext extends ValidationContext {

  /**
   * Easy way to cast me from {@link ValidationContext} in {@link Validator} implementation
   */
  public static OrganizationAttributeValidationContext from(ValidationContext vc) {
    return (OrganizationAttributeValidationContext) vc;
  }

  private OrganizationAttributeContext attributeContext;

  public OrganizationAttributeValidationContext(OrganizationAttributeContext attributeContext) {
    super(attributeContext.getSession());
    this.attributeContext = attributeContext;
  }

  public OrganizationAttributeContext getAttributeContext() {
    return attributeContext;
  }

  @Override
  public Map<String, Object> getAttributes() {
    Map<String, Object> attributes = super.getAttributes();

    attributes.put(OrganizationModel.class.getName(), getAttributeContext().getOrganization());

    return attributes;
  }
}
