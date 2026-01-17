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
import views.tags.settings.TextAreaFormSettings;

/**
 * Custom Thymeleaf element that will render into an textarea element with all appropriate HTML to
 * be structured correctly in terms of labels, validation messages, etc.
 *
 * <p>Sample Thymeleaf:
 *
 * <pre>{@code <cf:textarea id="firstName" name="firstName" label="First Name" />}</pre>
 *
 * <p>Sample rendered HTML:
 *
 * <pre>{@code <div class="usa-form-group">
 *   <label class="usa-label" for="firstName">First Name</label>
 *   <span id="error-message-firstName" class="usa-error-message" role="alert" hidden="hidden"></span>
 *   <textarea class="usa-textarea" id="firstName" name="firstName"></textarea>
 * </div>
 * }</pre>
 *
 * @see <a href="http://localhost:9000/dev/component/catalog/textarea">Textarea Component -
 *     Component Catalog</a>
 */
public final class TextAreaElementTagModelProcessor extends AbstractElementModelProcessor {
  private static final String TAG_NAME = "textarea";
  private static final int PRECEDENCE = 1000;

  public TextAreaElementTagModelProcessor(final String dialectPrefix) {
    super(
        TemplateMode.HTML,
        dialectPrefix, // Prefix for <cf:textarea>
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

    TextAreaFormSettings elementSettings =
        TextAreaFormSettings.builder()
            .id(getAttributeInfo(context, tag, "id"))
            .name(getAttributeInfo(context, tag, "name"))
            .label(getAttributeInfo(context, tag, "label"))
            .helpText(getAttributeInfo(context, tag, "help-text"))
            .validationMessage(getAttributeInfo(context, tag, "validation-message"))
            .value(getAttributeInfo(context, tag, "value"))
            .placeholder(getAttributeInfo(context, tag, "placeholder"))
            .size(getAttributeInfo(context, tag, "size"))
            .isValid(getAttributeInfo(context, tag, "is-valid", "true"))
            .required(getAttributeInfo(context, tag, "required"))
            .markdownEnabled(getAttributeInfo(context, tag, "markdown-enabled", "false"))
            .validationClass(getAttributeInfo(context, tag, "validation-class"))
            .validationField(getAttributeInfo(context, tag, "validation-field"))
            .minLength(getAttributeInfo(context, tag, "minlength"))
            .maxLength(getAttributeInfo(context, tag, "maxlength"))
            .readonly(getAttributeInfo(context, tag, "readonly", "false"))
            .disabled(getAttributeInfo(context, tag, "disabled", "false"))
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
    addTextAreaElement(elementSettings, targetModel, modelFactory);

    // Close form-group div
    targetModel.add(modelFactory.createCloseElementTag("div"));

    // Replace the entire original model with our new model
    sourceModel.reset();
    sourceModel.addModel(targetModel);
  }

  private static void addTextAreaElement(
      TextAreaFormSettings elementSettings, IModel targetModel, IModelFactory modelFactory) {
    // Build input attributes
    var textareaAttrs = new BetterLinkedHashMap<String, String>();

    // Attribute: class
    String textareaCssClasses =
        BetterStringJoiner.withSpaceDelimiter()
            .add("usa-textarea")
            .addIf(isNotBlank(elementSettings.size().value()), elementSettings.sizeCssClass())
            .toString();

    textareaAttrs.put("class", textareaCssClasses);

    // Attribute: id
    textareaAttrs.put(elementSettings.id().attributeName(), elementSettings.id().value());

    // Attribute: name
    textareaAttrs.put(elementSettings.name().attributeName(), elementSettings.name().value());

    // Attribute: placeholder
    textareaAttrs.putIf(
        isNotBlank(elementSettings.placeholder().value()),
        elementSettings.placeholder().attributeName(),
        elementSettings.placeholder().value());

    // Attribute: required
    textareaAttrs.put(buildBooleanAttribute("required", elementSettings.required()));

    // Attribute: readonly
    textareaAttrs.put(buildBooleanAttribute("readonly", elementSettings.readonly()));

    // Attribute: disabled
    textareaAttrs.put(buildBooleanAttribute("disabled", elementSettings.disabled()));

    // Attribute: aria-describedby
    var ariaDescribedByIds = getAriaDescribedByIds(elementSettings);

    textareaAttrs.putIf(
        ariaDescribedByIds.length() > 0, "aria-describedby", ariaDescribedByIds.toString());

    // Attribute: aria-invalid
    textareaAttrs.putIf(!elementSettings.isValid().valueAsBoolean(), "aria-invalid", "true");

    // Attribute: minLength
    textareaAttrs.putIf(
        isNotBlank(elementSettings.minLength().value()),
        elementSettings.minLength().attributeName(),
        elementSettings.minLength().value());

    // Attribute: maxLength
    textareaAttrs.putIf(
        isNotBlank(elementSettings.maxLength().value()),
        elementSettings.maxLength().attributeName(),
        elementSettings.maxLength().value());

    // Fill any other data-* and aria-* attributes
    textareaAttrs.putAll(getDataAndAriaAttributes(elementSettings.attributeMap()));

    // Set up validation databinding
    if (elementSettings.useValidationDataBinding()) {
      textareaAttrs.putAll(getValidationDataBindingAttributes(elementSettings));
    }

    targetModel.add(modelFactory.createOpenElementTag("textarea", textareaAttrs, null, false));

    // Set value
    if (isNotBlank(elementSettings.value().value())) {
      targetModel.add(modelFactory.createText(elementSettings.value().value()));
    }

    targetModel.add(modelFactory.createCloseElementTag("textarea"));
  }
}
