package io.phasetwo.service.model.profile.attribute.validator;

import com.google.auto.service.AutoService;
import io.phasetwo.service.model.OrganizationModel;
import io.phasetwo.service.model.profile.attribute.OrganizationAttributeContext;
import io.phasetwo.service.model.profile.attribute.OrganizationAttributeValidationContext;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.logging.Logger;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.validate.*;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.keycloak.common.util.ObjectUtil.isBlank;

@JBossLog
@AutoService(ValidatorFactory.class)
public class ReadOnlyOrganizationAttributeValidator implements SimpleValidator {

  private static final Logger logger = Logger.getLogger(
      ReadOnlyOrganizationAttributeValidator.class);

  public static final String ID = "org-readonly-attribute";

  public static final String CFG_PATTERN = "pattern";

  public static final String CFG_ERROR_MESSAGE = "update-read-only-attributes-rejected-message";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public ValidationContext validate(Object input, String inputHint, ValidationContext context,
      ValidatorConfig config) {
    OrganizationAttributeContext attributeContext = OrganizationAttributeValidationContext.from(
            context)
        .getAttributeContext();
    Map.Entry<String, List<String>> attribute = attributeContext.getAttribute();
    String key = attribute.getKey();
    logger.debugf("Validating key '%s'", key);

    Pattern pattern = (Pattern) config.get(CFG_PATTERN);
    logger.debugf("Pattern for validation '%s'", pattern);

    if (!pattern.matcher(key).find()) {
      return context;
    }

    @SuppressWarnings("unchecked")
    List<String> values = (List<String>) input;

    if (values == null) {
      return context;
    }

    OrganizationModel organization = attributeContext.getOrganization();
    String existingValue = organization == null ? null : organization.getFirstAttribute(key);

    String value = null;
    if (!values.isEmpty()) {
      value = values.get(0);
    }

    if (!isUnchanged(existingValue, value)) {
      logger.warnf(
          "Attempt to edit denied for attribute '%s' with pattern '%s' of organization '%s'", key,
          pattern,
          organization == null ? "new organization"
              : organization.getFirstAttribute(OrganizationModel.NAME));
      context.addError(new ValidationError(ID, key, CFG_ERROR_MESSAGE));
    }

    return context;
  }

  private boolean isUnchanged(String existingValue, String value) {
    if (existingValue == null && isBlank(value)) {
      // if attribute not set to the user and value is blank/null, then pass validation
      return true;
    }

    return ObjectUtil.isEqualOrBothNull(existingValue, value);
  }
}
