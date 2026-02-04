package views.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static views.ViewUtils.ProgramDisplayType.DRAFT;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.DivTag;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
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
import services.program.predicate.PredicateUseCase;
import services.program.predicate.PredicateValue;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import support.CfTestHelpers;
import support.TestQuestionBank;
import views.ViewUtils.ProgramDisplayType;

@RunWith(JUnitParamsRunner.class)
public class ProgramBaseViewTest {

  private static final long PROGRAM_ID = 1;
  private static final long BLOCK_ID = 2;
  private static final String BLOCK_NAME = "Block_name";
  private TestQuestionBank testQuestionBank = new TestQuestionBank(/* canSave= */ false);
  private SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
  private ImmutableList<QuestionDefinition> questionDefinitions =
      ImmutableList.of(
          testQuestionBank.dateApplicantBirthdate().getQuestionDefinition(),
          testQuestionBank.emailApplicantEmail().getQuestionDefinition());

  @Test
  @Parameters({"true", "false"})
  public void renderExistingPredicate_singleQuestion(boolean expandedFormLogicEnabled) {
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
            .renderExistingPredicate(
                PROGRAM_ID,
                BLOCK_ID,
                BLOCK_NAME,
                predicateDefinition,
                questionDefinitions,
                PredicateUseCase.VISIBILITY,
                /* includeEditFooter= */ false,
                /* expanded= */ false,
                expandedFormLogicEnabled);

    assertThat(result.render())
        .contains(
            """
            Block_name is <strong>hidden</strong> if <strong>&quot;applicant birth \
            date&quot;</strong> date is equal to <strong>2023-01-01</strong>""");
  }

  @Test
  @Parameters({"true", "false"})
  public void renderExistingPredicate_includeEditFooter(boolean expandedFormLogicEnabled) {
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
            .renderExistingPredicate(
                PROGRAM_ID,
                BLOCK_ID,
                BLOCK_NAME,
                predicateDefinition,
                questionDefinitions,
                PredicateUseCase.VISIBILITY,
                /* includeEditFooter= */ true,
                /* expanded= */ false,
                expandedFormLogicEnabled);

    assertThat(result.render()).contains("Edit visibility conditions");
  }

  @Test
  @Parameters({"true", "false"})
  public void renderExistingPredicate_orOfSingleLayerAnds_withSingleAnd(
      boolean expandedFormLogicEnabled) {
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
            .renderExistingPredicate(
                PROGRAM_ID,
                BLOCK_ID,
                BLOCK_NAME,
                predicateDefinition,
                questionDefinitions,
                PredicateUseCase.VISIBILITY,
                /* includeEditFooter= */ false,
                /* expanded= */ false,
                expandedFormLogicEnabled);

    assertThat(result.render())
        .contains(
            """
            Block_name is <strong>hidden</strong> if <strong>&quot;applicant \
            birth date&quot;</strong> date is equal to <strong>2023-01-01</strong> \
            AND <strong>&quot;applicant email address&quot;</strong> email is \
            equal to <strong>&quot;test@example.com&quot;</strong>""");
  }

  @Test
  @Parameters({"true", "false"})
  public void renderExistingPredicate_orOfSingleLayerAnds_withMultipleAnd(
      boolean expandedFormLogicEnabled) {
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
            .renderExistingPredicate(
                PROGRAM_ID,
                BLOCK_ID,
                BLOCK_NAME,
                predicateDefinition,
                questionDefinitions,
                PredicateUseCase.VISIBILITY,
                /* includeEditFooter= */ false,
                /* expanded= */ false,
                expandedFormLogicEnabled);

    if (expandedFormLogicEnabled) {
      assertThat(result.render())
          .contains(
              """
              Block_name is <strong>hidden</strong> if <strong>any</strong> conditions are \
              true:</p><ol class="list-decimal ml-4 pt-4"><li><strong>&quot;applicant birth \
              date&quot;</strong> date is equal to <strong>2023-01-01</strong> AND \
              <strong>&quot;applicant email address&quot;</strong> email is equal to \
              <strong>&quot;test@example.com&quot;</strong></li><li><strong>&quot;applicant birth \
              date&quot;</strong> date is equal to <strong>2023-03-03</strong> AND \
              <strong>&quot;applicant email address&quot;</strong> email is equal to \
              <strong>&quot;other@example.com&quot;</strong></li>""");
    } else {
      assertThat(result.render())
          .contains(
              """
              Block_name is <strong>hidden</strong> if <strong>any</strong> of the following is \
              true:</p><ol class="list-decimal ml-4 pt-4"><li><strong>&quot;applicant birth \
              date&quot;</strong> date is equal to <strong>2023-01-01</strong> AND \
              <strong>&quot;applicant email address&quot;</strong> email is equal to \
              <strong>&quot;test@example.com&quot;</strong></li><li><strong>&quot;applicant birth \
              date&quot;</strong> date is equal to <strong>2023-03-03</strong> AND \
              <strong>&quot;applicant email address&quot;</strong> email is equal to \
              <strong>&quot;other@example.com&quot;</strong></li>""");
    }
  }

  private static final class ProgramBlockBaseViewTestChild extends ProgramBaseView {

    ProgramBlockBaseViewTestChild(SettingsManifest settingsManifest) {
      super(settingsManifest);
    }

    @Override
    protected ProgramDisplayType getProgramDisplayStatus() {
      return DRAFT;
    }
  }
}
