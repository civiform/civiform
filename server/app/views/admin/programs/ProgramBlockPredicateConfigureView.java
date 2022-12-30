package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.input;
import static j2html.TagCreator.option;
import static j2html.TagCreator.select;
import static j2html.TagCreator.text;
import static play.mvc.Http.HttpVerbs.POST;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import controllers.admin.routes;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.LabelTag;
import j2html.tags.specialized.OptionTag;
import j2html.tags.specialized.SelectTag;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import play.mvc.Http;
import play.twirl.api.Content;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.predicate.AndNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
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

  private static final String COLUMN_WIDTH = "w-48";

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
      ImmutableList<QuestionDefinition> questionDefinitions) {
    return render(
        request, programDefinition, blockDefinition, questionDefinitions, TYPE.VISIBILITY);
  }

  public Content renderEligibility(
      Http.Request request,
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      ImmutableList<QuestionDefinition> questionDefinitions) {
    return render(
        request, programDefinition, blockDefinition, questionDefinitions, TYPE.ELIGIBILITY);
  }

  public Content render(
      Http.Request request,
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      ImmutableList<QuestionDefinition> questionDefinitions,
      TYPE type) {
    final String editPredicateUrl;
    final String typeDisplayName;
    final String formActionUrl;
    final Optional<PredicateDefinition> existingPredicate;

    switch (type) {
      case ELIGIBILITY:
        {
          typeDisplayName = "eligibility";
          existingPredicate =
              blockDefinition.eligibilityDefinition().map(EligibilityDefinition::predicate);
          formActionUrl =
              routes.AdminProgramBlockPredicatesController.updateEligibility(
                      programDefinition.id(), blockDefinition.id())
                  .url();
          editPredicateUrl =
              routes.AdminProgramBlockPredicatesController.editEligibility(
                      programDefinition.id(), blockDefinition.id())
                  .url();
          break;
        }
      case VISIBILITY:
        {
          typeDisplayName = "visibility";
          existingPredicate = blockDefinition.visibilityPredicate();
          formActionUrl =
              routes.AdminProgramBlockPredicatesController.updateVisibility(
                      programDefinition.id(), blockDefinition.id())
                  .url();
          editPredicateUrl =
              routes.AdminProgramBlockPredicatesController.editVisibility(
                      programDefinition.id(), blockDefinition.id())
                  .url();
          break;
        }
      default:
        {
          throw new RuntimeException("Unknown predicate view type");
        }
    }

    DivTag content =
        div(
                renderBackLink(editPredicateUrl, typeDisplayName, blockDefinition),
                h2(String.format("Configure %s conditions", typeDisplayName)).withClasses("my-6"),
                each(questionDefinitions, this::renderQuestionCard),
                renderPredicateConfigurator(
                    request, formActionUrl, questionDefinitions, existingPredicate, type))
            .withClasses("max-w-6xl", "mb-12");

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(String.format("Configure %s predicate", typeDisplayName))
            .addMainContent(renderProgramInfo(programDefinition), content);

    return layout.renderCentered(htmlBundle);
  }

  private ATag renderBackLink(
      String editPredicateUrl, String typeDisplayName, BlockDefinition blockDefinition) {
    return new LinkElement()
        .setHref(editPredicateUrl)
        .setIcon(Icons.ARROW_LEFT)
        .setText(
            String.format(
                "Return to %s conditions for %s screen", typeDisplayName, blockDefinition.name()))
        .setStyles("my-6")
        .asAnchorText();
  }

  private FormTag renderPredicateConfigurator(
      Http.Request request,
      String formAction,
      ImmutableList<QuestionDefinition> questionDefinitions,
      Optional<PredicateDefinition> maybeExistingPredicate,
      TYPE type) {
    FormTag formTag =
        form(
                makeCsrfTokenInputTag(request),
                type.equals(TYPE.ELIGIBILITY)
                    ? renderConfiguratorEligibilityHeader()
                    : renderConfiguratorVisibilityHeader(maybeExistingPredicate),
                renderQuestionHeaders(questionDefinitions, maybeExistingPredicate))
            .withAction(formAction)
            .withMethod(POST)
            .withClasses(
                "p-8", "bg-gray-100", "predicate-config-form", ReferenceClasses.PREDICATE_OPTIONS);

    DivTag valueRowContainer = div().withId("predicate-config-value-row-container");

    if (maybeExistingPredicate.isPresent()) {
      PredicateDefinition existingPredicate = maybeExistingPredicate.get();

      AtomicInteger groupCount = new AtomicInteger(0);
      getExistingAndNodes(existingPredicate).stream()
          .map(PredicateExpressionNode::getAndNode)
          .forEach(
              andNode ->
                  valueRowContainer.with(
                      renderValueRow(
                          questionDefinitions,
                          groupCount.incrementAndGet(),
                          Optional.of(andNode))));
    } else {
      valueRowContainer.with(
          renderValueRow(questionDefinitions, 1, /* maybeAndNode= */ Optional.empty()));
    }

    formTag.with(valueRowContainer);
    formTag.with(div(makeSvgTextButton("Add values", Icons.ADD).withId("predicate-add-value-set")));
    formTag.with(submitButton("Save condition").withClasses("my-4"));

    return formTag;
  }

  private static DivTag renderConfiguratorEligibilityHeader() {
    return div(
            text("Applicant is eligible when:"),
            input()
                .withType("hidden")
                .withName("predicateAction")
                .withValue(PredicateAction.ELIGIBLE_BLOCK.name()))
        .withClasses("font-bold");
  }

  private static DivTag renderConfiguratorVisibilityHeader(
      Optional<PredicateDefinition> maybeExistingPredicate) {
    DivTag container = div("Screen is ").withClasses("font-bold");
    SelectTag selectTag =
        select().withName("predicateAction").withClasses(ReferenceClasses.PREDICATE_ACTION);

    ImmutableList.of(PredicateAction.HIDE_BLOCK, PredicateAction.SHOW_BLOCK)
        .forEach(
            action ->
                selectTag.with(
                    option(action.toDisplayString())
                        .withClasses(
                            ReferenceClasses.MULTI_OPTION_QUESTION_OPTION,
                            ReferenceClasses.MULTI_OPTION_VALUE)
                        .withCondSelected(
                            maybeExistingPredicate
                                .map(PredicateDefinition::action)
                                .map(existingAction -> existingAction.equals(action))
                                .orElse(false))
                        .withValue(action.name())));

    container.with(selectTag);

    return container;
  }

  private ImmutableList<PredicateExpressionNode> getExistingAndNodes(
      PredicateDefinition existingPredicate) {
    switch (existingPredicate.computePredicateFormat()) {
      case SINGLE_QUESTION:
        {
          return ImmutableList.of(
              PredicateExpressionNode.create(
                  AndNode.create(ImmutableList.of(existingPredicate.rootNode()))));
        }

      case SINGLE_LAYER_AND:
        {
          return existingPredicate.rootNode().getOrNode().children();
        }

      default:
        {
          throw new RuntimeException(
              String.format(
                  "Unrecognized predicate format: %s", existingPredicate.computePredicateFormat()));
        }
    }
  }

  private DivTag renderQuestionHeaders(
      ImmutableList<QuestionDefinition> questionDefinitions,
      Optional<PredicateDefinition> maybeExistingPredicate) {
    DivTag container = div().withClasses("flex", "py-4");
    int columnNumber = 1;

    if (maybeExistingPredicate.isPresent()) {
      ImmutableList<LeafOperationExpressionNode> leafNodes =
          getExistingAndNodes(maybeExistingPredicate.get()).stream()
              .findFirst()
              .get()
              .getAndNode()
              .children()
              .stream()
              .map(PredicateExpressionNode::getLeafNode)
              .sorted(
                  (LeafOperationExpressionNode loenA, LeafOperationExpressionNode loenB) ->
                      (int) (loenA.questionId() - loenB.questionId()))
              .collect(ImmutableList.toImmutableList());

      for (LeafOperationExpressionNode leafNode : leafNodes) {
        long questionId = leafNode.questionId();
        QuestionDefinition qd =
            questionDefinitions.stream()
                .filter(questionDefinition -> questionDefinition.getId() == questionId)
                .findFirst()
                .get();

        container.with(
            div(
                    div(qd.getQuestionText().getDefault())
                        .withClasses(BaseStyles.INPUT, "text-gray-500", "mb-2", "truncate"),
                    createScalarDropdown(qd, Optional.of(leafNode.scalar())),
                    createOperatorDropdown(
                        qd, Optional.of(leafNode.scalar()), Optional.of(leafNode.operator())))
                .withClasses(COLUMN_WIDTH, columnNumber++ != 1 ? "ml-16" : null));
      }
    } else {
      for (var qd : questionDefinitions) {
        container.with(
            div(
                    div(qd.getQuestionText().getDefault())
                        .withClasses(BaseStyles.INPUT, "text-gray-500", "mb-2", "truncate"),
                    createScalarDropdown(qd, /* maybeScalar */ Optional.empty()),
                    createOperatorDropdown(
                        qd,
                        /* maybeSelectedScalar */ Optional.empty(),
                        /* maybeOperator */ Optional.empty()))
                .withClasses(COLUMN_WIDTH, columnNumber++ != 1 ? "ml-16" : null));
      }
    }

    return container.with(div().withClasses("w-28"));
  }

  private DivTag renderValueRow(
      ImmutableList<QuestionDefinition> questionDefinitions,
      int groupId,
      Optional<AndNode> maybeAndNode) {
    DivTag row = div().withClasses("flex", "mb-6", "predicate-config-value-row");
    DivTag andText = div("and").withClasses("object-center", "w-16", "p-4", "leading-10");
    int columnNumber = 1;

    if (maybeAndNode.isPresent()) {
      ImmutableList<LeafOperationExpressionNode> leafNodes =
          maybeAndNode.get().children().stream()
              .map(PredicateExpressionNode::getLeafNode)
              .sorted(
                  (LeafOperationExpressionNode loenA, LeafOperationExpressionNode loenB) ->
                      (int) (loenA.questionId() - loenB.questionId()))
              .collect(ImmutableList.toImmutableList());

      for (LeafOperationExpressionNode leafNode : leafNodes) {
        long questionId = leafNode.questionId();
        QuestionDefinition qd =
            questionDefinitions.stream()
                .filter(questionDefinition -> questionDefinition.getId() == questionId)
                .findFirst()
                .get();

        row.condWith(columnNumber++ != 1, andText)
            .with(
                createValueField(
                    qd,
                    groupId,
                    Optional.of(leafNode.scalar()),
                    Optional.of(leafNode.comparedValue())));
      }
    } else {
      for (var questionDefinition : questionDefinitions) {
        row.condWith(columnNumber++ != 1, andText)
            .with(
                createValueField(
                    questionDefinition,
                    groupId,
                    /* maybeScalar= */ Optional.empty(),
                    /* maybePredicateValue= */ Optional.empty()));
      }
    }

    DivTag delete =
        div(Icons.svg(Icons.DELETE).withClasses("w-8", groupId == 1 ? "hidden" : null))
            .attr("role", "button")
            .withClasses(
                "predicate-config-delete-value-row", "mx-6", "w-12", "pt-2", "cursor-pointer");

    return row.with(delete);
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

  private DivTag createScalarDropdown(
      QuestionDefinition questionDefinition, Optional<Scalar> maybeScalar) {
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
                scalar -> {
                  OptionTag tag =
                      option(scalar.toDisplayString())
                          .withValue(scalar.name())
                          // Add the scalar type as data so we can determine which operators to
                          // allow.
                          .withData("type", scalar.toScalarType().name().toLowerCase());

                  if (maybeScalar.isPresent() && maybeScalar.get().name().equals(scalar.name())) {
                    tag.isSelected();
                  }

                  return tag;
                })
            .collect(toImmutableList());

    var selectElement =
        new SelectWithLabel()
            .setFieldName(String.format("question-%d-scalar", questionDefinition.getId()))
            .setCustomOptions(options)
            .addReferenceClass(ReferenceClasses.PREDICATE_SCALAR_SELECT);

    if (scalars.size() == 1) {
      options.stream().findFirst().get().isSelected();
    }

    return selectElement
        .getSelectTag()
        .withData("question-id", String.valueOf(questionDefinition.getId()));
  }

  private DivTag createOperatorDropdown(
      QuestionDefinition questionDefinition,
      Optional<Scalar> maybeSelectedScalar,
      Optional<Operator> maybeOperator) {
    try {
      ImmutableSet<Scalar> scalars = Scalar.getScalars(questionDefinition.getQuestionType());

      if (scalars.size() == 1) {
        maybeSelectedScalar = scalars.stream().findFirst();
      }
    } catch (InvalidQuestionTypeException | UnsupportedQuestionTypeException e) {
      throw new RuntimeException(e);
    }

    // Copying to a final variable because variables referenced within
    // lambdas must be final or effectively final.
    final Optional<Scalar> finalMaybeSelectedScalar = maybeSelectedScalar;
    ImmutableList<OptionTag> operatorOptions =
        Arrays.stream(Operator.values())
            .map(
                operator -> {
                  // Add this operator's allowed scalar types as data, so that we can determine
                  // whether to show or hide each operator based on the current type of scalar
                  // selected.
                  OptionTag optionTag =
                      option(operator.toDisplayString()).withValue(operator.name());
                  operator
                      .getOperableTypes()
                      .forEach(type -> optionTag.withData(type.name().toLowerCase(), ""));

                  if (maybeOperator.isPresent()
                      && operator.name().equals(maybeOperator.get().name())) {
                    optionTag.isSelected();
                  }

                  finalMaybeSelectedScalar.ifPresent(
                      selectedScalar -> {
                        if (!operator.getOperableTypes().contains(selectedScalar.toScalarType())) {
                          optionTag.withClass("hidden");
                        }
                      });

                  return optionTag;
                })
            .collect(toImmutableList());

    return new SelectWithLabel()
        .setFieldName(String.format("question-%d-operator", questionDefinition.getId()))
        .setCustomOptions(operatorOptions)
        .addReferenceClass(ReferenceClasses.PREDICATE_OPERATOR_SELECT)
        .getSelectTag()
        .withData("question-id", String.valueOf(questionDefinition.getId()));
  }

  private DivTag createValueField(
      QuestionDefinition questionDefinition,
      int groupId,
      Optional<Scalar> maybeScalar,
      Optional<PredicateValue> maybePredicateValue) {
    DivTag valueField = div().withClasses(COLUMN_WIDTH);

    if (questionDefinition.getQuestionType().isMultiOptionType()) {
      // If it's a multi-option question, we need to provide a discrete list of possible values to
      // choose from instead of a freeform text field. Not only is it a better UX, but we store the
      // ID of the options rather than the display strings since the option display strings are
      // localized.
      ImmutableList<QuestionOption> options =
          ((MultiOptionQuestionDefinition) questionDefinition).getOptions();

      ImmutableSet<String> currentlyCheckedValues =
          maybePredicateValue
              .map(PredicateValue::value)
              .map(
                  value ->
                      Splitter.on(", ")
                          .splitToStream(value.substring(1, value.length() - 1))
                          .map(item -> item.replaceAll("\"", ""))
                          .collect(ImmutableSet.toImmutableSet()))
              .orElse(ImmutableSet.of());

      for (QuestionOption option : options) {
        boolean isChecked = currentlyCheckedValues.contains(String.valueOf(option.id()));

        LabelTag optionCheckbox =
            FieldWithLabel.checkbox()
                .setFieldName(
                    String.format(
                        "group-%d-question-%d-predicateValues[]",
                        groupId, questionDefinition.getId()))
                .setValue(String.valueOf(option.id()))
                .setLabelText(option.optionText().getDefault())
                .setChecked(isChecked)
                .getCheckboxTag();
        valueField.with(optionCheckbox);
      }

      return valueField.withData("question-id", String.valueOf(questionDefinition.getId()));
    } else {
      return valueField
          .withData("question-id", String.valueOf(questionDefinition.getId()))
          .with(
              FieldWithLabel.input()
                  .setFieldName(
                      String.format(
                          "group-%d-question-%d-predicateValue",
                          groupId, questionDefinition.getId()))
                  .setValue(
                      maybePredicateValue.map(
                          predicateValue ->
                              formatPredicateValue(maybeScalar.get(), predicateValue)))
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

  private static String formatPredicateValue(Scalar scalar, PredicateValue predicateValue) {
    switch (scalar.toScalarType()) {
      case CURRENCY_CENTS:
        {
          long storedCents = Long.parseLong(predicateValue.value());
          long dollars = storedCents / 100;
          long cents = storedCents % 100;
          return String.format("%d.%02d", dollars, cents);
        }

      case DATE:
        {
          return Instant.ofEpochMilli(Long.parseLong(predicateValue.value()))
              .atZone(ZoneId.systemDefault())
              .toLocalDate()
              .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

      case LIST_OF_STRINGS:
      case LONG:
      case STRING:
        {
          return predicateValue.value();
        }

      default:
        {
          throw new RuntimeException(
              String.format("Unknown scalar type: %s", scalar.toScalarType()));
        }
    }
  }
}
