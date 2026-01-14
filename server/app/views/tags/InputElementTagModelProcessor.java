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
import views.tags.settings.InputFormSettings;

/**
 * Custom Thymeleaf element that will render into an input element with all appropriate HTML to be
 * structured correctly in terms of labels, validation messages, etc.
 *
 * <p>Sample Thymeleaf:
 *
 * <pre>{@code <cf:input id="firstName" name="firstName" label="First Name" />}</pre>
 *
 * <p>Sample rendered HTML:
 *
 * <pre>{@code <div class="usa-form-group">
 *   <label class="usa-label" for="firstName">First Name</label>
 *   <span id="error-message-firstName" class="usa-error-message" role="alert" hidden="hidden"></span>
 *   <input type="text" class="usa-input" id="firstName" name="firstName"/>
 * </div>
 * }</pre>
 *
 * @see <a href="http://localhost:9000/dev/component/catalog/input">Input Component - Component
 *     Catalog</a>
 */
public final class InputElementTagModelProcessor extends AbstractElementModelProcessor {
  private static final String TAG_NAME = "input";
  private static final int PRECEDENCE = 1000;

  public InputElementTagModelProcessor(final String dialectPrefix) {
    super(
        TemplateMode.HTML,
        dialectPrefix, // Prefix for <cf:input>
        TAG_NAME,
        true, // Apply to tag name
        null,
        false,
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

    InputFormSettings elementSettings =
        InputFormSettings.builder()
            .id(getAttributeInfo(context, tag, "id"))
            .name(getAttributeInfo(context, tag, "name"))
            .label(getAttributeInfo(context, tag, "label"))
            .helpText(getAttributeInfo(context, tag, "help-text"))
            .validationMessage(getAttributeInfo(context, tag, "validation-message"))
            .value(getAttributeInfo(context, tag, "value"))
            .placeholder(getAttributeInfo(context, tag, "placeholder"))
            .type(getAttributeInfo(context, tag, "type", "text"))
            .size(getAttributeInfo(context, tag, "size"))
            .isValid(getAttributeInfo(context, tag, "is-valid", "true"))
            .required(getAttributeInfo(context, tag, "required"))
            .validationClass(getAttributeInfo(context, tag, "validation-class"))
            .validationField(getAttributeInfo(context, tag, "validation-field"))
            .attributeMap(tag.getAttributeMap())
            .build();

    elementSettings.validate(() -> getTemplateHtml(sourceModel));

    // Add wrapping div with form-group classes
    var divCssClasses =
        BetterStringJoiner.withSpaceDelimiter()
            .add("usa-form-group")
            .addIf(!elementSettings.isValid().valueAsBoolean(), "usa-form-group--error");

    var divAttrs = new BetterLinkedHashMap<String, String>();
    divAttrs.put("class", divCssClasses.toString());

    targetModel.add(modelFactory.createOpenElementTag("div", divAttrs, null, false));

    addLabelElement(elementSettings, targetModel, modelFactory);
    addHelpTextDivElement(elementSettings, targetModel, modelFactory);
    addValidationSpanElement(elementSettings, targetModel, modelFactory);
    addInputElement(elementSettings, targetModel, modelFactory);

    // Close form-group div
    targetModel.add(modelFactory.createCloseElementTag("div"));

    // Replace the entire original model with our new model
    sourceModel.reset();
    sourceModel.addModel(targetModel);
  }

  private static void addInputElement(
      InputFormSettings elementSettings, IModel targetModel, IModelFactory modelFactory) {
    // Build input attributes
    var inputAttrs = new BetterLinkedHashMap<String, String>();

    // Attribute: type
    inputAttrs.put(elementSettings.type().attributeName(), elementSettings.type().value());

    // Attribute: class
    String inputCssClasses =
        BetterStringJoiner.withSpaceDelimiter()
            .add("usa-input")
            .addIf(isNotBlank(elementSettings.size().value()), elementSettings.sizeCssClass())
            .toString();

    inputAttrs.put("class", inputCssClasses);

    // Attribute: id
    inputAttrs.put(elementSettings.id().attributeName(), elementSettings.id().value());

    // Attribute: name
    inputAttrs.put(elementSettings.name().attributeName(), elementSettings.name().value());

    // Attribute: value
    inputAttrs.putIf(
        isNotBlank(elementSettings.value().value()),
        elementSettings.value().attributeName(),
        elementSettings.value().value());

    // Attribute: placeholder
    inputAttrs.putIf(
        isNotBlank(elementSettings.placeholder().value()),
        elementSettings.placeholder().attributeName(),
        elementSettings.placeholder().value());

    // Attribute: required
    inputAttrs.put(buildBooleanAttribute("required", elementSettings.required()));

    // Attribute: aria-describedby
    var ariaDescribedByIds = getAriaDescribedByIds(elementSettings);

    inputAttrs.putIf(
        ariaDescribedByIds.length() > 0, "aria-describedby", ariaDescribedByIds.toString());

    // Attribute: aria-invalid
    inputAttrs.putIf(!elementSettings.isValid().valueAsBoolean(), "aria-invalid", "true");

    // Fill any other data-* and aria-* attributes
    inputAttrs.putAll(getDataAndAriaAttributes(elementSettings.attributeMap()));

    // Set up validation databinding
    if (elementSettings.useValidationDataBinding()) {
      inputAttrs.putAll(getValidationDataBindingAttributes(elementSettings));
    }

    // Add self-closing input tag
    targetModel.add(
        modelFactory.createStandaloneElementTag("input", inputAttrs, null, false, true));
  }
}
