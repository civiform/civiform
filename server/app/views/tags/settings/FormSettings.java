package views.tags.settings;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.experimental.Accessors;
import views.tags.AbstractElementModelProcessor.AttributeInfo;

@Data
@Accessors(fluent = true)
public abstract class FormSettings {
  private static final ImmutableList<String> ALLOWED_SIZES =
      ImmutableList.of("2xs", "xs", "sm", "small", "md", "medium", "lg", "xl", "2xl");

  private final AttributeInfo id;
  private final AttributeInfo name;
  private final AttributeInfo label;
  private final AttributeInfo helpText;
  private final AttributeInfo validationMessage;
  private final AttributeInfo required;
  private final AttributeInfo isValid;
  private final AttributeInfo value;
  private final AttributeInfo placeholder;
  private final AttributeInfo size;
  private final AttributeInfo validationClass;
  private final AttributeInfo validationField;
  private final AttributeInfo readonly;
  private final AttributeInfo disabled;
  private final Map<String, String> attributeMap;

  private final String errorMessageId;
  private final String helpTextMessageId;

  protected FormSettings(
      AttributeInfo id,
      AttributeInfo name,
      AttributeInfo label,
      AttributeInfo helpText,
      AttributeInfo validationMessage,
      AttributeInfo required,
      AttributeInfo isValid,
      AttributeInfo value,
      AttributeInfo placeholder,
      AttributeInfo size,
      AttributeInfo validationClass,
      AttributeInfo validationField,
      AttributeInfo readonly,
      AttributeInfo disabled,
      Map<String, String> attributeMap) {
    this.id = id;
    this.name = name;
    this.label = label;
    this.helpText = helpText;
    this.validationMessage = validationMessage;
    this.required = required;
    this.isValid = isValid;
    this.value = value;
    this.placeholder = placeholder;
    this.size = size;
    this.validationClass = validationClass;
    this.validationField = validationField;
    this.readonly = readonly;
    this.disabled = disabled;
    this.attributeMap = attributeMap;

    this.errorMessageId = makeId("error-message", name());
    this.helpTextMessageId = makeId("help-text", name());
  }

  public String errorMessageId() {
    return errorMessageId;
  }

  public String helpTextId() {
    return helpTextMessageId;
  }

  public String sizeCssClass() {
    return "usa-input--%s".formatted(size().value());
  }

  /** Returns true if configured to use reflection to find the validation settings */
  public boolean useValidationDataBinding() {
    return isNotBlank(validationClass().value()) && isNotBlank(validationField().value());
  }

  /**
   * Verify that all the supplied settings are correct.
   *
   * @param thymeleafTemplateSupplier the HTML of the unprocessed thymeleaf template
   */
  public final void validate(Supplier<String> thymeleafTemplateSupplier) {
    var sb = new StringBuilder();

    // id
    if (id().value() == null) {
      sb.append("Attribute 'id' is null.\n");
    } else if (isBlank(id().value())) {
      sb.append("Attribute 'id' is blank.\n");
    } else if (!isValidId(id().value())) {
      sb.append("Attribute 'id' is not a valid id\n");
    }

    // name
    if (name().value() == null) {
      sb.append("Attribute 'name' is null.\n");
    } else if (isBlank(name().value())) {
      sb.append("Attribute 'name' is blank.\n");
    } else if (!isValidName(name().value())) {
      sb.append("Attribute 'name' is not a valid name\n");
    }

    // label
    if (label() == null) {
      sb.append("Attribute 'label' is null.\n");
    } else if (isBlank(label().value())) {
      sb.append("Attribute 'label' is blank.\n");
    }

    // validation class/field
    if (isNotBlank(validationClass().value()) && isBlank(validationField().value())) {
      sb.append(
          "Attribute 'validation-class' is set, but 'validation-field' is not set. Both must be"
              + " set if one is set.\n");
    } else if (isBlank(validationClass().value()) && isNotBlank(validationField().value())) {
      sb.append(
          "Attribute 'validation-field' is set, but 'validation-class' is not set. Both must be"
              + " set if one is set.\n");
    }

    if (!name().isThymeleafAttribute() && !isValidId(errorMessageId())) {
      sb.append("Attribute 'name' is unable to build a valid errorMessageId.\n");
    }

    if (!name().isThymeleafAttribute()
        && isNotBlank(helpText().value())
        && !isValidId(helpTextId())) {
      sb.append("Attribute 'name' is unable to build a valid helpTextId.\n");
    }

    // size
    if (isNotBlank(size().value()) && !ALLOWED_SIZES.contains(size().value())) {
      sb.append(
          "Attribute 'size' is not valid with '%s'. Either do not set a size or use one of these allowed sizes: %s\n"
              .formatted(size(), String.join(", ", ALLOWED_SIZES)));
    }

    // Get any custom validation results
    var validateInternalResult = validateInternal();

    if (validateInternalResult != null && !validateInternalResult.isEmpty()) {
      sb.append(validateInternalResult);
    }

    // Configure validation result
    if (!sb.isEmpty()) {
      sb.insert(0, "Civiform 'cf' element is invalid.\n\n");
      sb.append("\n");

      var thymeleafTemplate = thymeleafTemplateSupplier.get();
      if (isNotBlank(thymeleafTemplate)) {
        sb.append(thymeleafTemplate);
      }

      throw new IllegalStateException(sb.toString());
    }
  }

  /** Override to allow subclasses to add additional validation checks */
  protected StringBuilder validateInternal() {
    return null;
  }

  /** Builds a string that can be safely used as an ID */
  private String makeId(String prefix, AttributeInfo info) {
    if (isBlank(info.value())) {
      return "";
    }

    if (info.isThymeleafAttribute()) {
      return info.value();
    }

    var cleaned = info.value().trim().replaceAll("[^a-zA-Z0-9]+", "-").replaceAll("^-+|-+$", "");

    return "%s-%s".formatted(prefix, cleaned);
  }

  protected static boolean isValidId(String id) {
    if (isBlank(id)) {
      return false;
    }

    // Pattern: starts with letter or underscore, followed by letters, digits, underscores, or
    // hyphens
    return Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_-]*$").matcher(id).matches();
  }

  protected static boolean isValidName(String name) {
    if (name == null || name.isEmpty()) {
      return false;
    }

    // Pattern: alphanumeric, underscore, hyphen, period, and brackets
    // No spaces or other special characters
    return Pattern.compile("^[a-zA-Z0-9_.-]+(?:\\[[a-zA-Z0-9_.-]*\\])*$").matcher(name).matches();
  }
}
