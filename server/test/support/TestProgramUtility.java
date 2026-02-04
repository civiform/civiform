package support;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Random;
import models.ApplicationStep;
import models.DisplayMode;
import repository.VersionRepository;
import services.applicant.question.Scalar;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.QuestionService;
import services.question.types.NameQuestionDefinition;

/**
 * Utility class for creating program definitions in tests.
 *
 * <p>This class provides helper methods to construct ProgramDefinition objects with specific
 * configurations for testing purposes, such as programs with non-gating eligibility rules.
 */
public class TestProgramUtility {
  /**
   * Creates a new program definition with a non-gating eligibility rule.
   *
   * <p>The program will have a basic setup including an ID, admin name, description, external link,
   * display mode, and program type. It will include a single application step and a single required
   * question. The eligibility definition is set to be non-gating, meaning that even if an applicant
   * does not meet the eligibility criteria, they can still proceed with the application. The
   * eligibility is based on a simple predicate: the first name of the provided question must be
   * "eligible name".
   *
   * @param questionService The {@link QuestionService} instance to use.
   * @param versionRepository The {@link VersionRepository} instance to use for publishing the new
   *     version.
   * @param question The {@link NameQuestionDefinition} to be used as a required question and for
   *     the eligibility predicate.
   * @return A {@link ProgramDefinition} configured with a non-gating eligibility rule.
   */
  public static ProgramDefinition createProgramWithNongatingEligibility(
      QuestionService questionService,
      VersionRepository versionRepository,
      NameQuestionDefinition question) {
    EligibilityDefinition eligibilityDef =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            question.getId(),
                            Scalar.FIRST_NAME,
                            Operator.EQUAL_TO,
                            PredicateValue.of("eligible name"))),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram(
                ProgramDefinition.builder()
                    .setId(new Random().nextLong())
                    .setAdminName("name")
                    .setAdminDescription("desc")
                    .setExternalLink("https://usa.gov")
                    .setDisplayMode(DisplayMode.PUBLIC)
                    .setProgramType(ProgramType.DEFAULT)
                    .setEligibilityIsGating(false)
                    .setLoginOnly(false)
                    .setAcls(new ProgramAcls())
                    .setCategories(ImmutableList.of())
                    .setApplicationSteps(
                        ImmutableList.of(new ApplicationStep("title", "description")))
                    .setBridgeDefinitions(ImmutableMap.of())
                    .build())
            .withBlock()
            .withRequiredQuestionDefinitions(ImmutableList.of(question))
            .withEligibilityDefinition(eligibilityDef)
            .buildDefinition();
    versionRepository.publishNewSynchronizedVersion();
    return programDefinition;
  }
}
