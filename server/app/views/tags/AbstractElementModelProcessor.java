package views.tags;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.checkerframework.errorprone.com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import helpers.BetterLinkedHashMap;
import helpers.BetterStringJoiner;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;
import views.PlayValidationAttributeExtractor;
import views.tags.settings.FormSettings;

@Slf4j
public abstract class AbstractElementModelProcessor
    extends org.thymeleaf.processor.element.AbstractElementModelProcessor {
  @Builder
  public record AttributeInfo(
      String attributeName, boolean hasAttribute, boolean isThymeleafAttribute, String value) {
    public Boolean valueAsBoolean() {
      return Boolean.parseBoolean(value());
    }
  }

  public AbstractElementModelProcessor(
      TemplateMode templateMode,
      String dialectPrefix,
      String elementName,
      boolean prefixElementName,
      String attributeName,
      boolean prefixAttributeName,
      int precedence) {
    super(
        templateMode,
        dialectPrefix,
        elementName,
        prefixElementName,
        attributeName,
        prefixAttributeName,
        precedence);
  }

  /**
   * Gets the Thymeleaf template as a string that is currently being operated on to display in
   * errors.
   */
  protected static String getTemplateHtml(IModel model) {
    try {
      StringWriter fullWriter = new StringWriter();
      model.write(fullWriter);
      return fullWriter.toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Get details for an attribute out of the context */
  protected AttributeInfo getAttributeInfo(
      ITemplateContext context, IProcessableElementTag tag, String attributeName) {
    return getAttributeInfo(context, tag, attributeName, null);
  }

  /** Get details for an attribute out of the context */
  protected AttributeInfo getAttributeInfo(
      ITemplateContext context,
      IProcessableElementTag tag,
      String attributeName,
      final String defaultValue) {

    // Not a th-prefixed attribute so return with
    // the supplied value or the default if not provided.
    // The attribute could be empty if it is something like
    // <input required /> which when present indicates it is
    // enabled.
    if (tag.hasAttribute(attributeName)) {
      String value = tag.getAttributeValue(attributeName);

      return AttributeInfo.builder()
          .attributeName(attributeName)
          .hasAttribute(true)
          .isThymeleafAttribute(false)
          .value(value != null ? value : defaultValue)
          .build();
    }

    // Attribute was not found with or without a prefix
    if (!tag.hasAttribute("th", attributeName)) {
      return AttributeInfo.builder()
          .attributeName(attributeName)
          .hasAttribute(false)
          .isThymeleafAttribute(false)
          .value(defaultValue)
          .build();
    }

    String value = tag.getAttributeValue("th", attributeName);

    // No value was found for the th-prefixed attribute, return default value
    if (isBlank(value)) {
      return AttributeInfo.builder()
          .attributeName("th:" + attributeName)
          .hasAttribute(true)
          .isThymeleafAttribute(true)
          .value(defaultValue)
          .build();
    }

    // If the element has a th:each attribute then it is templated to repeat.
    // In this case just use the value of the attribute as-is and let Thymeleaf
    // process the element with its normal methods outside this pass. Otherwise,
    // the expression parser below will throw trying to convert the expression
    // into an actual value.
    if (tag.hasAttribute("th:each")) {
      return AttributeInfo.builder()
          .attributeName("th:" + attributeName)
          .hasAttribute(true)
          .isThymeleafAttribute(true)
          .value(value)
          .build();
    }

    try {
      IStandardExpressionParser parser =
          StandardExpressions.getExpressionParser(context.getConfiguration());
      IStandardExpression expression = parser.parseExpression(context, value);
      Object result = expression.execute(context);
      String resultString = result != null ? result.toString() : defaultValue;

      // if parsing was successful, return as plain attribute name (without the th prefix)
      return AttributeInfo.builder()
          .attributeName(attributeName)
          .hasAttribute(true)
          .isThymeleafAttribute(false)
          .value(resultString)
          .build();

    } catch (RuntimeException ex) {
      var msg =
          "Unable to render custom Thymeleaf element 'cf:%s'. attributeName: 'th:%s', value: '%s'."
              .formatted(
                  getMatchingElementName().getMatchingElementName().getElementName(),
                  attributeName,
                  value);
      log.error(msg, ex);
      throw ex;
    }
  }

  protected static void addLabelElement(
      FormSettings elementSettings, IModel targetModel, IModelFactory modelFactory) {
    var labelAttrs = new BetterLinkedHashMap<String, String>();
    labelAttrs.put("class", "usa-label");
    labelAttrs.put("for", elementSettings.id().value());

    targetModel.add(modelFactory.createOpenElementTag("label", labelAttrs, null, false));
    targetModel.add(modelFactory.createText(elementSettings.label().value()));
    targetModel.add(modelFactory.createCloseElementTag("label"));
  }

  protected static void addHelpTextDivElement(
      FormSettings elementSettings, IModel targetModel, IModelFactory modelFactory) {
    if (isBlank(elementSettings.helpText().value())) {
      return;
    }

    var helpAttrs = new BetterLinkedHashMap<String, String>();
    helpAttrs.put("id", elementSettings.helpTextId());
    helpAttrs.put("class", "usa-hint");

    targetModel.add(modelFactory.createOpenElementTag("div", helpAttrs, null, false));
    targetModel.add(modelFactory.createText(elementSettings.helpText().value()));
    targetModel.add(modelFactory.createCloseElementTag("div"));
  }

  protected static void addValidationSpanElement(
      FormSettings elementSettings, IModel targetModel, IModelFactory modelFactory) {
    // Add validation message span if showing errors (before input)
    var spanAttrs = new BetterLinkedHashMap<String, String>();
    spanAttrs.put("id", elementSettings.errorMessageId());
    spanAttrs.put("class", "usa-error-message");
    spanAttrs.put("role", "alert");

    spanAttrs.putIf(elementSettings.isValid().valueAsBoolean(), "hidden", "hidden");

    targetModel.add(modelFactory.createOpenElementTag("span", spanAttrs, null, false));

    if (isNotBlank(elementSettings.validationMessage().value())) {
      targetModel.add(modelFactory.createText(elementSettings.validationMessage().value()));
    }

    targetModel.add(modelFactory.createCloseElementTag("span"));
  }

  /** Setup any ids to be used later for aria-describedby on the form control element */
  protected static BetterStringJoiner getAriaDescribedByIds(FormSettings elementSettings) {
    return BetterStringJoiner.withSpaceDelimiter()
        .addIf(!elementSettings.isValid().valueAsBoolean(), elementSettings.errorMessageId())
        .addIf(isNotBlank(elementSettings.helpText().value()), elementSettings.helpTextId());
  }

  /** Get the correctly formatted HTML way of setting a boolean attribute. */
  protected static Map.Entry<String, String> buildBooleanAttribute(
      String attributeName, AttributeInfo attrInfo) {
    checkNotNull(attributeName);

    if (isBlank(attrInfo.value())) {
      return null;
    }

    if (attrInfo.isThymeleafAttribute()) {
      return Map.entry("th:%s".formatted(attributeName), attrInfo.value());
    } else if (attrInfo.valueAsBoolean()) {
      return Map.entry(attributeName, attributeName);
    }

    return null;
  }

  /** Filter full attribute map for data-* and aria-* attributes */
  protected static Map<String, String> getDataAndAriaAttributes(Map<String, String> attributeMap) {
    var ignoredAttributes = Set.of("aria-describedby", "aria-invalid");
    var allowedAttributePrefixes = ImmutableList.of("data-", "th:data-", "aria-", "th:aria-");

    Map<String, String> attrs = new LinkedHashMap<>();

    attributeMap.forEach(
        (name, value) -> {
          if (!ignoredAttributes.contains(name)
              && allowedAttributePrefixes.stream().anyMatch(name::startsWith)) {
            attrs.put(name, value);
          }
        });

    return attrs;
  }

  /**
   * Builds a map of data-* attributes for client side validation pulling in display messages from
   * the Play constraint annotations on the data model.
   */
  protected static Map<String, String> getValidationDataBindingAttributes(
      FormSettings elementSettings) {
    return PlayValidationAttributeExtractor.getHtmlAttributes(
            findClass(elementSettings.validationClass().value()),
            elementSettings.validationField().value())
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                entry -> "th:" + entry.getKey(),
                entry -> {
                  String key = "th:" + entry.getKey();
                  // Transform message keys to Thymeleaf expressions
                  return key.startsWith("th:data-") && key.endsWith("-message")
                      ? "#{%s}".formatted(entry.getValue())
                      : entry.getValue();
                }));
  }

  /** Get the class based on the string className */
  private static Class<?> findClass(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Could not find class: '%s'".formatted(className), e);
    }
  }
}
