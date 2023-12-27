package io.phasetwo.service.model.profile.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OrganizationAttributeMetadata {

  public static final Predicate<OrganizationAttributeContext> ALWAYS_TRUE = context -> true;
  public static final Predicate<OrganizationAttributeContext> ALWAYS_FALSE = context -> false;

  private final String attributeName;

  private final List<Predicate<OrganizationAttributeContext>> writeAllowed = new ArrayList<>();

  private List<OrganizationAttributeValidatorMetadata> validators;

  public OrganizationAttributeMetadata(String attributeName) {
    this(attributeName, ALWAYS_TRUE);
  }

  public OrganizationAttributeMetadata(String attributeName,
      Predicate<OrganizationAttributeContext> writeAllowed) {
    this.attributeName = attributeName;
    this.writeAllowed.add(writeAllowed);
  }

  OrganizationAttributeMetadata(String attributeName,
      List<Predicate<OrganizationAttributeContext>> writeAllowed) {
    this.attributeName = attributeName;
    this.writeAllowed.addAll(writeAllowed);
  }

  public String getName() {
    return attributeName;
  }

  public boolean isReadOnly(OrganizationAttributeContext context) {
    return !canEdit(context);
  }

  public boolean canEdit(OrganizationAttributeContext context) {
    return allConditionsMet(writeAllowed, context);
  }

  private boolean allConditionsMet(List<Predicate<OrganizationAttributeContext>> predicates,
      OrganizationAttributeContext context) {
    return predicates.stream().allMatch(p -> p.test(context));
  }

  public List<OrganizationAttributeValidatorMetadata> getValidators() {
    return validators;
  }

  public OrganizationAttributeMetadata addValidators(
      List<OrganizationAttributeValidatorMetadata> validators) {
    if (this.validators == null) {
      this.validators = new ArrayList<>();
    }

    this.validators.removeIf(validators::contains);
    this.validators.addAll(
        validators.stream().filter(Objects::nonNull).collect(Collectors.toList()));

    return this;
  }

  @Override
  public OrganizationAttributeMetadata clone() {
    OrganizationAttributeMetadata cloned = new OrganizationAttributeMetadata(attributeName,
        writeAllowed);
    // we clone validators list to allow adding or removing validators. Validators
    // itself are not cloned as we do not expect them to be reconfigured.
    if (validators != null) {
      cloned.addValidators(validators);
    }
    return cloned;
  }
}
