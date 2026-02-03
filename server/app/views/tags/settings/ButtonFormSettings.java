package views.tags.settings;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import views.tags.AbstractElementModelProcessor.AttributeInfo;

@Getter
@Setter
@Accessors(fluent = true)
public final class ButtonFormSettings {
  private static final ImmutableList<String> ALLOWED_TYPES =
      ImmutableList.of("button", "reset", "submit");
  private static final ImmutableList<String> ALLOWED_SIZES = ImmutableList.of("big");
  private static final ImmutableList<String> ALLOWED_VARIANTS =
      ImmutableList.of("secondary", "outline", "accent-cool", "accent-warm", "base", "unstyled");

  private final AttributeInfo id;
  private final AttributeInfo name;
  private final AttributeInfo value;
  private final AttributeInfo text;
  private final AttributeInfo type;
  private final AttributeInfo variant;
  private final AttributeInfo size;
  private final AttributeInfo disabled;
  private final AttributeInfo inverse;
  private final Map<String, String> attributeMap;

  @Builder
  public ButtonFormSettings(
      AttributeInfo id,
      AttributeInfo name,
      AttributeInfo value,
      AttributeInfo text,
      AttributeInfo type,
      AttributeInfo variant,
      AttributeInfo size,
      AttributeInfo disabled,
      AttributeInfo inverse,
      Map<String, String> attributeMap) {
    this.id = id;
    this.name = name;
    this.value = value;
    this.text = text;
    this.type = type;
    this.variant = variant;
    this.size = size;
    this.disabled = disabled;
    this.inverse = inverse;
    this.attributeMap = attributeMap;
  }

  /** Return variant css class or empty string */
  public String getVariantCssClass() {
    if (ALLOWED_VARIANTS.contains(variant().value())) {
      return "usa-button--%s".formatted(variant().value());
    }

    return "";
  }

  /** Return size css class or empty string */
  public String getSizeCssClass() {
    if (size().value().equalsIgnoreCase("big")) {
      return "usa-button--big";
    }

    return "";
  }

  /** Return inverse css class or empty string */
  public String getInverseCssClass() {
    if (inverse().valueAsBoolean()) {
      return "usa-button--inverse";
    }

    return "";
  }

  /**
   * Verify that all the supplied settings are correct, throwing an exception if not.
   *
   * @param thymeleafTemplateSupplier the HTML of the unprocessed thymeleaf template
   */
  public void validate(Supplier<String> thymeleafTemplateSupplier) {
    var sb = new StringBuilder();

    // type
    if (isNotBlank(type().value()) && !ALLOWED_TYPES.contains(type().value())) {
      sb.append(
          "Attribute 'type' is not valid with '%s'. Use one of these allowed types: %s\n"
              .formatted(type().value(), String.join(", ", ALLOWED_TYPES)));
    }

    // size
    if (isNotBlank(size().value()) && !ALLOWED_SIZES.contains(size().value())) {
      sb.append(
          "Attribute 'size' is not valid with '%s'. Either do not set a size or use one of these allowed sizes: %s\n"
              .formatted(size().value(), String.join(", ", ALLOWED_SIZES)));
    }

    // variant
    if (isNotBlank(variant().value()) && !ALLOWED_VARIANTS.contains(variant().value())) {
      sb.append(
          "Attribute 'variant' is not valid with '%s'. Either do not set a variant or use one of these allowed variants: %s\n"
              .formatted(variant().value(), String.join(", ", ALLOWED_VARIANTS)));
    }

    // Configure validation result
    if (!sb.isEmpty()) {
      sb.insert(0, "Civiform 'cf:button' element is invalid.\n\n");
      sb.append("\n");

      var thymeleafTemplate = thymeleafTemplateSupplier.get();
      if (isNotBlank(thymeleafTemplate)) {
        sb.append(thymeleafTemplate);
      }

      throw new IllegalArgumentException(sb.toString());
    }
  }
}
