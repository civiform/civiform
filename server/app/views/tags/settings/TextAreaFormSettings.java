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
public class TextAreaFormSettings extends FormSettings {
  private final AttributeInfo minLength;
  private final AttributeInfo maxLength;
  private final AttributeInfo markdownEnabled;

  @Builder
  public TextAreaFormSettings(
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
      AttributeInfo minLength,
      AttributeInfo maxLength,
      AttributeInfo readonly,
      AttributeInfo disabled,
      AttributeInfo markdownEnabled,
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
    this.minLength = minLength;
    this.maxLength = maxLength;
    this.markdownEnabled = markdownEnabled;
  }

  @Override
  public String sizeCssClass() {
    return "usa-textarea--%s".formatted(size().value());
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
