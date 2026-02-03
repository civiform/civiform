package views.tags.settings;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.ImmutableList;
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
public class RadioFormSettings extends FormSettings {
  private final AttributeInfo tiled;
  private final AttributeInfo columns;

  @Builder
  public RadioFormSettings(
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
      AttributeInfo tiled,
      AttributeInfo columns,
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
        null, // placeholder,
        size,
        validationClass,
        validationField,
        readonly,
        disabled,
        attributeMap);
    this.tiled = tiled;
    this.columns = columns;
  }

  @Override
  protected StringBuilder validateInternal() {
    var sb = new StringBuilder();

    if (isNotBlank(columns().value())) {
      var allowedColumns = ImmutableList.of("2", "3", "4");

      if (!allowedColumns.contains(columns().value())) {
        sb.append(
            "Attribute 'columns' is not valid with '%s'. Either do not set 'columns' or use one of these allowed options: %s\n"
                .formatted(size(), String.join(", ", allowedColumns)));
      }
    }

    return sb;
  }
}
