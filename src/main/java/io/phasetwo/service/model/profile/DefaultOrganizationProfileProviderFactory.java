package io.phasetwo.service.model.profile;

import com.google.auto.service.AutoService;
import io.phasetwo.service.model.OrganizationModel;
import io.phasetwo.service.model.OrganizationProfileProvider;
import io.phasetwo.service.model.OrganizationProfileProviderFactory;
import io.phasetwo.service.model.profile.attribute.OrganizationAttributeContext;
import io.phasetwo.service.model.profile.attribute.OrganizationAttributeValidatorMetadata;
import io.phasetwo.service.model.profile.attribute.validator.ReadOnlyOrganizationAttributeValidator;
import io.phasetwo.service.model.profile.attribute.validator.ReadOnlyOrganizationNameValidator;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.validate.ValidatorConfig;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.phasetwo.service.model.profile.attribute.DefaultOrganizationAttributes.READ_ONLY_ATTRIBUTE_KEY;

@JBossLog
@AutoService(OrganizationProfileProviderFactory.class)
public class DefaultOrganizationProfileProviderFactory implements
    OrganizationProfileProviderFactory {

  public static final String PROVIDER_ID = "organization-profile";
  private static final Logger logger = Logger.getLogger(OrganizationProfileProviderFactory.class);
  public static final String CONFIG_ADMIN_READ_ONLY_ATTRIBUTES = "admin-read-only-attributes";
  public static final String CONFIG_READ_ONLY_ATTRIBUTES = "read-only-attributes";
  private static final String[] DEFAULT_ADMIN_READ_ONLY_ATTRIBUTES = {"admin_org_ro_*"};
  private static final String[] DEFAULT_READ_ONLY_ATTRIBUTES = {"org_ro_*"};
  private static final Pattern readOnlyAttributesPattern = getRegexPatternString(
      DEFAULT_READ_ONLY_ATTRIBUTES);
  private static final Pattern adminReadOnlyAttributesPattern = getRegexPatternString(
      DEFAULT_ADMIN_READ_ONLY_ATTRIBUTES);

  private final Map<OrganizationProfileContext, OrganizationProfileMetadata> contextualMetadataRegistry = new HashMap<>();

  public static Pattern getRegexPatternString(String[] builtinReadOnlyAttributes) {
    if (builtinReadOnlyAttributes != null) {
      List<String> readOnlyAttributes = new ArrayList<>(Arrays.asList(builtinReadOnlyAttributes));

      String regexStr = readOnlyAttributes.stream()
          .map(configAttrName -> configAttrName.endsWith("*")
              ? "^" + Pattern.quote(configAttrName.substring(0, configAttrName.length() - 1))
              + ".*$"
              : "^" + Pattern.quote(configAttrName) + "$").collect(Collectors.joining("|"));
      regexStr = "(?i:" + regexStr + ")";

      return Pattern.compile(regexStr);
    }

    return null;
  }

  @Override
  public void init(Config.Scope config) {
    contextualMetadataRegistry.clear();
    Pattern pattern = getRegexPatternString(config.getArray(CONFIG_READ_ONLY_ATTRIBUTES));
    logger.debugf("Pattern from spi config: '%s'", pattern);

    OrganizationAttributeValidatorMetadata readOnlyValidator = null;

    if (pattern != null) {
      readOnlyValidator = createReadOnlyAttributeValidator(pattern);
    }

    // for standard org user
    addContextualProfileMetadata(
        createOrgUserResourceValidation(config, OrganizationProfileContext.UPDATE_PROFILE,
            readOnlyValidator));
    // for admin on new endpoint /{orgId}/manage
    addContextualProfileMetadata(createAdminManagementResourceValidation(config,
        OrganizationProfileContext.UPDATE_METADATA));
  }

  private OrganizationAttributeValidatorMetadata createReadOnlyAttributeValidator(Pattern pattern) {
    return new OrganizationAttributeValidatorMetadata(ReadOnlyOrganizationAttributeValidator.ID,
        ValidatorConfig.builder()
            .config(ReadOnlyOrganizationAttributeValidator.CFG_PATTERN, pattern).build());
  }

  private OrganizationProfileMetadata createOrgUserResourceValidation(Config.Scope config,
      OrganizationProfileContext context,
      OrganizationAttributeValidatorMetadata readOnlyValidator) {
    // We assume that a standard user shouldn't be able to modify what a platform admin can't
    OrganizationProfileMetadata metadata = createAdminManagementResourceValidation(config, context);

    metadata.addAttribute(OrganizationModel.NAME,
        DefaultOrganizationProfileProviderFactory::writeName,
        List.of(new OrganizationAttributeValidatorMetadata(ReadOnlyOrganizationNameValidator.ID)));

    List<OrganizationAttributeValidatorMetadata> readonlyValidators = new ArrayList<>();
    readonlyValidators.add(createReadOnlyAttributeValidator(readOnlyAttributesPattern));

    if (readOnlyValidator != null) {
      readonlyValidators.add(readOnlyValidator);
    }

    if (!metadata.getAttribute(READ_ONLY_ATTRIBUTE_KEY).isEmpty()) {
      // append read only validator to existing admin validator
      metadata.getAttribute(READ_ONLY_ATTRIBUTE_KEY).get(0).addValidators(readonlyValidators);
    } else {
      metadata.addAttribute(READ_ONLY_ATTRIBUTE_KEY, readonlyValidators);
    }

    return metadata;
  }

  private OrganizationProfileMetadata createAdminManagementResourceValidation(Config.Scope config,
      OrganizationProfileContext context) {
    Pattern p = getRegexPatternString(config.getArray(CONFIG_ADMIN_READ_ONLY_ATTRIBUTES));
    OrganizationProfileMetadata metadata = new OrganizationProfileMetadata(context);

    List<OrganizationAttributeValidatorMetadata> readonlyValidators = new ArrayList<>();
    readonlyValidators.add(createReadOnlyAttributeValidator(adminReadOnlyAttributesPattern));

    if (p != null) {
      readonlyValidators.add(createReadOnlyAttributeValidator(p));
    }

    metadata.addAttribute(READ_ONLY_ATTRIBUTE_KEY, readonlyValidators);
    return metadata;
  }

  private void addContextualProfileMetadata(OrganizationProfileMetadata metadata) {
    if (contextualMetadataRegistry.putIfAbsent(metadata.getContext(), metadata) != null) {
      throw new IllegalStateException(
          "Multiple profile metadata found for context " + metadata.getContext());
    }
  }

  private static boolean writeName(OrganizationAttributeContext c) {
    if (OrganizationProfileContext.UPDATE_PROFILE.equals(c.getContext())) {
      return false;
    }
    return true;
  }

  @Override
  public OrganizationProfileProvider create(KeycloakSession session) {
    return new DefaultOrganizationProfileProvider(session, this);
  }

  @Override
  public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
  }

  @Override
  public void close() {

  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  protected Map<OrganizationProfileContext, OrganizationProfileMetadata> getContextualMetadataRegistry() {
    return contextualMetadataRegistry;
  }
}
