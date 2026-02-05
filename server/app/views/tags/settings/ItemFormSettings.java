package views.tags.settings;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import views.tags.AbstractElementModelProcessor.AttributeInfo;
import views.tags.ValidationContext;

@Getter
@Setter
@Accessors(fluent = true)
public final class ItemFormSettings {
  private final AttributeInfo thEach;
  private final AttributeInfo id;
  private final AttributeInfo value;
  private final AttributeInfo label;
  private final AttributeInfo description;
  private final AttributeInfo selected;
  private final AttributeInfo disabled;
  private final Map<String, String> attributeMap;

  @Builder
  public ItemFormSettings(
      AttributeInfo thEach,
      AttributeInfo id,
      AttributeInfo value,
      AttributeInfo label,
      AttributeInfo description,
      AttributeInfo selected,
      AttributeInfo disabled,
      Map<String, String> attributeMap) {
    this.thEach = thEach;
    this.id = id;
    this.value = value;
    this.label = label;
    this.description = description;
    this.selected = selected;
    this.disabled = disabled;
    this.attributeMap = attributeMap;
  }

  public ValidationContext validate() {
    var sb = new StringBuilder();

    // id
    if (id().value() == null) {
      sb.append("Attribute 'id' is null.\n");
    } else if (isBlank(id().value())) {
      sb.append("Attribute 'id' is blank.\n");
    }

    if (label().value() == null) {
      sb.append("Attribute 'label' is null.\n");
    } else if (isBlank(label().value())) {
      sb.append("Attribute 'label' is blank.\n");
    }

    if (value().value() == null) {
      sb.append("Attribute 'value' is null.\n");
    } else if (isBlank(value().value())) {
      sb.append("Attribute 'value' is blank.\n");
    }

    // Configure validation result
    boolean modelIsValid = sb.isEmpty();

    if (!modelIsValid) {
      sb.insert(0, "Civiform 'item' element is invalid.\n\n");
      sb.append("\n");
    }

    return new ValidationContext(modelIsValid, sb);
  }
}
