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
import views.tags.settings.SelectFormSettings;

/**
 * Custom Thymeleaf element that will render into a select element with all appropriate HTML to be
 * structured correctly in terms of labels, validation messages, etc.
 *
 * <p>Sample Thymeleaf:
 *
 * <pre>{@code <cf:select
 *   id="country"
 *   name="country"
 *   label="Country">
 *   <option value="">--Select a country--</option>
 *   <option value="us">United States</option>
 *   <option value="ca">Canada</option>
 * </cf:select>}</pre>
 *
 * <p>Sample rendered HTML:
 *
 * <pre>{@code <div class="usa-form-group">
 *   <label class="usa-label" for="country">Country</label>
 *   <span id="error-message-country" class="usa-error-message" role="alert" hidden="hidden"></span>
 *   <select class="usa-select" id="country" name="country">
 *     <option value="">--Select a country--</option>
 *     <option value="us">United States</option>
 *     <option value="ca">Canada</option>
 *   </select>
 * </div>
 * }</pre>
 *
 * @see <a href="http://localhost:9000/dev/component/catalog/select">Select Component - Component
 *     Catalog</a>
 */
public final class SelectElementTagModelProcessor extends AbstractElementModelProcessor {

  private static final String TAG_NAME = "select";
  private static final int PRECEDENCE = 1000;

  public SelectElementTagModelProcessor(final String dialectPrefix) {
    super(
        TemplateMode.HTML,
        dialectPrefix, // Prefix for <cf:select>
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

    SelectFormSettings elementSettings =
        SelectFormSettings.builder()
            .id(getAttributeInfo(context, tag, "id"))
            .name(getAttributeInfo(context, tag, "name"))
            .label(getAttributeInfo(context, tag, "label"))
            .helpText(getAttributeInfo(context, tag, "help-text"))
            .validationMessage(getAttributeInfo(context, tag, "validation-message"))
            .value(getAttributeInfo(context, tag, "value"))
            .size(getAttributeInfo(context, tag, "size"))
            .isValid(getAttributeInfo(context, tag, "is-valid", "true"))
            .required(getAttributeInfo(context, tag, "required"))
            .validationClass(getAttributeInfo(context, tag, "validation-class"))
            .validationField(getAttributeInfo(context, tag, "validation-field"))
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
    addSelectElement(elementSettings, targetModel, modelFactory, sourceModel);

    // Add closing div tag
    targetModel.add(modelFactory.createCloseElementTag("div"));

    // Replace the entire original model with our new model
    sourceModel.reset();
    sourceModel.addModel(targetModel);
  }

  private static void addSelectElement(
      SelectFormSettings elementSettings,
      IModel targetModel,
      IModelFactory modelFactory,
      IModel sourceModel) {
    // Build select attributes
    var selectAttrs = new BetterLinkedHashMap<String, String>();

    // Attribute: class
    String selectCssClass =
        BetterStringJoiner.withSpaceDelimiter()
            .add("usa-select")
            .addIf(isNotBlank(elementSettings.size().value()), elementSettings.sizeCssClass())
            .toString();

    selectAttrs.put("class", selectCssClass);

    // Attribute: id
    selectAttrs.put("id", elementSettings.id().value());

    // Attribute: name
    selectAttrs.put("name", elementSettings.name().value());

    // Attribute: required
    selectAttrs.put(buildBooleanAttribute("required", elementSettings.required()));

    // Attribute: readonly
    selectAttrs.put(buildBooleanAttribute("readonly", elementSettings.readonly()));

    // Attribute: disabled
    selectAttrs.put(buildBooleanAttribute("disabled", elementSettings.disabled()));

    // Attribute: aria-describedby
    var ariaDescribedByIds = getAriaDescribedByIds(elementSettings);

    selectAttrs.putIf(
        ariaDescribedByIds.length() > 0, "aria-describedby", ariaDescribedByIds.toString());

    // Attribute: aria-invalid
    selectAttrs.putIf(!elementSettings.isValid().valueAsBoolean(), "aria-invalid", "true");

    // Fill any other data-* and aria-* attributes
    selectAttrs.putAll(getDataAndAriaAttributes(elementSettings.attributeMap()));

    // Set up validation databinding
    if (elementSettings.useValidationDataBinding()) {
      selectAttrs.putAll(getValidationDataBindingAttributes(elementSettings));
    }

    // Add opening select tag
    targetModel.add(modelFactory.createOpenElementTag("select", selectAttrs, null, false));

    // Extract and preserve the body content (option elements, etc.)
    // Skip the first (opening tag) and last (closing tag) events
    for (int i = 1; i < sourceModel.size() - 1; i++) {
      targetModel.add(sourceModel.get(i));
    }

    // Add self-closing input tag
    targetModel.add(modelFactory.createCloseElementTag("select"));
  }
}
