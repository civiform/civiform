package views.tags;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.http.util.TextUtils.isBlank;

import helpers.BetterLinkedHashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
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
 * Processor for <cf:radio> that: - Wraps everything in a form-group div with error styling (if
 * is-valid="false") - Renders a <fieldset> wrapper - Renders a <legend> with the given label text -
 * Renders optional help text div (if help-text attribute provided) - Renders error message span (if
 * is-valid="false") - Processes nested <item> elements to create radio inputs with their labels -
 * Each radio button is wrapped in a usa-radio div - Uses the tile variant (usa-radio__input--tile)
 * by default, or if tiled="true" - Supports required and disabled attributes - Supports label
 * descriptions - Handles both regular and 'th:' prefixed attributes - Only copies data-* attributes
 * to the radio inputs
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
            .placeholder(getAttributeInfo(context, tag, "placeholder"))
            .size(getAttributeInfo(context, tag, "size"))
            .isValid(getAttributeInfo(context, tag, "is-valid", "true"))
            .required(getAttributeInfo(context, tag, "required", "false"))
            .tiled(getAttributeInfo(context, tag, "tiled", "true"))
            .columns(getAttributeInfo(context, tag, "columns"))
            .validationClass(getAttributeInfo(context, tag, "validation-class"))
            .validationField(getAttributeInfo(context, tag, "validation-field"))
            .attributeMap(tag.getAttributeMap())
            .build();

    elementSettings.validate(() -> getTemplateHtml(sourceModel));

    // ---------------------
    var itemSettingsList = new ArrayList<ItemFormSettings>();

    // Process child item elements
    // Skip the first (opening tag) and last (closing tag) events
    for (int i = 1; i < sourceModel.size() - 1; i++) {
      if (sourceModel.get(i) instanceof IProcessableElementTag itemTag) {
        // Check if this is an opening item tag
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


//    var valres1 = itemSettingsList.stream()
//      .map(ItemFormSettings::validate)
//      .filter(x -> !x.valid())
//      .toList();

      //.map(ValidationContext::message);

    var valres = itemSettingsList.stream()
      .map(ItemFormSettings::validate)
      .filter(x -> !x.valid())
      .map(ValidationContext::message)
      .map(StringBuilder::toString)
      .findAny()
      .orElse("");

    if (isNotBlank(valres)) {
      throw new IllegalStateException(valres);
    }

    // ---------------------

    // Add wrapping div with form-group classes
    var divAttrs =
        Map.of(
            "class",
            elementSettings.isValid().valueAsBoolean()
                ? "usa-form-group"
                : "usa-form-group usa-form-group--error");
    targetModel.add(modelFactory.createOpenElementTag("div", divAttrs, null, false));

    // Add fieldset
    Map<String, String> fieldsetAttrs = new LinkedHashMap<>();
    fieldsetAttrs.put("class", "usa-fieldset");

    if (isNotBlank(elementSettings.validationMessage().value())) {
      fieldsetAttrs.put("data-required-message", elementSettings.validationMessage().value());
    }

    targetModel.add(modelFactory.createOpenElementTag("fieldset", fieldsetAttrs, null, false));

    addLegendElement(elementSettings, targetModel, modelFactory);
    addHelpTextDivElement(elementSettings, targetModel, modelFactory);
    addValidationSpanElement(elementSettings, targetModel, modelFactory);

    for (var itemSettings : itemSettingsList) {
      Map<String, String> radioDivAttrs = new LinkedHashMap<>();
      radioDivAttrs.put("class", "usa-radio");
      radioDivAttrs.computeIfAbsent(
          "th:each",
          k -> isNotBlank(itemSettings.thEach().value()) ? itemSettings.thEach().value() : null);

      targetModel.add(modelFactory.createOpenElementTag("div", radioDivAttrs, null, false));

      addRadioInputElement(itemSettings, elementSettings, targetModel, modelFactory);
      addRadioLabelElement(itemSettings, targetModel, modelFactory);

      // Close radio wrapper div
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

    // Build input class with optional tile variant and error modifier
    var inputClasses = new StringJoiner(" ");
    inputClasses.add("usa-radio__input");

    if (elementSettings.tiled().valueAsBoolean()) {
      inputClasses.add("usa-radio__input--tile");
    }

    var inputAttrs = new BetterLinkedHashMap<String, String>();
    inputAttrs.put("type", "radio");
    inputAttrs.put("class", inputClasses.toString());

    if (isNotBlank(itemSettings.id().value())) {
      inputAttrs.put(
          itemSettings.id().isThymeleafAttribute() ? "th:id" : "id", itemSettings.id().value());
    }

    inputAttrs.put("name", elementSettings.name().value());

    if (isNotBlank(itemSettings.value().value())) {
      inputAttrs.put(
          itemSettings.value().isThymeleafAttribute() ? "th:value" : "value",
          itemSettings.value().value());
    }

    inputAttrs.put(buildBooleanAttribute("checked", itemSettings.selected()));
    inputAttrs.put(buildBooleanAttribute("disabled", itemSettings.disabled()));
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
    // Add label for checkbox
    Map<String, String> labelAttrs = new LinkedHashMap<>();
    labelAttrs.put("class", "usa-radio__label");

    if (itemSettings.id().isThymeleafAttribute() && isNotBlank(itemSettings.id().value())) {
      labelAttrs.put("th:for", itemSettings.id().value());
    } else if (isNotBlank(itemSettings.id().value())) {
      labelAttrs.put("for", itemSettings.id().value());
    }

    targetModel.add(modelFactory.createOpenElementTag("label", labelAttrs, null, false));

    if (itemSettings.label().isThymeleafAttribute() && isNotBlank(itemSettings.label().value())) {
      var spanAttrs = Map.of("th:text", itemSettings.label().value());
      targetModel.add(
          modelFactory.createStandaloneElementTag("th:block", spanAttrs, null, false, false));
    } else if (isNotBlank(itemSettings.label().value())) {
      targetModel.add(modelFactory.createText(itemSettings.label().value()));
    }

    // Add description span if provided
    addDescriptionSpanElement(itemSettings, targetModel, modelFactory);

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
