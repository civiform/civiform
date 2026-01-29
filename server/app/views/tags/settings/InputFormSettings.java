package views.tags.settings;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import views.tags.AbstractElementModelProcessor.AttributeInfo;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true)
public final class InputFormSettings extends FormSettings {
  private static final ImmutableList<String> ALLOWED_TYPES =
      ImmutableList.of(
          "color",
          "date",
          "datetime-local",
          "email",
          "hidden",
          "month",
          "number",
          "password",
          "range",
          "search",
          "tel",
          "text",
          "time",
          "url",
          "week");

  private final AttributeInfo type;
  private final AttributeInfo min;
  private final AttributeInfo max;
  private final AttributeInfo minLength;
  private final AttributeInfo maxLength;
  private final AttributeInfo pattern;

  @Builder
  public InputFormSettings(
      AttributeInfo id,
      AttributeInfo name,
      AttributeInfo label,
      AttributeInfo helpText,
      AttributeInfo validationMessage,
      AttributeInfo required,
      AttributeInfo isValid,
      AttributeInfo value,
      AttributeInfo placeholder,
      AttributeInfo type,
      AttributeInfo size,
      AttributeInfo validationClass,
      AttributeInfo validationField,
      AttributeInfo min,
      AttributeInfo max,
      AttributeInfo minLength,
      AttributeInfo maxLength,
      AttributeInfo pattern,
      AttributeInfo readonly,
      AttributeInfo disabled,
      Map<String, String> attributeMap) {
    super(
        id,
        name,
        label,
        helpText,
        validationMessage,
        required,
        isValid,
        value,
        placeholder,
        size,
        validationClass,
        validationField,
        readonly,
        disabled,
        attributeMap);
    this.type = type;
    this.min = min;
    this.max = max;
    this.minLength = minLength;
    this.maxLength = maxLength;
    this.pattern = pattern;
  }

  @Override
  protected StringBuilder validateInternal() {
    var sb = new StringBuilder();

    if (isNotBlank(type().value()) && !ALLOWED_TYPES.contains(type().value())) {
      sb.append(
          "Attribute 'type' is not valid with '%s'. Use one of these allowed types: %s\n"
              .formatted(type(), String.join(", ", ALLOWED_TYPES)));
    }

    if (isNotBlank(minLength().value())
        && !isNumeric(minLength().value())
        && !minLength().isThymeleafAttribute()) {
      sb.append(
          "Attribute 'minLength' is set, but value of '%s' is not a number.\n"
              .formatted(minLength().value()));
    }

    if (isNotBlank(maxLength().value())
        && !isNumeric(maxLength().value())
        && !maxLength().isThymeleafAttribute()) {
      sb.append(
          "Attribute 'maxLength' is set, but value of '%s' is not a number.\n"
              .formatted(maxLength().value()));
    }

    return sb;
  }
}
