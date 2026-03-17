package views.tags.settings;

import static org.apache.commons.lang3.StringUtils.isBlank;
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
public class CheckboxFormSettings extends FormSettings {
  private static final ImmutableList<String> ALLOWED_COLUMNS = ImmutableList.of("2", "3", "4");

  private final AttributeInfo tiled;
  private final AttributeInfo columns;

  @Builder
  public CheckboxFormSettings(
      AttributeInfo id,
      AttributeInfo name,
      AttributeInfo label, // required
      AttributeInfo helpText,
      AttributeInfo validationMessage,
      AttributeInfo required,
      AttributeInfo isValid,
      AttributeInfo value, // required
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
        null, // placeholder
        size,
        validationClass,
        validationField,
        readonly,
        disabled,
        attributeMap);
    this.tiled = tiled;
    this.columns = columns;
  }

  public String columnCssClass() {
    if (isBlank(columns().value())) {
      return "";
    }

    return "checkbox-group-col-%s".formatted(columns().value());
  }

  @Override
  protected StringBuilder validateInternal() {
    var sb = new StringBuilder();

    if (isNotBlank(columns().value()) && !ALLOWED_COLUMNS.contains(columns().value())) {
      sb.append(
          "Attribute 'columns' is not valid with '%s'. Either do not set 'columns' or use one of these allowed options: %s\n"
              .formatted(columns().value(), String.join(", ", ALLOWED_COLUMNS)));
    }

    return sb;
  }
}
