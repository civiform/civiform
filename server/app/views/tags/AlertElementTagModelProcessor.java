package views.tags;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.model.IText;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;
import views.tags.settings.AlertFormSettings;

/**
 * Processor for <cf:alert> that transforms: <cf:alert type="info" slim="true" no-icon="true">
 *
 * <h2>Lorem Ipsum</h2>
 *
 * <text>Lorem Ipsum</text> </cf:alert>
 *
 * <p>Or with <content> for rich HTML: <cf:alert type="success">
 *
 * <h2>Lorem Ipsum</h2>
 *
 * <content>
 *
 * <p>Lorem Ipsum
 *
 * <ul>
 *   <li>one
 * </ul>
 *
 * </content> </cf:alert>
 *
 * <p>Into USWDS alert markup with proper structure and classes. Note: slim="true" and heading
 * elements (h1-h6) cannot be used together. Note: Only one of <text> or <content> can be used, not
 * both. Note: Heading elements (h1-h6) must be the first element in the alert.
 */
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
    IModel targetModel = modelFactory.createModel();

    var elementSettings =
        AlertFormSettings.builder()
            .alertType(getAttributeInfo(context, tag, "type", "info"))
            .slim(getAttributeInfo(context, tag, "slim", "false"))
            .noIcon(getAttributeInfo(context, tag, "no-icon", "false"))
            .build();

    validate(sourceModel, elementSettings);

    // Build class string for outer div
    var alertClasses = new StringJoiner(" ").add("usa-alert");
    alertClasses.add("usa-alert--%s".formatted(elementSettings.alertType().value()));

    if (elementSettings.slim().valueAsBoolean()) {
      alertClasses.add("usa-alert--slim");
    }

    if (elementSettings.noIcon().valueAsBoolean()) {
      alertClasses.add("usa-alert--no-icon");
    }

    // open outer container div
    targetModel.add(
        modelFactory.createOpenElementTag(
            "div", Map.of("class", alertClasses.toString()), null, false));

    // open alert body div
    targetModel.add(
        modelFactory.createOpenElementTag("div", Map.of("class", "usa-alert__body"), null, false));

    // Process the body content and transform heading (h1-h6), <text>, and <content> elements
    processBodyContent(sourceModel, targetModel, modelFactory);

    // close alert body div
    targetModel.add(modelFactory.createCloseElementTag("div"));

    // close outer container div
    targetModel.add(modelFactory.createCloseElementTag("div"));

    // Replace the original model
    sourceModel.reset();
    sourceModel.addModel(targetModel);
  }

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
        String elementName = elementTag.getElementCompleteName();
        if (isHeadingElement(elementName)) {
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
        String elementName = elementTag.getElementCompleteName();
        if (isTextElement(elementName)) {
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
        String elementName = elementTag.getElementCompleteName();
        if (isContentElement(elementName)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Process the body content of the alert, transforming heading (h1-h6), <text>, and <content>
   * elements
   */
  private void processBodyContent(
      IModel originalModel, IModel targetModel, IModelFactory modelFactory) {

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
          i = processTextElement(originalModel, targetModel, modelFactory, i);
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
   * Process h1-h6 element and add usa-alert__heading class
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
    headingTag
        .getAttributeMap()
        .forEach(
            (attrName, attrValue) -> {
              headingAttrs.put(attrName, attrValue);
            });

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
   * Process <text> element and transform to
   *
   * <p>tag
   *
   * @return the index after the closing tag
   */
  private int processTextElement(
      IModel originalModel, IModel targetModel, IModelFactory modelFactory, int startIndex) {

    Map<String, String> textAttrs = new LinkedHashMap<>();
    textAttrs.put("class", "usa-alert__text");
    targetModel.add(modelFactory.createOpenElementTag("p", textAttrs, null, false));

    // Copy the content between opening and closing tags
    int closingIndex = findClosingTag(originalModel, startIndex, "text");
    for (int i = startIndex + 1; i < closingIndex; i++) {
      targetModel.add(originalModel.get(i));
    }

    // Close p tag
    targetModel.add(modelFactory.createCloseElementTag("p"));

    return closingIndex + 1; // Return index after closing tag
  }

  /**
   * Process <content> element and insert its contents as-is
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
        String elementName = tag.getElementCompleteName();
        if (isMatchingElement(elementName, tagName)) {
          depth++;
        }
      } else if (event instanceof ICloseElementTag closeTag) {
        String elementName = closeTag.getElementCompleteName();
        if (isMatchingElement(elementName, tagName)) {
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
