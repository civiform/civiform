package views.tags.settings;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;

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
        type,
        size,
        validationClass,
        validationField,
        readonly,
        disabled,
        attributeMap);
    this.min = min;
    this.max = max;
    this.minLength = minLength;
    this.maxLength = maxLength;
    this.pattern = pattern;
  }

  @Override
  protected StringBuilder validateInternal() {
    var sb = new StringBuilder();

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
