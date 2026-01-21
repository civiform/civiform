package views.tags.settings;

import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import views.tags.AbstractElementModelProcessor.AttributeInfo;

@Getter
@Setter
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SelectFormSettings extends FormSettings {

  @Builder
  public SelectFormSettings(
      AttributeInfo id,
      AttributeInfo name,
      AttributeInfo label,
      AttributeInfo helpText,
      AttributeInfo validationMessage,
      AttributeInfo required,
      AttributeInfo isValid,
      AttributeInfo value,
      AttributeInfo size,
      AttributeInfo validationClass,
      AttributeInfo validationField,
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
        null, // placeholder
        size,
        validationClass,
        validationField,
        readonly,
        disabled,
        attributeMap);
  }
}
