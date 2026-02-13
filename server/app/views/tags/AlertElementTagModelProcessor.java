package views.tags;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import helpers.BetterLinkedHashMap;
import helpers.BetterStringJoiner;
import java.util.LinkedHashMap;
import java.util.Map;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.model.IText;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;
import org.unbescape.html.HtmlEscape;
import views.tags.settings.AlertFormSettings;

/** Processor for <cf:alert> that transforms: <cf:alert type="info" slim="true" no-icon="true"> */
public final class AlertElementTagModelProcessor extends AbstractElementModelProcessor {

  private static final String TAG_NAME = "alert";
  private static final int PRECEDENCE = 1000;

  public AlertElementTagModelProcessor(final String dialectPrefix) {
    super(TemplateMode.HTML, dialectPrefix, TAG_NAME, true, null, false, PRECEDENCE);
  }

  @Override
  protected void doProcess(
      ITemplateContext context,
      IModel sourceModel,
      IElementModelStructureHandler structureHandler) {

    IProcessableElementTag tag = (IProcessableElementTag) sourceModel.get(0);
    IModelFactory modelFactory = context.getModelFactory();

    // Evaluate th:if / th:unless before doing anything else. If the condition
    // says "don't render", wipe the model and bail out early.
    if (!evaluateConditionalAttributes(context, tag)) {
      sourceModel.reset();
      return;
    }

    IModel targetModel = modelFactory.createModel();

    var elementSettings =
        AlertFormSettings.builder()
            .alertType(getAttributeInfo(context, tag, "type", "info"))
            .slim(getAttributeInfo(context, tag, "slim", "false"))
            .noIcon(getAttributeInfo(context, tag, "no-icon", "false"))
            .build();

    validate(sourceModel, elementSettings);

    // Add outermost usa-alert div with classes
    var alertClasses =
        BetterStringJoiner.withSpaceDelimiter()
            .add("usa-alert")
            .add("usa-alert--%s".formatted(elementSettings.alertType().value()))
            .addIf(elementSettings.slim().valueAsBoolean(), "usa-alert--slim")
            .addIf(elementSettings.noIcon().valueAsBoolean(), "use-alert--no-icon");

    var alertDivAttrs = new BetterLinkedHashMap<String, String>();
    alertDivAttrs.put("class", alertClasses.toString());

    // open outer container div
    targetModel.add(modelFactory.createOpenElementTag("div", alertDivAttrs, null, false));

    // open alert body div
    var alertBodyDivAttrs = new BetterLinkedHashMap<String, String>();
    alertBodyDivAttrs.put("class", "usa-alert__body");

    targetModel.add(modelFactory.createOpenElementTag("div", alertBodyDivAttrs, null, false));

    // Process the body content and transform heading (h1-h6), <text>, and <content> elements
    processBodyContent(context, sourceModel, targetModel, modelFactory);

    // close alert body div
    targetModel.add(modelFactory.createCloseElementTag("div"));

    // close outer alert container div
    targetModel.add(modelFactory.createCloseElementTag("div"));

    // Replace the original model
    sourceModel.reset();
    sourceModel.addModel(targetModel);
  }

  // ---------------------------------------------------------------------------
  // Conditional attribute evaluation (th:if / th:unless)
  // ---------------------------------------------------------------------------

  /**
   * Evaluates {@code th:if} and {@code th:unless} on the {@code <cf:alert>} opening tag.
   *
   * @return {@code true} if the element should be rendered, {@code false} if it should be
   *     suppressed.
   */
  private boolean evaluateConditionalAttributes(
      ITemplateContext context, IProcessableElementTag tag) {

    // th:if — suppress when expression is falsy
    String thIf = tag.getAttributeValue("th:if");

    if (thIf != null) {
      Object result = evaluateExpression(context, thIf);
      if (!isTruthy(result)) {
        return false;
      }
    }

    // th:unless — suppress when expression is truthy
    String thUnless = tag.getAttributeValue("th:unless");

    if (thUnless != null) {
      Object result = evaluateExpression(context, thUnless);
      if (isTruthy(result)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Evaluates a Thymeleaf expression string (e.g. {@code "${someVar}"}) against the current
   * context.
   */
  private Object evaluateExpression(ITemplateContext context, String expression) {
    IStandardExpressionParser parser =
        StandardExpressions.getExpressionParser(context.getConfiguration());
    IStandardExpression parsedExpr = parser.parseExpression(context, expression);
    return parsedExpr.execute(context);
  }

  /**
   * Mirrors Thymeleaf's own truthiness rules: {@code null}, {@code false}, and the string {@code
   * "false"} are all falsy; everything else is truthy.
   */
  private boolean isTruthy(Object value) {
    if (value == null) return false;
    if (value instanceof Boolean b) return b;
    if (value instanceof String s) return !s.isEmpty() && !s.equalsIgnoreCase("false");
    return true;
  }

  // ---------------------------------------------------------------------------
  // Validation
  // ---------------------------------------------------------------------------

  private void validate(IModel sourceModel, AlertFormSettings elementSettings) {
    validateHeadingPosition(sourceModel);
    validateSlimAlertHasNoHeadingElements(sourceModel, elementSettings);
    validateTextOrContentElement(sourceModel);
  }

  /** Validate that heading elements (h1-h6) only appear as the first element */
  private void validateHeadingPosition(IModel model) {
    boolean foundFirstElement = false;
    boolean foundHeading = false;

    for (int i = 1; i < model.size() - 1; i++) {
      ITemplateEvent event = model.get(i);

      if (event instanceof IProcessableElementTag elementTag) {
        String elementName = elementTag.getElementCompleteName();

        if (isHeadingElement(elementName)) {
          if (foundFirstElement && !foundHeading) {
            throw new TemplateProcessingException(
                "Invalid cf:alert configuration: heading elements (h1-h6) must be the first element"
                    + " in the alert");
          }
          foundHeading = true;
          foundFirstElement = true;
        } else if (!isWhitespaceOnlyText(event)) {
          foundFirstElement = true;
        }
      } else if (event instanceof IText textEvent) {
        if (!textEvent.getText().trim().isEmpty()) {
          foundFirstElement = true;
        }
      }
    }
  }

  private void validateSlimAlertHasNoHeadingElements(
      IModel sourceModel, AlertFormSettings elementSettings) {
    if (elementSettings.slim().valueAsBoolean() && containsHeadingElement(sourceModel)) {
      throw new TemplateProcessingException(
          "Invalid cf:alert configuration: slim=\"true\" cannot be used with heading elements"
              + " (h1-h6)");
    }
  }

  private void validateTextOrContentElement(IModel sourceModel) {
    // Validate: only one of <text> or <content> can be used
    boolean hasText = containsTextElement(sourceModel);
    boolean hasContent = containsContentElement(sourceModel);

    if (hasText && hasContent) {
      throw new TemplateProcessingException(
          "Invalid cf:alert configuration: cannot use both <text> and <content> elements. Use only"
              + " one.");
    }
  }

  /** Check if the model contains a heading element (h1-h6) */
  private boolean containsHeadingElement(IModel model) {
    for (int i = 1; i < model.size() - 1; i++) {
      ITemplateEvent event = model.get(i);
      if (event instanceof IProcessableElementTag elementTag) {
        if (isHeadingElement(elementTag.getElementCompleteName())) {
          return true;
        }
      }
    }
    return false;
  }

  /** Check if the model contains a text element */
  private boolean containsTextElement(IModel model) {
    for (int i = 1; i < model.size() - 1; i++) {
      ITemplateEvent event = model.get(i);
      if (event instanceof IProcessableElementTag elementTag) {
        if (isTextElement(elementTag.getElementCompleteName())) {
          return true;
        }
      }
    }
    return false;
  }

  /** Check if the model contains a content element */
  private boolean containsContentElement(IModel model) {
    for (int i = 1; i < model.size() - 1; i++) {
      ITemplateEvent event = model.get(i);
      if (event instanceof IProcessableElementTag elementTag) {
        if (isContentElement(elementTag.getElementCompleteName())) {
          return true;
        }
      }
    }
    return false;
  }

  // ---------------------------------------------------------------------------
  // Body content processing
  // ---------------------------------------------------------------------------

  /**
   * Process the body content of the alert, transforming heading (h1-h6), {@code <text>}, and {@code
   * <content>} elements.
   */
  private void processBodyContent(
      ITemplateContext context,
      IModel originalModel,
      IModel targetModel,
      IModelFactory modelFactory) {

    int i = 1; // Skip the opening <cf:alert> tag
    while (i < originalModel.size() - 1) { // Stop before closing </cf:alert> tag
      ITemplateEvent event = originalModel.get(i);

      if (event instanceof IProcessableElementTag elementTag) {
        String elementName = elementTag.getElementCompleteName();

        if (isHeadingElement(elementName)) {
          // Transform h1-h6 to include class="usa-alert__heading"
          i = processHeadingElement(originalModel, targetModel, modelFactory, i, elementName);
        } else if (isTextElement(elementName)) {
          // Transform <text> to <p class="usa-alert__text">
          i = processTextElement(context, originalModel, targetModel, modelFactory, i);
        } else if (isContentElement(elementName)) {
          // Insert <content> HTML as-is
          i = processContentElement(originalModel, targetModel, i);
        } else {
          // Copy other elements as-is
          targetModel.add(event);
          i++;
        }
      } else if (event instanceof IText textEvent) {
        // Skip whitespace-only text nodes
        if (!textEvent.getText().trim().isEmpty()) {
          targetModel.add(event);
        }
        i++;
      } else {
        // Copy other events (comments, etc.)
        targetModel.add(event);
        i++;
      }
    }
  }

  /**
   * Process h1-h6 element and add {@code usa-alert__heading} class.
   *
   * @return the index after the closing tag
   */
  private int processHeadingElement(
      IModel originalModel,
      IModel targetModel,
      IModelFactory modelFactory,
      int startIndex,
      String headingTagName) {

    IProcessableElementTag headingTag = (IProcessableElementTag) originalModel.get(startIndex);

    // Get the base heading tag name (h1, h2, etc.) without any prefix

    // Build attributes map with usa-alert__heading class
    Map<String, String> headingAttrs = new LinkedHashMap<>();

    // Copy existing attributes
    headingTag.getAttributeMap().forEach(headingAttrs::put);

    // Add or merge the usa-alert__heading class
    String existingClass = headingAttrs.get("class");
    if (isNotBlank(existingClass)) {
      headingAttrs.put("class", existingClass + " usa-alert__heading");
    } else {
      headingAttrs.put("class", "usa-alert__heading");
    }

    targetModel.add(modelFactory.createOpenElementTag(headingTagName, headingAttrs, null, false));

    // Copy the content between opening and closing tags
    int closingIndex = findClosingTag(originalModel, startIndex, headingTagName);
    for (int i = startIndex + 1; i < closingIndex; i++) {
      targetModel.add(originalModel.get(i));
    }

    // Close heading tag
    targetModel.add(modelFactory.createCloseElementTag(headingTagName));

    return closingIndex + 1; // Return index after closing tag
  }

  /**
   * Process {@code <text>} element and transform to {@code <p class="usa-alert__text">}.
   *
   * <p>If the {@code <text>} tag carries a {@code th:text} (or {@code data-th-text}) attribute, the
   * expression is evaluated and its result is emitted as the paragraph's text content, replacing
   * any inner markup. Otherwise the inner content is copied as-is (original behaviour).
   *
   * @return the index after the closing tag
   */
  private int processTextElement(
      ITemplateContext context,
      IModel originalModel,
      IModel targetModel,
      IModelFactory modelFactory,
      int startIndex) {

    IProcessableElementTag textTag = (IProcessableElementTag) originalModel.get(startIndex);

    Map<String, String> textAttrs = new LinkedHashMap<>();
    textAttrs.put("class", "usa-alert__text");
    targetModel.add(modelFactory.createOpenElementTag("p", textAttrs, null, false));

    // Copy the content between opening and closing tags
    int closingIndex = findClosingTag(originalModel, startIndex, "text");

    String thText = textTag.getAttributeValue("th:text");

    // Close p tag
    if (thText != null) {
      // Evaluate the expression and emit the result as an escaped text node,
      // matching Thymeleaf's standard th:text behaviour.
      Object result = evaluateExpression(context, thText);
      String evaluated = (result == null) ? "" : result.toString();
      targetModel.add(modelFactory.createText(HtmlEscape.escapeHtml4Xml(evaluated)));
    } else {
      // Original behaviour: copy inner content as-is
      for (int i = startIndex + 1; i < closingIndex; i++) {
        targetModel.add(originalModel.get(i));
      }
    }

    targetModel.add(modelFactory.createCloseElementTag("p"));

    return closingIndex + 1; // Return index after closing tag
  }

  /**
   * Process {@code <content>} element and insert its contents as-is.
   *
   * @return the index after the closing tag
   */
  private int processContentElement(IModel originalModel, IModel targetModel, int startIndex) {

    // Copy all content between opening and closing tags without modification
    int closingIndex = findClosingTag(originalModel, startIndex, "content");
    for (int i = startIndex + 1; i < closingIndex; i++) {
      targetModel.add(originalModel.get(i));
    }

    return closingIndex + 1; // Return index after closing tag
  }

  /** Find the closing tag for a given element */
  private int findClosingTag(IModel model, int openingTagIndex, String tagName) {
    int depth = 1;
    for (int i = openingTagIndex + 1; i < model.size(); i++) {
      ITemplateEvent event = model.get(i);
      if (event instanceof IProcessableElementTag tag) {
        if (isMatchingElement(tag.getElementCompleteName(), tagName)) {
          depth++;
        }
      } else if (event instanceof ICloseElementTag closeTag) {
        if (isMatchingElement(closeTag.getElementCompleteName(), tagName)) {
          depth--;
          if (depth == 0) {
            return i;
          }
        }
      }
    }
    return model.size() - 1;
  }

  /** Check if element is a heading element (h1-h6) */
  private boolean isHeadingElement(String elementName) {
    return elementName.matches("^h[1-6]$");
  }

  private boolean isTextElement(String elementName) {
    return "text".equals(elementName);
  }

  private boolean isContentElement(String elementName) {
    return "content".equals(elementName);
  }

  private boolean isMatchingElement(String elementName, String tagName) {
    return elementName.equals(tagName);
  }

  private boolean isWhitespaceOnlyText(ITemplateEvent event) {
    if (event instanceof IText textEvent) {
      return textEvent.getText().trim().isEmpty();
    }
    return false;
  }
}
