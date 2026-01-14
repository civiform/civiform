package views.tags.settings;

import java.util.Map;
import lombok.Builder;
import lombok.experimental.Accessors;
import views.tags.AbstractElementModelProcessor.AttributeInfo;

@Accessors(fluent = true)
public final class InputFormSettings extends FormSettings {
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
        attributeMap);
  }
}
