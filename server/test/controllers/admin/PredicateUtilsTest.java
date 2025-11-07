package controllers.admin;

import static j2html.TagCreator.join;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import j2html.tags.UnescapedText;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.applicant.question.Scalar;
import services.program.predicate.AndNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.OrNode;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;

@RunWith(JUnitParamsRunner.class)
public class PredicateUtilsTest {
  @Test
  @Parameters({"true", "false"})
  public void getReadablePredicateDescription_singleQuestion_headingOnly(
      boolean expandedFormLogicEnabled) {
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1000,
                    Scalar.NUMBER,
                    Operator.GREATER_THAN_OR_EQUAL_TO,
                    PredicateValue.of(1000L))),
            PredicateAction.SHOW_BLOCK);

    ReadablePredicate readablePredicate =
        PredicateUtils.getReadablePredicateDescription(
            "My Test Block", predicate, ImmutableList.of(), expandedFormLogicEnabled);

    assertThat(readablePredicate.heading())
        .isEqualTo("My Test Block is shown if number is greater than or equal to 1000");
    assertThat(readablePredicate.formattedHtmlHeading().toString())
        .isEqualTo(
            """
            My Test Block is <strong>shown</strong> if number is greater than or equal to \
            <strong>1000</strong>""");
    assertThat(readablePredicate.conditionList()).isEmpty();
    assertThat(readablePredicate.formattedHtmlConditionList()).isEmpty();
  }

  @Test
  @Parameters({"true", "false"})
  public void getReadablePredicateDescription_singleAnd_headingOnly(
      boolean expandedFormLogicEnabled) {
    ImmutableList<PredicateExpressionNode> andStatements =
        ImmutableList.of(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1000,
                    Scalar.CITY,
                    Operator.EQUAL_TO,
                    PredicateValue.of("Phoenix"))),
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1001,
                    Scalar.NUMBER,
                    Operator.LESS_THAN,
                    PredicateValue.of(4))),
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1002,
                    Scalar.TEXT,
                    Operator.NOT_EQUAL_TO,
                    PredicateValue.of("hello"))));
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                OrNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(AndNode.create(andStatements))))),
            PredicateAction.HIDE_BLOCK);

    ReadablePredicate readablePredicate =
        PredicateUtils.getReadablePredicateDescription(
            "My Test Block", predicate, ImmutableList.of(), expandedFormLogicEnabled);

    assertThat(readablePredicate.heading())
        .isEqualTo(
            """
            My Test Block is hidden if city is equal to "Phoenix" AND number is less than 4 AND \
            text is not equal to "hello\"""");
    assertThat(readablePredicate.formattedHtmlHeading().toString())
        .isEqualTo(
            """
            My Test Block is <strong>hidden</strong> if city is equal to \
            <strong>&quot;Phoenix&quot;</strong> AND number is less than <strong>4</strong> \
            AND text is not equal to <strong>&quot;hello&quot;</strong>""");
    assertThat(readablePredicate.conditionList()).isEmpty();
    assertThat(readablePredicate.formattedHtmlConditionList()).isEmpty();
  }

  @Test
  @Parameters({"true", "false"})
  public void getReadablePredicateDescription_multipleAnds_headingAndConditionList(
      boolean expandedFormLogicEnabled) {
    // number == 4 && text == "four"
    ImmutableList<PredicateExpressionNode> andStatements1 =
        ImmutableList.of(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1001,
                    Scalar.NUMBER,
                    Operator.EQUAL_TO,
                    PredicateValue.of(4))),
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1002,
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("four"))));
    // number == 5 && text == "five"
    ImmutableList<PredicateExpressionNode> andStatements2 =
        ImmutableList.of(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1001,
                    Scalar.NUMBER,
                    Operator.EQUAL_TO,
                    PredicateValue.of(5))),
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1002,
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("five"))));
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                OrNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(AndNode.create(andStatements1)),
                        PredicateExpressionNode.create(AndNode.create(andStatements2))))),
            PredicateAction.ELIGIBLE_BLOCK);

    ReadablePredicate readablePredicate =
        PredicateUtils.getReadablePredicateDescription(
            "My Test Block", predicate, ImmutableList.of(), expandedFormLogicEnabled);

    if (expandedFormLogicEnabled) {
      assertThat(readablePredicate.heading())
          .isEqualTo("Applicant is eligible if any conditions are true:");
      assertThat(readablePredicate.formattedHtmlHeading().toString())
          .isEqualTo(
              """
Applicant is <strong>eligible</strong> if <strong>any</strong> conditions are true:""");
    } else {
      assertThat(readablePredicate.heading())
          .isEqualTo("Applicant is eligible if any of the following is true:");
      assertThat(readablePredicate.formattedHtmlHeading().toString())
          .isEqualTo(
              """
              Applicant is <strong>eligible</strong> if <strong>any</strong> of the \
              following is true:""");
    }
    assertThat(readablePredicate.conditionList()).isPresent();
    assertThat(readablePredicate.conditionList().get().size()).isEqualTo(2);
    assertThat(readablePredicate.conditionList().get().get(0))
        .isEqualTo("number is equal to 4 AND text is equal to \"four\"");
    assertThat(readablePredicate.conditionList().get().get(1))
        .isEqualTo("number is equal to 5 AND text is equal to \"five\"");
    assertThat(readablePredicate.formattedHtmlConditionList()).isPresent();
    assertThat(readablePredicate.formattedHtmlConditionList().get().size()).isEqualTo(2);
    assertThat(readablePredicate.formattedHtmlConditionList().get().get(0).toString())
        .isEqualTo(
            """
            number is equal to <strong>4</strong> AND text is equal to \
            <strong>&quot;four&quot;</strong>""");
    assertThat(readablePredicate.formattedHtmlConditionList().get().get(1).toString())
        .isEqualTo(
            """
            number is equal to <strong>5</strong> AND text is equal to \
            <strong>&quot;five&quot;</strong>""");
  }

  @Test
  public void joinUnescapedText_emptyList() {
    UnescapedText result = PredicateUtils.joinUnescapedText(ImmutableList.of(), "AND");
    assertThat(result.toString()).isEqualTo("");
  }

  @Test
  public void joinUnescapedText_singleElement() {
    UnescapedText result =
        PredicateUtils.joinUnescapedText(ImmutableList.of(join("single component")), "AND");
    assertThat(result.toString()).isEqualTo("single component");
  }

  @Test
  public void joinUnescapedText_multipleElements() {
    UnescapedText result =
        PredicateUtils.joinUnescapedText(ImmutableList.of(join("first"), join("second")), "AND");
    assertThat(result.toString()).isEqualTo("first AND second");
  }
}
