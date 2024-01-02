package io.phasetwo.service.model.profile.attribute;

import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.Validator;
import org.keycloak.validate.ValidatorConfig;
import org.keycloak.validate.Validators;

import java.util.Map;

public class OrganizationAttributeValidatorMetadata {

  private final String validatorId;
  private final ValidatorConfig validatorConfig;

  public OrganizationAttributeValidatorMetadata(String validatorId) {
    this.validatorId = validatorId;
    this.validatorConfig = ValidatorConfig.configFromMap(null);
  }

  public OrganizationAttributeValidatorMetadata(String validatorId,
      ValidatorConfig validatorConfig) {
    this.validatorId = validatorId;
    this.validatorConfig = validatorConfig;
  }

  public String getValidatorId() {
    return validatorId;
  }

  /**
   * Get validator configuration as map.
   *
   * @return never null
   */
  public Map<String, Object> getValidatorConfig() {
    return validatorConfig.asMap();
  }

  /**
   * Run validation for given AttributeContext.
   *
   * @param context to validate
   * @return context containing errors if any found
   */
  public ValidationContext validate(OrganizationAttributeContext context) {
    Validator validator = Validators.validator(context.getSession(), validatorId);
    if (validator == null) {
      throw new RuntimeException(
          "No validator with id " + validatorId
              + " found to validate OrganizationProfile attribute "
              + context.getMetadata().getName() + " in realm " + context.getSession().getContext()
              .getRealm()
              .getName());
    }

    return validator.validate(context.getAttribute().getValue(), context.getMetadata().getName(),
        new OrganizationAttributeValidationContext(context), validatorConfig);
  }
}
