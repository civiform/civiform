package models;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import repository.ProgramRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramType;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public class ProgramModelTest extends ResetPostgres {

  private ProgramRepository repo;
  private Long uniqueProgramId;

  @Before
  public void setupProgramRepository() {
    repo = instanceOf(ProgramRepository.class);
    uniqueProgramId = new Random().nextLong();
  }

  @Test
  public void canSaveProgram() throws UnsupportedQuestionTypeException {
    QuestionDefinition questionDefinition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.TEXT)
            .setId(123L)
            .setName("question")
            .setDescription("applicant's name")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is your name?"))
            .build();
    long programDefinitionId = uniqueProgramId;
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("First Block")
            .setDescription("basic info")
            .setLocalizedName(LocalizedStrings.withDefaultValue("First Block"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("basic info"))
            .setProgramQuestionDefinitions(
                ImmutableList.of(
                    ProgramQuestionDefinition.create(
                        questionDefinition, Optional.of(programDefinitionId))))
            .build();
    HashSet<Long> tiOrgList = new HashSet<>();
    tiOrgList.add(1L);
    tiOrgList.add(3L);
    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(programDefinitionId)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "ProgramTest"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "desc"))
            .setLocalizedShortDescription(LocalizedStrings.of(Locale.US, "Short description"))
            .setLocalizedConfirmationMessage(
                LocalizedStrings.of(Locale.US, "custom confirmation message"))
            .setBlockDefinitions(ImmutableList.of(blockDefinition))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setNotificationPreferences(
                ImmutableList.of(ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS))
            .setProgramType(ProgramType.COMMON_INTAKE_FORM)
            .setEligibilityIsGating(false)
            .setAcls(new ProgramAcls(tiOrgList))
            .setLocalizedSummaryImageDescription(
                Optional.of(LocalizedStrings.of(Locale.US, "custom summary image description")))
            .setSummaryImageFileKey(Optional.of("program-card-images/program-1/testFile.png"))
            .setCategories(ImmutableList.of())
            .build();
    ProgramModel program = new ProgramModel(definition);

    program.save();

    ProgramModel found = repo.lookupProgram(program.id).toCompletableFuture().join().get();

    assertThat(found.getProgramDefinition().adminName()).isEqualTo("Admin name");
    assertThat(found.getProgramDefinition().localizedName())
        .isEqualTo(LocalizedStrings.of(Locale.US, "ProgramTest"));
    assertThat(found.getProgramDefinition().localizedShortDescription())
        .isEqualTo(LocalizedStrings.of(Locale.US, "Short description"));
    assertThat(found.getProgramDefinition().notificationPreferences())
        .containsExactlyInAnyOrder(
            ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS);
    assertThat(found.getProgramDefinition().localizedConfirmationMessage())
        .isEqualTo(LocalizedStrings.of(Locale.US, "custom confirmation message"));
    assertThat(found.getProgramDefinition().localizedSummaryImageDescription().get())
        .isEqualTo(LocalizedStrings.of(Locale.US, "custom summary image description"));
    assertThat(found.getProgramDefinition().summaryImageFileKey().get())
        .isEqualTo("program-card-images/program-1/testFile.png");
    assertThat(found.getProgramDefinition().blockDefinitions().get(0).name())
        .isEqualTo("First Block");
    assertThat(found.getProgramDefinition().programType())
        .isEqualTo(ProgramType.COMMON_INTAKE_FORM);
    assertThat(found.getProgramDefinition().eligibilityIsGating()).isEqualTo(false);
    assertThat(found.getProgramDefinition().acls().getTiProgramViewAcls()).contains(1L);
    assertThat(found.getProgramDefinition().acls().getTiProgramViewAcls()).contains(3L);
    assertThat(found.getCategories()).isInstanceOf(ImmutableList.class);

    assertThat(
            found
                .getProgramDefinition()
                .blockDefinitions()
                .get(0)
                .programQuestionDefinitions()
                .get(0)
                .id())
        .isEqualTo(questionDefinition.getId());
  }

  @Test
  public void correctlySerializesDifferentQuestionTypes() throws UnsupportedQuestionTypeException {
    AddressQuestionDefinition addressQuestionDefinition =
        (AddressQuestionDefinition)
            new QuestionDefinitionBuilder()
                .setQuestionType(QuestionType.ADDRESS)
                .setId(456L)
                .setName("address question")
                .setDescription("applicant's address")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your address?"))
                .build();
    NameQuestionDefinition nameQuestionDefinition =
        (NameQuestionDefinition)
            new QuestionDefinitionBuilder()
                .setQuestionType(QuestionType.NAME)
                .setId(789L)
                .setName("name question")
                .setDescription("applicant's name")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your name?"))
                .build();

    long programDefinitionId = uniqueProgramId;
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("First Block")
            .setDescription("basic info")
            .setLocalizedName(LocalizedStrings.withDefaultValue("First Block"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("basic info"))
            .setProgramQuestionDefinitions(
                ImmutableList.of(
                    ProgramQuestionDefinition.create(
                        addressQuestionDefinition, Optional.of(programDefinitionId)),
                    ProgramQuestionDefinition.create(
                        nameQuestionDefinition, Optional.of(programDefinitionId))))
            .build();

    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(programDefinitionId)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "ProgramTest"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "desc"))
            .setLocalizedShortDescription(LocalizedStrings.of(Locale.US, "short desc"))
            .setBlockDefinitions(ImmutableList.of(blockDefinition))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(false)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .build();
    ProgramModel program = new ProgramModel(definition);
    program.save();

    ProgramModel found = repo.lookupProgram(program.id).toCompletableFuture().join().get();

    ProgramQuestionDefinition addressQuestion =
        found.getProgramDefinition().blockDefinitions().get(0).programQuestionDefinitions().get(0);
    assertThat(addressQuestion.id()).isEqualTo(addressQuestionDefinition.getId());
    ProgramQuestionDefinition nameQuestion =
        found.getProgramDefinition().blockDefinitions().get(0).programQuestionDefinitions().get(1);
    assertThat(nameQuestion.id()).isEqualTo(nameQuestionDefinition.getId());
  }

  @Test
  public void blockPredicates_serializedCorrectly() throws Exception {
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    1L, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of(""))),
            PredicateAction.HIDE_BLOCK);

    EligibilityDefinition eligibilityDefinition =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            1L, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of(""))),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();

    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("Test predicates")
            .setDescription("set hide and deprecated optional")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Test predicates"))
            .setLocalizedDescription(
                LocalizedStrings.withDefaultValue("set hide and deprecated optional"))
            .setProgramQuestionDefinitions(ImmutableList.of())
            .setVisibilityPredicate(predicate)
            .setEligibilityDefinition(eligibilityDefinition)
            .build();

    ProgramDefinition definition =
        ProgramDefinition.builder()
            .setId(uniqueProgramId)
            .setAdminName("Admin name")
            .setAdminDescription("Admin description")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "ProgramTest"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "desc"))
            .setLocalizedShortDescription(LocalizedStrings.of(Locale.US, "short desc"))
            .setBlockDefinitions(ImmutableList.of(blockDefinition))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(false)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .build();
    ProgramModel program = new ProgramModel(definition);
    program.save();

    ProgramModel found = repo.lookupProgram(program.id).toCompletableFuture().join().get();

    assertThat(found.getProgramDefinition().blockDefinitions()).hasSize(1);
    BlockDefinition block = found.getProgramDefinition().getBlockDefinition(1L);
    assertThat(block.visibilityPredicate()).hasValue(predicate);
    assertThat(block.eligibilityDefinition()).hasValue(eligibilityDefinition);
    assertThat(block.eligibilityDefinition().get().predicate().predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(block.optionalPredicate()).isEmpty();
  }

  @Test
  public void unorderedBlockDefinitions_getOrderedBlockDefinitionsOnSave() {
    long programDefinitionId = uniqueProgramId;
    ImmutableList<BlockDefinition> unorderedBlocks =
        ImmutableList.<BlockDefinition>builder()
            .add(
                BlockDefinition.builder()
                    .setId(1L)
                    .setName("enumerator")
                    .setDescription("description")
                    .setLocalizedName(LocalizedStrings.withDefaultValue("enumerator"))
                    .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank
                                .enumeratorApplicantHouseholdMembers()
                                .getQuestionDefinition(),
                            Optional.of(programDefinitionId)))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(2L)
                    .setName("top level")
                    .setDescription("description")
                    .setLocalizedName(LocalizedStrings.withDefaultValue("top level"))
                    .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank.emailApplicantEmail().getQuestionDefinition(),
                            Optional.of(programDefinitionId)))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(3L)
                    .setName("nested enumerator")
                    .setDescription("description")
                    .setLocalizedName(LocalizedStrings.withDefaultValue("nested enumerator"))
                    .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
                    .setEnumeratorId(Optional.of(1L))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank
                                .enumeratorNestedApplicantHouseholdMemberJobs()
                                .getQuestionDefinition(),
                            Optional.of(programDefinitionId)))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(4L)
                    .setName("repeated")
                    .setDescription("description")
                    .setLocalizedName(LocalizedStrings.withDefaultValue("repeated"))
                    .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
                    .setEnumeratorId(Optional.of(1L))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank
                                .nameRepeatedApplicantHouseholdMemberName()
                                .getQuestionDefinition(),
                            Optional.of(programDefinitionId)))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(5L)
                    .setName("nested repeated")
                    .setDescription("description")
                    .setLocalizedName(LocalizedStrings.withDefaultValue("nested repeated"))
                    .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
                    .setEnumeratorId(Optional.of(3L))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank
                                .numberNestedRepeatedApplicantHouseholdMemberDaysWorked()
                                .getQuestionDefinition(),
                            Optional.of(programDefinitionId)))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(6L)
                    .setName("top level 2")
                    .setDescription("description")
                    .setLocalizedName(LocalizedStrings.withDefaultValue("top level 2"))
                    .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank.nameApplicantName().getQuestionDefinition(),
                            Optional.of(programDefinitionId)))
                    .build())
            .build();

    ProgramDefinition programDefinition =
        ProgramDefinition.builder()
            .setId(programDefinitionId)
            .setAdminName("test program")
            .setAdminDescription("test description")
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setLocalizedName(LocalizedStrings.withDefaultValue("test name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("test description"))
            .setLocalizedShortDescription(LocalizedStrings.of(Locale.US, "short desc"))
            .setBlockDefinitions(unorderedBlocks)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(false)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .build();

    assertThat(programDefinition.hasOrderedBlockDefinitions()).isFalse();

    ProgramModel program = programDefinition.toProgram();
    program.save();

    assertThat(program.getProgramDefinition().hasOrderedBlockDefinitions()).isTrue();
  }
}
