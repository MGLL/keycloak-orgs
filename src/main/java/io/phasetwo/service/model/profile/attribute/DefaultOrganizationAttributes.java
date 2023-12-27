package io.phasetwo.service.model.profile.attribute;

import io.phasetwo.service.model.OrganizationModel;
import io.phasetwo.service.model.profile.OrganizationProfileContext;
import io.phasetwo.service.model.profile.OrganizationProfileMetadata;
import org.jboss.logging.Logger;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.StringUtil;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

public class DefaultOrganizationAttributes extends HashMap<String, List<String>> implements
    OrganizationAttributes {

  private static final Logger logger = Logger.getLogger(DefaultOrganizationAttributes.class);

  public static final String READ_ONLY_ATTRIBUTE_KEY = "org.read.only";

  protected final OrganizationProfileContext context;
  protected final KeycloakSession session;
  private final Map<String, OrganizationAttributeMetadata> metadataByAttribute;
  protected final OrganizationModel organization;

  public DefaultOrganizationAttributes(OrganizationProfileContext context,
      Map<String, ?> attributes,
      OrganizationModel organization, OrganizationProfileMetadata profileMetadata,
      KeycloakSession session) {
    this.context = context;
    this.organization = organization;
    this.session = session;
    this.metadataByAttribute = configureMetadata(profileMetadata.getAttributes());
    putAll(Collections.unmodifiableMap(normalizeAttributes(attributes)));
  }

  private OrganizationAttributeContext createAttributeContext(Entry<String, List<String>> attribute,
      OrganizationAttributeMetadata metadata) {
    return new OrganizationAttributeContext(context, session, attribute, organization, metadata,
        this);
  }

  private OrganizationAttributeContext createAttributeContext(String attributeName,
      OrganizationAttributeMetadata metadata) {
    return new OrganizationAttributeContext(context, session, createAttribute(attributeName),
        organization, metadata, this);
  }

  protected OrganizationAttributeContext createAttributeContext(
      OrganizationAttributeMetadata metadata) {
    return createAttributeContext(createAttribute(metadata.getName()), metadata);
  }

  private Map<String, OrganizationAttributeMetadata> configureMetadata(
      List<OrganizationAttributeMetadata> attributes) {
    Map<String, OrganizationAttributeMetadata> metadatas = new HashMap<>();

    for (OrganizationAttributeMetadata metadata : attributes) {
      metadatas.put(metadata.getName(), metadata);
    }

    return metadatas;
  }

  private SimpleImmutableEntry<String, List<String>> createAttribute(String name) {
    return new SimpleImmutableEntry<String, List<String>>(name, null) {
      @Override
      public List<String> getValue() {
        List<String> values = get(name);

        if (values == null) {
          return EMPTY_VALUE;
        }

        return values;
      }
    };
  }

  private Map<String, List<String>> normalizeAttributes(Map<String, ?> attributes) {
    Map<String, List<String>> newAttributes = new HashMap<>();

    if (attributes != null) {
      for (Map.Entry<String, ?> entry : attributes.entrySet()) {
        String name = entry.getKey();
        List<String> values = normalizeAttributeValues(entry.getValue());
        newAttributes.put(name, Collections.unmodifiableList(values));
      }
    }

    for (String attributeName : metadataByAttribute.keySet()) {
      newAttributes.put(attributeName,
          normalizeAttributeValues(
              organization.getAttributes().getOrDefault(attributeName, EMPTY_VALUE)));
    }

    if (organization != null) {
      if (newAttributes.getOrDefault(OrganizationModel.NAME, emptyList()).isEmpty() && isReadOnly(
          OrganizationModel.NAME)) {
        newAttributes.put(OrganizationModel.NAME,
            normalizeAttributeValues(attributes.get(OrganizationModel.NAME)));
      }
    }

    return newAttributes;
  }

  protected List<String> normalizeAttributeValues(Object value) {
    List<String> values;

    if (value instanceof String) {
      values = Collections.singletonList((String) value);
    } else {
      values = (List<String>) value;
    }

    Stream<String> valuesStream = Optional.ofNullable(values).orElse(EMPTY_VALUE).stream()
        .filter(Objects::nonNull);
    return valuesStream.collect(Collectors.toList());
  }

  protected boolean isReadOnlyFromMetadata(String attributeName) {
    OrganizationAttributeMetadata attributeMetadata = metadataByAttribute.get(attributeName);

    if (attributeMetadata == null) {
      return false;
    }

    return attributeMetadata.isReadOnly(createAttributeContext(attributeMetadata));
  }

  protected boolean isReadOnlyInternalAttribute(String attributeName) {
    // read-only can be configured through the provider so we try to validate global validations
    OrganizationAttributeMetadata readonlyMetadata = metadataByAttribute.get(
        READ_ONLY_ATTRIBUTE_KEY);

    if (readonlyMetadata == null) {
      return false;
    }

    OrganizationAttributeContext attributeContext = createAttributeContext(attributeName,
        readonlyMetadata);

    for (OrganizationAttributeValidatorMetadata validator : readonlyMetadata.getValidators()) {
      ValidationContext vc = validator.validate(attributeContext);
      if (!vc.isValid()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public List<String> get(String name) {
    return getOrDefault(name, EMPTY_VALUE);
  }

  @Override
  public boolean isReadOnly(String name) {
    if (OrganizationModel.NAME.equals(name)) {
      return true;
    }

    if (isReadOnlyFromMetadata(name) || isReadOnlyInternalAttribute(name)) {
      return true;
    }

    // we assume that if there is no metadata, it's writable
    return getMetadata(name) != null;
  }

  @Override
  public boolean validate(String name, Consumer<ValidationError>... listeners) {
    Entry<String, List<String>> attribute = createAttribute(name);
    List<OrganizationAttributeMetadata> metadatas = new ArrayList<>();

    metadatas.addAll(Optional.ofNullable(this.metadataByAttribute.get(attribute.getKey()))
        .map(Collections::singletonList)
        .orElse(emptyList()));
    metadatas.addAll(
        Optional.ofNullable(this.metadataByAttribute.get(READ_ONLY_ATTRIBUTE_KEY))
            .map(Collections::singletonList)
            .orElse(emptyList()));

    Boolean result = null;

    for (OrganizationAttributeMetadata metadata : metadatas) {
      OrganizationAttributeContext attributeContext = createAttributeContext(attribute, metadata);

      for (OrganizationAttributeValidatorMetadata validator : metadata.getValidators()) {
        ValidationContext vc = validator.validate(attributeContext);

        if (vc.isValid()) {
          continue;
        }

        if (organization != null && metadata.isReadOnly(attributeContext)) {
          List<String> value = organization.getAttributesStream(name).filter(StringUtil::isNotBlank)
              .collect(Collectors.toList());
          List<String> newValue = attribute.getValue().stream().filter(StringUtil::isNotBlank)
              .collect(Collectors.toList());
          if (CollectionUtil.collectionEquals(value, newValue)) {
            logger.debugf(
                "Organization '%s' attribute '%s' has previous validation errors %s but is read-only in context %s.",
                organization.getName(), name, vc.getErrors(), attributeContext.getContext());
            continue;
          }
        }

        if (result == null) {
          result = false;
        }

        if (listeners != null) {
          for (ValidationError error : vc.getErrors()) {
            for (Consumer<ValidationError> consumer : listeners) {
              consumer.accept(error);
            }
          }
        }
      }
    }

    return result == null;
  }

  @Override
  public boolean contains(String name) {
    return containsKey(name);
  }

  @Override
  public Set<String> nameSet() {
    return keySet();
  }

  @Override
  public Map<String, List<String>> getWritable() {
    Map<String, List<String>> attributes = new HashMap<>(this);

    for (String name : nameSet()) {
      OrganizationAttributeMetadata metadata = getMetadata(name);

      // we assume that if there is no metadata, it's writable
      if (metadata != null && !metadata.canEdit(createAttributeContext(metadata))) {
        attributes.remove(name);
      }
    }

    return attributes;
  }

  @Override
  public Map<String, List<String>> toMap() {
    return Collections.unmodifiableMap(this);
  }

  @Override
  public OrganizationAttributeMetadata getMetadata(String name) {
    return Optional.ofNullable(metadataByAttribute.get(name))
        .map(OrganizationAttributeMetadata::clone).orElse(null);
  }
}
