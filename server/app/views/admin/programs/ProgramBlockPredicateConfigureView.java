package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.option;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.LabelTag;
import j2html.tags.specialized.OptionTag;
import java.util.Arrays;
import javax.inject.Inject;
import play.data.DynamicForm;
import play.mvc.Http;
import play.twirl.api.Content;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.predicate.Operator;
import services.question.QuestionOption;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.SelectWithLabel;
import views.style.BaseStyles;
import views.style.ReferenceClasses;

public final class ProgramBlockPredicateConfigureView extends ProgramBlockView {

  public enum TYPE {
    ELIGIBILITY,
    VISIBILITY
  }

  private final AdminLayout layout;

  @Inject
  public ProgramBlockPredicateConfigureView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.PROGRAMS);
  }

  public Content renderVisibility(
      Http.Request request,
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      DynamicForm form) {
    return render(request, programDefinition, blockDefinition, form, TYPE.VISIBILITY);
  }

  public Content renderEligibility(
      Http.Request request,
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      DynamicForm form) {
    return render(request, programDefinition, blockDefinition, form, TYPE.ELIGIBILITY);
  }

  public Content render(
      Http.Request request,
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      DynamicForm form,
      TYPE type) {
    final String editPredicateUrl;
    final String typeDisplayName;

    switch (type) {
      case ELIGIBILITY:
        {
          typeDisplayName = "eligibility";
          //        editPredicateUrl =
          // routes.AdminProgramBlockPredicatesController.editEligibility(programDefinition.id(),
          // blockDefinition.id())
          //          .url();
          break;
        }
      case VISIBILITY:
        {
          typeDisplayName = "visibility";
          //        editPredicateUrl =
          // routes.AdminProgramBlockPredicatesController.edit(programDefinition.id(),
          // blockDefinition.id())
          //          .url();
          break;
        }
      default:
        {
          throw new RuntimeException("Unknown predicate view type");
        }
    }
    editPredicateUrl = "foo";

    ImmutableList<QuestionDefinition> questionDefinitions =
        getQuestionDefinitions(programDefinition, blockDefinition, form);

    DivTag content =
        div(
                new LinkElement()
                    .setHref(editPredicateUrl)
                    .setText(
                        String.format(
                            "Return to %s conditions for %s screen",
                            typeDisplayName, blockDefinition.name()))
                    .setStyles("my-6")
                    .asAnchorText(),
                h2("Configure eligibility conditions").withClasses("my-6"),
                each(questionDefinitions, this::renderQuestionCard),
                renderConditionGroup(questionDefinitions),
                submitButton("Save condition").withClasses("my-4"))
            .withClasses("max-w-6xl", "mb-12");

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle("Configure predicate")
            .addMainContent(renderProgramInfo(programDefinition), content);

    return layout.renderCentered(htmlBundle);
  }

  private DivTag renderConditionGroup(ImmutableList<QuestionDefinition> questionDefinitions) {
    DivTag container =
        div().withId("condition-group-template").withClasses("flex", "bg-gray-100", "py-4");
    DivTag rowContainer = div().withClasses("grow");

    var count = 1;
    for (var questionDefinition : questionDefinitions) {
      DivTag questionRow =
          div(
                  div(
                          div().withClasses("text-base", "p-2"),
                          div("And").withClasses("px-4").withCondClass(count++ == 1, "hidden"))
                      .withClasses("w-1/12", "pt-4"),
                  div(questionDefinition.getQuestionText().getDefault())
                      .withClasses("w-2/12", "pt-4"),
                  createScalarDropdown(questionDefinition).withClasses("w-2/12"),
                  createOperatorDropdown(questionDefinition).withClasses("w-2/12"),
                  createValueField(questionDefinition).withClasses("grow"))
              .withClasses("flex", "flex-row", "gap-2");

      rowContainer.with(questionRow);
    }

    DivTag delete =
        div(Icons.svg(Icons.DELETE).withClasses("w-8"))
            .attr("role", "button")
            .withClasses("place-self-center", "mx-6", "cursor-pointer");

    return container.with(rowContainer, delete);
  }

  private ImmutableList<QuestionDefinition> getQuestionDefinitions(
      ProgramDefinition programDefinition, BlockDefinition blockDefinition, DynamicForm form) {
    try {
      return programDefinition
          .getAvailablePredicateQuestionDefinitions(blockDefinition.id())
          .stream()
          .filter(
              questionDefinition ->
                  form.rawData()
                      .containsKey(String.format("question-%d", questionDefinition.getId())))
          .collect(ImmutableList.toImmutableList());
    } catch (ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private DivTag renderQuestionCard(QuestionDefinition questionDefinition) {
    String questionHelpText =
        questionDefinition.getQuestionHelpText().isEmpty()
            ? ""
            : questionDefinition.getQuestionHelpText().getDefault();

    return div()
        .withClasses("my-4", "p-4", "flex", "flex-row", "gap-4", "border", "border-gray-300")
        .with(
            div(Icons.questionTypeSvg(questionDefinition.getQuestionType())
                    .withClasses("shrink-0", "h-12", "w-6"))
                .withClasses("flex", "items-center"))
        .with(
            div()
                .withClasses("text-left")
                .with(
                    div(questionDefinition.getQuestionText().getDefault()).withClasses("font-bold"),
                    div(questionHelpText).withClasses("mt-1", "text-sm"),
                    div(String.format("Admin ID: %s", questionDefinition.getName()))
                        .withClasses("mt-1", "text-sm")));
  }

  private DivTag createScalarDropdown(QuestionDefinition questionDefinition) {
    ImmutableSet<Scalar> scalars;
    try {
      scalars = Scalar.getScalars(questionDefinition.getQuestionType());
    } catch (InvalidQuestionTypeException | UnsupportedQuestionTypeException e) {
      // This should never happen since we filter out Enumerator questions before this point.
      return div()
          .withText("Sorry, you cannot create a show/hide predicate with this question type.");
    }

    ImmutableList<OptionTag> options =
        scalars.stream()
            .map(
                scalar ->
                    option(scalar.toDisplayString())
                        .withValue(scalar.name())
                        // Add the scalar type as data so we can determine which operators to allow.
                        .withData("type", scalar.toScalarType().name().toLowerCase()))
            .collect(toImmutableList());

    var selectElement =
        new SelectWithLabel()
            .setFieldName("scalar")
            .setLabelText("Field")
            .setCustomOptions(options)
            .addReferenceClass(ReferenceClasses.PREDICATE_SCALAR_SELECT);

    if (scalars.size() == 1) {
      options.stream().findFirst().get().isSelected();
    }

    return selectElement.getSelectTag();
  }

  private DivTag createOperatorDropdown(QuestionDefinition questionDefinition) {
    ImmutableList<OptionTag> operatorOptions =
        Arrays.stream(Operator.values())
            .map(
                operator -> {
                  // Add this operator's allowed scalar types as data, so that we can determine
                  // whether to show or hide each operator based on the current type of scalar
                  // selected.
                  OptionTag option = option(operator.toDisplayString()).withValue(operator.name());
                  operator
                      .getOperableTypes()
                      .forEach(type -> option.withData(type.name().toLowerCase(), ""));
                  return option;
                })
            .collect(toImmutableList());

    return new SelectWithLabel()
        .setFieldName(
            String.format("group-GROUP_ID-question-%d-operator", questionDefinition.getId()))
        .setLabelText("Operator")
        .setCustomOptions(operatorOptions)
        .addReferenceClass(ReferenceClasses.PREDICATE_OPERATOR_SELECT)
        .getSelectTag();
  }

  private DivTag createValueField(QuestionDefinition questionDefinition) {
    if (questionDefinition.getQuestionType().isMultiOptionType()) {
      // If it's a multi-option question, we need to provide a discrete list of possible values to
      // choose from instead of a freeform text field. Not only is it a better UX, but we store the
      // ID of the options rather than the display strings since the option display strings are
      // localized.
      ImmutableList<QuestionOption> options =
          ((MultiOptionQuestionDefinition) questionDefinition).getOptions();
      DivTag valueOptionsDiv =
          div().with(div("Values").withClasses(BaseStyles.CHECKBOX_GROUP_LABEL));
      for (QuestionOption option : options) {
        LabelTag optionCheckbox =
            FieldWithLabel.checkbox()
                .setFieldName(
                    String.format(
                        "group-GROUP_ID-question-%d-predicateValues[]", questionDefinition.getId()))
                .setValue(String.valueOf(option.id()))
                .setLabelText(option.optionText().getDefault())
                .getCheckboxTag();
        valueOptionsDiv.with(optionCheckbox);
      }
      return valueOptionsDiv;
    } else {
      return div()
          .with(
              FieldWithLabel.input()
                  .setFieldName(
                      String.format(
                          "group-GROUP_ID-question-%d-predicateValue", questionDefinition.getId()))
                  .setLabelText("Value")
                  .addReferenceClass(ReferenceClasses.PREDICATE_VALUE_INPUT)
                  .getInputTag())
          .with(
              div()
                  .withClasses(
                      ReferenceClasses.PREDICATE_VALUE_COMMA_HELP_TEXT,
                      "hidden",
                      "text-xs",
                      BaseStyles.FORM_LABEL_TEXT_COLOR)
                  .withText("Enter a list of comma-separated values. For example, \"v1,v2,v3\"."));
    }
  }
}
