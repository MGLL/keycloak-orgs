package io.phasetwo.service.model.profile.exception;

import jakarta.ws.rs.core.Response;
import org.keycloak.validate.ValidationError;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ValidationException extends RuntimeException implements Consumer<ValidationError> {

  private final Map<String, List<ValidationException.Error>> errors = new HashMap<>();

  public List<ValidationException.Error> getErrors() {
    return errors.values().stream().reduce(new ArrayList<>(), (l, r) -> {
      l.addAll(r);
      return l;
    }, (l, r) -> l);
  }

  public boolean hasError(String... types) {
    if (types.length == 0) {
      return !errors.isEmpty();
    }

    for (String type : types) {
      if (errors.containsKey(type)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if there are validation errors related to the attribute with the given {@code name}.
   *
   * @param name
   * @return
   */
  public boolean isAttributeOnError(String... name) {
    if (name.length == 0) {
      return !errors.isEmpty();
    }

    List<String> names = Arrays.asList(name);

    return errors.values().stream().flatMap(Collection::stream)
        .anyMatch(error -> names.contains(error.getAttribute()));
  }

  @Override
  public void accept(ValidationError error) {
    addError(error);
  }

  void addError(ValidationError error) {
    List<ValidationException.Error> errors = this.errors.computeIfAbsent(error.getMessage(),
        (k) -> new ArrayList<>());
    errors.add(new ValidationException.Error(error));
  }

  @Override
  public String toString() {
    return "ValidationException [errors=" + errors + "]";
  }

  @Override
  public String getMessage() {
    return toString();
  }

  public Response.Status getStatusCode() {
    for (Map.Entry<String, List<ValidationException.Error>> entry : errors.entrySet()) {
      for (ValidationException.Error error : entry.getValue()) {
        if (!Response.Status.BAD_REQUEST.equals(error.getStatusCode())) {
          return error.getStatusCode();
        }
      }
    }
    return Response.Status.BAD_REQUEST;
  }

  public static class Error implements Serializable {

    private final ValidationError error;

    public Error(ValidationError error) {
      this.error = error;
    }

    public String getAttribute() {
      return error.getInputHint();
    }

    public String getMessage() {
      return error.getMessage();
    }

    public Object[] getMessageParameters() {
      return error.getInputHintWithMessageParameters();
    }

    @Override
    public String toString() {
      return "Error [error=" + error + "]";
    }

    public String getFormattedMessage(BiFunction<String, Object[], String> messageFormatter) {
      return messageFormatter.apply(getMessage(), getMessageParameters());
    }

    public Response.Status getStatusCode() {
      return error.getStatusCode();
    }
  }
}
