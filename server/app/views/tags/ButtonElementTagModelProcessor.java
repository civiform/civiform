package views.tags;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import helpers.BetterLinkedHashMap;
import helpers.BetterStringJoiner;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;
import views.tags.settings.ButtonFormSettings;

/**
 * Custom Thymeleaf element that will render into a button element with all appropriate HTML to be
 * structured correctly for USWDS.
 *
 * <p>Sample Thymeleaf:
 *
 * <pre>{@code <cf:button id="submitBtn" text="Submit" />}</pre>
 *
 * <p>Sample rendered HTML:
 *
 * <pre>{@code <button type="button" class="usa-button" id="submitBtn">Submit</button>
 * }</pre>
 *
 * @see <a href="http://localhost:9000/dev/component/catalog/button">Button Component - Component
 *     Catalog</a>
 */
public final class ButtonElementTagModelProcessor extends AbstractElementModelProcessor {

  private static final String TAG_NAME = "button";
  private static final int PRECEDENCE = 1000;

  public ButtonElementTagModelProcessor(final String dialectPrefix) {
    super(
        TemplateMode.HTML,
        dialectPrefix, // Prefix for <cf:button>
        TAG_NAME,
        /* prefixElementName= */ true, // Apply to tag name
        /* attributeName= */ null,
        /* prefixAttributeName= */ false,
        PRECEDENCE);
  }

  @Override
  protected void doProcess(
      ITemplateContext context,
      IModel sourceModel,
      IElementModelStructureHandler structureHandler) {

    // Get the opening tag (first event in the model)
    IProcessableElementTag tag = (IProcessableElementTag) sourceModel.get(0);
    IModelFactory modelFactory = context.getModelFactory();
    IModel targetModel = modelFactory.createModel();

    var elementSettings =
        ButtonFormSettings.builder()
            .id(getAttributeInfo(context, tag, "id"))
            .name(getAttributeInfo(context, tag, "name"))
            .value(getAttributeInfo(context, tag, "value"))
            .text(getAttributeInfo(context, tag, "text"))
            .type(getAttributeInfo(context, tag, "type", "button"))
            .variant(getAttributeInfo(context, tag, "variant", ""))
            .size(getAttributeInfo(context, tag, "size", ""))
            .disabled(getAttributeInfo(context, tag, "disabled"))
            .inverse(getAttributeInfo(context, tag, "inverse", "false"))
            .attributeMap(tag.getAttributeMap())
            .build();

    elementSettings.validate(() -> getTemplateHtml(sourceModel));

    // Build button classes
    var buttonCssClasses =
        BetterStringJoiner.withSpaceDelimiter()
            .add("usa-button")
            .addIf(
                isNotBlank(elementSettings.variant().value()), elementSettings.getVariantCssClass())
            .addIf(isNotBlank(elementSettings.size().value()), elementSettings.getSizeCssClass())
            .addIf(
                elementSettings.inverse().valueAsBoolean(), elementSettings.getInverseCssClass());

    // Build button attributes
    var buttonAttrs = new BetterLinkedHashMap<String, String>();
    buttonAttrs.put("class", buttonCssClasses.toString());

    // Render <a> for type=link, <button> otherwise
    if ("link".equals(elementSettings.type().value())) {
      // Use getAttributeInfo for href
      var hrefInfo = getAttributeInfo(context, tag, "href", "#");
      String href = hrefInfo.value();
      // Evaluate Thymeleaf expression if present
      if (hrefInfo.isThymeleafAttribute() && isNotBlank(href)) {
        var parser =
            org.thymeleaf.standard.expression.StandardExpressions.getExpressionParser(
                context.getConfiguration());
        var expr = parser.parseExpression(context, href);
        Object result = expr.execute(context);
        href = result != null ? result.toString() : "#";
      }
      buttonAttrs.put("href", href);
      // Attribute: id
      buttonAttrs.putIf(
          isNotBlank(elementSettings.id().value()),
          elementSettings.id().attributeName(),
          elementSettings.id().value());
      // Fill any other data-* and aria-* attributes
      buttonAttrs.putAll(getDataAndAriaAttributes(elementSettings.attributeMap()));
      // Add opening <a> tag
      targetModel.add(
          modelFactory.createOpenElementTag(
              "a", buttonAttrs, /* attributeValueQuotes= */ null, /* synthetic= */ false));
      // Add text content or child elements
      if (isNotBlank(elementSettings.text().value())) {
        targetModel.add(modelFactory.createText(elementSettings.text().value()));
      } else {
        for (int i = 1; i < sourceModel.size() - 1; i++) {
          targetModel.add(sourceModel.get(i));
        }
      }
      // Add closing <a> tag
      targetModel.add(modelFactory.createCloseElementTag("a"));
    } else {
      // Attribute: type
      buttonAttrs.put("type", elementSettings.type().value());
      // Attribute: id
      buttonAttrs.putIf(
          isNotBlank(elementSettings.id().value()),
          elementSettings.id().attributeName(),
          elementSettings.id().value());
      // Attribute: name
      buttonAttrs.putIf(
          isNotBlank(elementSettings.name().value()),
          elementSettings.name().attributeName(),
          elementSettings.name().value());
      // Attribute: value
      buttonAttrs.putIf(
          isNotBlank(elementSettings.value().value()),
          elementSettings.value().attributeName(),
          elementSettings.value().value());
      // Attribute: disabled
      buttonAttrs.put(buildBooleanAttribute("disabled", elementSettings.disabled()));
      // Fill any other data-* and aria-* attributes
      buttonAttrs.putAll(getDataAndAriaAttributes(elementSettings.attributeMap()));
      // Add opening <button> tag
      targetModel.add(
          modelFactory.createOpenElementTag(
              "button", buttonAttrs, /* attributeValueQuotes= */ null, /* synthetic= */ false));
      // Add text content or child elements
      if (isNotBlank(elementSettings.text().value())) {
        targetModel.add(modelFactory.createText(elementSettings.text().value()));
      } else {
        for (int i = 1; i < sourceModel.size() - 1; i++) {
          targetModel.add(sourceModel.get(i));
        }
      }
      // Add closing <button> tag
      targetModel.add(modelFactory.createCloseElementTag("button"));
    }
    // Replace the entire original model with our new model
    sourceModel.reset();
    sourceModel.addModel(targetModel);
  }
}
