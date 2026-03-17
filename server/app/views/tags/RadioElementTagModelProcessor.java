package views.tags;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.http.util.TextUtils.isBlank;

import helpers.BetterLinkedHashMap;
import helpers.BetterStringJoiner;
import java.util.ArrayList;
import java.util.Map;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;
import views.tags.settings.FormSettings;
import views.tags.settings.ItemFormSettings;
import views.tags.settings.RadioFormSettings;

/**
 * Custom Thymeleaf element that will render into radio button(s) with all appropriate HTML to be
 * structured correctly in terms of labels, validation messages, etc
 *
 * <p>Sample Thymeleaf:
 *
 * <pre>{@code <cf:radio name="notifications" label="Notifications">
 *   <item id="email" value="email" label="Email" />
 * </cf:radio>}</pre>
 *
 * <p>Sample rendered HTML:
 *
 * <pre>{@code <div class="usa-form-group">
 *   <fieldset class="usa-fieldset">
 *     <legend class="usa-legend">Contact Method</legend>
 *     <span id="error-message-contact" class="usa-error-message" role="alert" hidden="hidden"></span>
 *     <div class="usa-radio">
 *       <input type="radio" class="usa-radio__input usa-radio__input--tile" id="email" name="contact" value="email" />
 *       <label class="usa-radio__label" for="email">Email</label>
 *     </div>
 *   </fieldset>
 * </div>
 * }</pre>
 *
 * @see <a href="http://localhost:9000/dev/component/catalog/radio">Radio Component - Component
 *     Catalog</a>
 */
public final class RadioElementTagModelProcessor extends AbstractElementModelProcessor {

  private static final String TAG_NAME = "radio";
  private static final int PRECEDENCE = 900;

  public RadioElementTagModelProcessor(final String dialectPrefix) {
    super(
        TemplateMode.HTML,
        dialectPrefix, // Prefix for <cf:radio>
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

    RadioFormSettings elementSettings =
        RadioFormSettings.builder()
            .id(getAttributeInfo(context, tag, "id"))
            .name(getAttributeInfo(context, tag, "name"))
            .label(getAttributeInfo(context, tag, "label"))
            .helpText(getAttributeInfo(context, tag, "help-text"))
            .validationMessage(getAttributeInfo(context, tag, "validation-message"))
            .value(getAttributeInfo(context, tag, "value"))
            .size(getAttributeInfo(context, tag, "size"))
            .isValid(getAttributeInfo(context, tag, "is-valid", "true"))
            .required(getAttributeInfo(context, tag, "required"))
            .tiled(getAttributeInfo(context, tag, "tiled", "true"))
            .validationClass(getAttributeInfo(context, tag, "validation-class"))
            .validationField(getAttributeInfo(context, tag, "validation-field"))
            .readonly(getAttributeInfo(context, tag, "readonly", "false"))
            .disabled(getAttributeInfo(context, tag, "disabled", "false"))
            .attributeMap(tag.getAttributeMap())
            .build();

    elementSettings.validate(() -> getTemplateHtml(sourceModel));

    var itemSettingsList = new ArrayList<ItemFormSettings>();

    // Process child item elements
    // Skip the first (opening tag) and last (closing tag) events. These are
    // the parent/current html elements, and we only want the children.
    for (int i = 1; i < sourceModel.size() - 1; i++) {
      if (sourceModel.get(i) instanceof IProcessableElementTag itemTag) {
        // Check if this is an <item> element. If it is not skip it as it isn't allowed.
        if (!"item".equalsIgnoreCase(itemTag.getElementCompleteName())) {
          continue;
        }

        itemSettingsList.add(
            ItemFormSettings.builder()
                .thEach(getAttributeInfo(context, itemTag, "th:each"))
                .id(getAttributeInfo(context, itemTag, "id"))
                .value(getAttributeInfo(context, itemTag, "value"))
                .label(getAttributeInfo(context, itemTag, "label"))
                .description(getAttributeInfo(context, itemTag, "description"))
                .selected(getAttributeInfo(context, itemTag, "selected"))
                .disabled(getAttributeInfo(context, itemTag, "disabled"))
                .attributeMap(itemTag.getAttributeMap())
                .build());
      }
    }

    var itemValidationResults =
        itemSettingsList.stream()
            .map(ItemFormSettings::validate)
            .filter(x -> !x.valid())
            .map(ValidationContext::message)
            .map(StringBuilder::toString)
            .findAny()
            .orElse("");

    if (isNotBlank(itemValidationResults)) {
      throw new IllegalStateException(itemValidationResults);
    }

    // ---------------------

    // Add wrapping div with form-group classes
    var divCssClasses =
        BetterStringJoiner.withSpaceDelimiter()
            .add("usa-form-group")
            .addIf(!elementSettings.isValid().valueAsBoolean(), "usa-form-group--error");

    var divAttrs = new BetterLinkedHashMap<String, String>();
    divAttrs.put("class", divCssClasses.toString());

    targetModel.add(modelFactory.createOpenElementTag("div", divAttrs, null, false));

    // Add fieldset
    var fieldsetAttrs = new BetterLinkedHashMap<String, String>();
    fieldsetAttrs.put("class", "usa-fieldset");

    fieldsetAttrs.putIf(
        isNotBlank(elementSettings.validationMessage().value()),
        "data-required-message",
        elementSettings.validationMessage().value());

    // Add wrapping fieldset
    targetModel.add(modelFactory.createOpenElementTag("fieldset", fieldsetAttrs, null, false));

    addLegendElement(elementSettings, targetModel, modelFactory);
    addHelpTextDivElement(elementSettings, targetModel, modelFactory);
    addValidationSpanElement(elementSettings, targetModel, modelFactory);

    // Process each item (radio)
    for (var itemSettings : itemSettingsList) {
      var radioDivAttrs = new BetterLinkedHashMap<String, String>();
      radioDivAttrs.put("class", "usa-radio");
      radioDivAttrs.putIf(
          isNotBlank(itemSettings.thEach().value()), "th:each", itemSettings.thEach().value());

      // Add usa-radio div
      targetModel.add(modelFactory.createOpenElementTag("div", radioDivAttrs, null, false));

      addRadioInputElement(itemSettings, elementSettings, targetModel, modelFactory);
      addRadioLabelElement(itemSettings, targetModel, modelFactory);

      // Close usa-radio div
      targetModel.add(modelFactory.createCloseElementTag("div"));
    }

    // Close fieldset
    targetModel.add(modelFactory.createCloseElementTag("fieldset"));

    // Close wrapping div
    targetModel.add(modelFactory.createCloseElementTag("div"));

    // Replace the entire original model with our new model
    sourceModel.reset();
    sourceModel.addModel(targetModel);
  }

  private static void addRadioInputElement(
      ItemFormSettings itemSettings,
      RadioFormSettings elementSettings,
      IModel targetModel,
      IModelFactory modelFactory) {
    // Build radio input attributes
    var inputAttrs = new BetterLinkedHashMap<String, String>();

    // Attribute: type
    inputAttrs.put("type", "radio");

    // Attribute: class
    String inputCssClasses =
        BetterStringJoiner.withSpaceDelimiter()
            .add("usa-radio__input")
            .addIf(elementSettings.tiled().valueAsBoolean(), "usa-radio__input--tile")
            .addIf(!elementSettings.isValid().valueAsBoolean(), "usa-radio__input--error")
            .toString();

    inputAttrs.put("class", inputCssClasses);

    // Attribute: id
    inputAttrs.putIf(
        isNotBlank(itemSettings.id().value()),
        itemSettings.id().attributeName(),
        itemSettings.id().value());

    // Attribute: name
    inputAttrs.put(elementSettings.name().attributeName(), elementSettings.name().value());

    // Attribute: value
    inputAttrs.putIf(
        isNotBlank(itemSettings.value().value()),
        itemSettings.value().attributeName(),
        itemSettings.value().value());

    // Attribute: checked
    inputAttrs.put(buildBooleanAttribute("checked", itemSettings.selected()));

    // Attribute: disabled
    inputAttrs.put(buildBooleanAttribute("disabled", itemSettings.disabled()));

    // Attribute: required
    inputAttrs.put(buildBooleanAttribute("required", elementSettings.required()));

    // Add ARIA attributes if showing errors
    if (!elementSettings.isValid().valueAsBoolean()) {
      inputAttrs.put("aria-describedby", elementSettings.errorMessageId());
      inputAttrs.put("aria-invalid", "true");
    }

    inputAttrs.putAll(getDataAndAriaAttributes(itemSettings.attributeMap()));

    // Add radio input (self-closing)
    targetModel.add(
        modelFactory.createStandaloneElementTag("input", inputAttrs, null, false, true));
  }

  private static void addRadioLabelElement(
      ItemFormSettings itemSettings, IModel targetModel, IModelFactory modelFactory) {
    // Add label for radio
    var labelAttrs = new BetterLinkedHashMap<String, String>();
    labelAttrs.put("class", "usa-radio__label");

    labelAttrs.putIf(
        isNotBlank(itemSettings.id().value()),
        itemSettings.id().isThymeleafAttribute() ? "th:for" : "for",
        itemSettings.id().value());

    // Add label
    targetModel.add(modelFactory.createOpenElementTag("label", labelAttrs, null, false));

    if (itemSettings.label().isThymeleafAttribute()) {
      var spanAttrs = Map.of("th:text", itemSettings.label().value());
      targetModel.add(
          modelFactory.createStandaloneElementTag("th:block", spanAttrs, null, false, false));
    } else {
      targetModel.add(modelFactory.createText(itemSettings.label().value()));
    }

    // Add description span if provided
    addDescriptionSpanElement(itemSettings, targetModel, modelFactory);

    // Close label
    targetModel.add(modelFactory.createCloseElementTag("label"));
  }

  private static void addDescriptionSpanElement(
      ItemFormSettings itemSettings, IModel targetModel, IModelFactory modelFactory) {
    if (isBlank(itemSettings.description().value())) {
      return;
    }

    var descAttrs = Map.of("class", "usa-radio__label-description");
    targetModel.add(modelFactory.createOpenElementTag("span", descAttrs, null, false));

    if (itemSettings.description().isThymeleafAttribute()) {
      var descTextAttrs = Map.of("th:text", itemSettings.description().value());
      targetModel.add(
          modelFactory.createStandaloneElementTag("th:block", descTextAttrs, null, false, false));
    } else {
      targetModel.add(modelFactory.createText(itemSettings.description().value()));
    }

    targetModel.add(modelFactory.createCloseElementTag("span"));
  }

  private static void addLegendElement(
      FormSettings elementSettings, IModel targetModel, IModelFactory modelFactory) {
    // Add legend if labelText is provided
    var legendAttrs = Map.of("class", "usa-legend");
    targetModel.add(modelFactory.createOpenElementTag("legend", legendAttrs, null, false));
    targetModel.add(modelFactory.createText(elementSettings.label().value()));
    targetModel.add(modelFactory.createCloseElementTag("legend"));
  }
}
