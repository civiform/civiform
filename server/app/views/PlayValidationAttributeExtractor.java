package views;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Max;
import play.data.validation.Constraints.MaxLength;
import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.MinLength;
import play.data.validation.Constraints.Pattern;
import play.data.validation.Constraints.Required;

/** Extracts Play validation constraints from a model */
public final class PlayValidationAttributeExtractor {
  /**
   * Reflect upon the supplied modelClass to get a map of any Play data validation constraint
   * annotations for the fieldName.
   */
  public static Map<String, String> getHtmlAttributes(Class<?> modelClass, String fieldName) {
    Map<String, String> attributes = new HashMap<>();

    try {
      Field field = modelClass.getDeclaredField(fieldName);
      Annotation[] annotations = field.getAnnotations();

      for (Annotation annotation : annotations) {
        if (annotation instanceof Required req) {
          attributes.put("required", "required");
          if (!req.message().isEmpty()) {
            attributes.put("data-required-message", req.message());
          }
        } else if (annotation instanceof MinLength minLength) {
          attributes.put("minlength", String.valueOf(minLength.value()));
          if (!minLength.message().isEmpty()) {
            attributes.put("data-minlength-message", minLength.message());
          }
        } else if (annotation instanceof MaxLength maxLength) {
          attributes.put("maxlength", String.valueOf(maxLength.value()));
          if (!maxLength.message().isEmpty()) {
            attributes.put("data-maxlength-message", maxLength.message());
          }
        } else if (annotation instanceof Email email) {
          if (!email.message().isEmpty()) {
            attributes.put("data-email-message", email.message());
          }
        } else if (annotation instanceof Pattern pattern) {
          attributes.put("pattern", pattern.value());
          if (!pattern.message().isEmpty()) {
            attributes.put("data-pattern-message", pattern.message());
          }
        } else if (annotation instanceof Min min) {
          attributes.put("min", String.valueOf(min.value()));
          if (!min.message().isEmpty()) {
            attributes.put("data-min-message", min.message());
          }
        } else if (annotation instanceof Max max) {
          attributes.put("max", String.valueOf(max.value()));
          if (!max.message().isEmpty()) {
            attributes.put("data-max-message", max.message());
          }
        }
      }

    } catch (NoSuchFieldException e) {
      // Field not found, return empty map
    }

    return attributes;
  }

  /**
   * Reflect upon the supplied modelClass to get a string of any Play data validation constraint
   * annotations for the fieldName.
   */
  public static String getAttributesAsString(Class<?> modelClass, String fieldName) {
    Map<String, String> attrs = getHtmlAttributes(modelClass, fieldName);
    StringBuilder sb = new StringBuilder();

    for (Map.Entry<String, String> entry : attrs.entrySet()) {
      sb.append("%s=\"%s\" ".formatted(entry.getKey(), entry.getValue()));
    }

    return sb.toString().trim();
  }
}
