package views.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static views.ViewUtils.ProgramDisplayType.DRAFT;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.DivTag;
import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import services.applicant.question.Scalar;
import services.program.predicate.AndNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.OrNode;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import support.CfTestHelpers;
import support.TestQuestionBank;
import views.ViewUtils.ProgramDisplayType;

@RunWith(JUnitParamsRunner.class)
public class ProgramBaseViewTest {

  private static final String BLOCK_NAME = "Block_name";
  private TestQuestionBank testQuestionBank = new TestQuestionBank(/* canSave= */ false);
  private SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
  private ImmutableList<QuestionDefinition> questionDefinitions =
      ImmutableList.of(
          testQuestionBank.dateApplicantBirthdate().getQuestionDefinition(),
          testQuestionBank.emailApplicantEmail().getQuestionDefinition());

  @Test
  public void renderExistingPredicate_singleQuestion() {
    var predicateDefinition =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                    .setScalar(Scalar.DATE)
                    .setOperator(Operator.EQUAL_TO)
                    .setComparedValue(CfTestHelpers.stringToPredicateDate("2023-01-01"))
                    .build()),
            PredicateAction.HIDE_BLOCK);

    DivTag result =
        new ProgramBlockBaseViewTestChild(mockSettingsManifest)
            .renderExistingPredicate(BLOCK_NAME, predicateDefinition, questionDefinitions);

    assertThat(result.render())
        .contains(
            "Block_name is hidden if &quot;applicant birth date&quot; date is equal to"
                + " 2023-01-01");
  }

  @Test
  public void renderExistingPredicate_orOfSingleLayerAnds_withSingleAnd() {
    var predicateDefinition =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                OrNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(
                            AndNode.create(
                                ImmutableList.of(
                                    PredicateExpressionNode.create(
                                        LeafOperationExpressionNode.builder()
                                            .setQuestionId(
                                                testQuestionBank.dateApplicantBirthdate().id)
                                            .setScalar(Scalar.DATE)
                                            .setOperator(Operator.EQUAL_TO)
                                            .setComparedValue(
                                                CfTestHelpers.stringToPredicateDate("2023-01-01"))
                                            .build()),
                                    PredicateExpressionNode.create(
                                        LeafOperationExpressionNode.builder()
                                            .setQuestionId(
                                                testQuestionBank.emailApplicantEmail().id)
                                            .setScalar(Scalar.EMAIL)
                                            .setOperator(Operator.EQUAL_TO)
                                            .setComparedValue(PredicateValue.of("test@example.com"))
                                            .build()))))))),
            PredicateAction.HIDE_BLOCK);

    DivTag result =
        new ProgramBlockBaseViewTestChild(mockSettingsManifest)
            .renderExistingPredicate(BLOCK_NAME, predicateDefinition, questionDefinitions);

    assertThat(result.render())
        .contains(
            "Block_name is hidden if &quot;applicant birth date&quot; date is equal to 2023-01-01"
                + " and &quot;applicant email address&quot; email is equal to"
                + " &quot;test@example.com&quot;");
  }

  @Test
  public void renderExistingPredicate_orOfSingleLayerAnds_withMultipleAnd() {
    var predicateDefinition =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                OrNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(
                            AndNode.create(
                                ImmutableList.of(
                                    PredicateExpressionNode.create(
                                        LeafOperationExpressionNode.builder()
                                            .setQuestionId(
                                                testQuestionBank.dateApplicantBirthdate().id)
                                            .setScalar(Scalar.DATE)
                                            .setOperator(Operator.EQUAL_TO)
                                            .setComparedValue(
                                                CfTestHelpers.stringToPredicateDate("2023-01-01"))
                                            .build()),
                                    PredicateExpressionNode.create(
                                        LeafOperationExpressionNode.builder()
                                            .setQuestionId(
                                                testQuestionBank.emailApplicantEmail().id)
                                            .setScalar(Scalar.EMAIL)
                                            .setOperator(Operator.EQUAL_TO)
                                            .setComparedValue(PredicateValue.of("test@example.com"))
                                            .build())))),
                        PredicateExpressionNode.create(
                            AndNode.create(
                                ImmutableList.of(
                                    PredicateExpressionNode.create(
                                        LeafOperationExpressionNode.builder()
                                            .setQuestionId(
                                                testQuestionBank.dateApplicantBirthdate().id)
                                            .setScalar(Scalar.DATE)
                                            .setOperator(Operator.EQUAL_TO)
                                            .setComparedValue(
                                                CfTestHelpers.stringToPredicateDate("2023-03-03"))
                                            .build()),
                                    PredicateExpressionNode.create(
                                        LeafOperationExpressionNode.builder()
                                            .setQuestionId(
                                                testQuestionBank.emailApplicantEmail().id)
                                            .setScalar(Scalar.EMAIL)
                                            .setOperator(Operator.EQUAL_TO)
                                            .setComparedValue(
                                                PredicateValue.of("other@example.com"))
                                            .build()))))))),
            PredicateAction.HIDE_BLOCK);

    DivTag result =
        new ProgramBlockBaseViewTestChild(mockSettingsManifest)
            .renderExistingPredicate(BLOCK_NAME, predicateDefinition, questionDefinitions);

    assertThat(result.render())
        .contains(
            "Block_name is hidden if any of:<ul class=\"list-disc ml-4 mb-4\"><li>&quot;applicant"
                + " birth date&quot; date is equal to 2023-01-01 and &quot;applicant email"
                + " address&quot; email is equal to"
                + " &quot;test@example.com&quot;</li><li>&quot;applicant birth date&quot; date is"
                + " equal to 2023-03-03 and &quot;applicant email address&quot; email is equal to"
                + " &quot;other@example.com&quot;</li></ul>");
  }

  private static final class ProgramBlockBaseViewTestChild extends ProgramBaseView {

    public ProgramBlockBaseViewTestChild(SettingsManifest settingsManifest) {
      super(settingsManifest);
    }

    @Override
    protected ProgramDisplayType getProgramDisplayStatus() {
      return DRAFT;
    }
  }
}
