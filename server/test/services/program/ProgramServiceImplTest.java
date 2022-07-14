package services.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import forms.BlockForm;
import io.ebean.DB;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import models.Account;
import models.DisplayMode;
import models.Program;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.applicant.question.Scalar;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.TextQuestionDefinition;
import support.ProgramBuilder;

public class ProgramServiceImplTest extends ResetPostgres {

  private QuestionDefinition addressQuestion;
  private QuestionDefinition colorQuestion;
  private QuestionDefinition nameQuestion;
  private ProgramServiceImpl ps;

  @Before
  public void setProgramServiceImpl() {
    ps = instanceOf(ProgramServiceImpl.class);
  }

  @Before
  public void setUp() {
    addressQuestion = testQuestionBank.applicantAddress().getQuestionDefinition();
    colorQuestion = testQuestionBank.applicantFavoriteColor().getQuestionDefinition();
    nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
  }

  @Test
  public void syncQuestions_constructsAllQuestionDefinitions() {
    QuestionDefinition questionOne = nameQuestion;
    QuestionDefinition questionTwo = addressQuestion;
    QuestionDefinition questionThree = colorQuestion;

    ProgramBuilder.newDraftProgram("program1")
        .withBlock()
        .withRequiredQuestionDefinition(questionOne)
        .withRequiredQuestionDefinition(questionTwo)
        .withBlock()
        .withRequiredQuestionDefinition(questionThree)
        .buildDefinition();
    ProgramBuilder.newDraftProgram("program2")
        .withBlock()
        .withRequiredQuestionDefinition(questionTwo)
        .withBlock()
        .withRequiredQuestionDefinition(questionOne)
        .buildDefinition();

    ImmutableList<ProgramDefinition> programDefinitions =
        ps.getActiveAndDraftPrograms().getDraftPrograms();

    QuestionDefinition found = programDefinitions.get(0).getQuestionDefinition(0, 0);
    assertThat(found).isInstanceOf(NameQuestionDefinition.class);
    found = programDefinitions.get(0).getQuestionDefinition(0, 1);
    assertThat(found).isInstanceOf(AddressQuestionDefinition.class);
    found = programDefinitions.get(0).getQuestionDefinition(1, 0);
    assertThat(found).isInstanceOf(TextQuestionDefinition.class);
    found = programDefinitions.get(1).getQuestionDefinition(0, 0);
    assertThat(found).isInstanceOf(AddressQuestionDefinition.class);
    found = programDefinitions.get(1).getQuestionDefinition(1, 0);
    assertThat(found).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void createProgram_setsId() {
    assertThat(ps.getActiveAndDraftPrograms().getActiveSize()).isEqualTo(0);
    assertThat(ps.getActiveAndDraftPrograms().getDraftSize()).isEqualTo(0);

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            "ProgramService",
            "description",
            "name",
            "description",
            "",
            DisplayMode.PUBLIC.getValue());

    assertThat(result.hasResult()).isTrue();
    assertThat(result.getResult().id()).isNotNull();
  }

  @Test
  public void createProgram_hasEmptyBlock() {
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            "ProgramService",
            "description",
            "name",
            "description",
            "",
            DisplayMode.PUBLIC.getValue());

    assertThat(result.hasResult()).isTrue();
    assertThat(result.getResult().blockDefinitions()).hasSize(1);
    assertThat(result.getResult().getBlockDefinitionByIndex(0).get().id()).isEqualTo(1L);
    assertThat(result.getResult().getBlockDefinitionByIndex(0).get().name()).isEqualTo("Screen 1");
  }

  @Test
  public void createProgram_returnsErrors() {
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition("", "", "", "", "", DisplayMode.PUBLIC.getValue());

    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsExactly(
            CiviFormError.of("program admin name cannot be blank"),
            CiviFormError.of("program admin description cannot be blank"),
            CiviFormError.of("program display name cannot be blank"),
            CiviFormError.of("program display description cannot be blank"));
  }

  @Test
  public void createProgramWithoutDisplayMode_returnsError() {
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition("ProgramService", "description", "name", "description", "", "");

    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsExactly(CiviFormError.of("program display mode cannot be blank"));
  }

  @Test
  public void createProgram_protectsAgainstProgramNameCollisions() {
    ps.createProgramDefinition(
        "name",
        "description",
        "display name",
        "display description",
        "",
        DisplayMode.PUBLIC.getValue());

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            "name",
            "description",
            "display name",
            "display description",
            "",
            DisplayMode.PUBLIC.getValue());

    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsExactly(CiviFormError.of("a program named name already exists"));
  }

  @Test
  public void createProgram_protectsAgainstProgramSlugCollisions() {
    // Two programs with names that are different but slugify to same value.
    ps.createProgramDefinition(
        "name one",
        "description",
        "display name",
        "display description",
        "",
        DisplayMode.PUBLIC.getValue());

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            // Program name here is missing the extra space
            // so that the names are different but the resulting
            // slug is the same.
            "name  one",
            "description",
            "display name",
            "display description",
            "",
            DisplayMode.PUBLIC.getValue());

    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsExactly(CiviFormError.of("a program named name  one already exists"));
  }

  @Test
  public void updateProgram_withNoProgram_throwsProgramNotFoundException() {
    assertThatThrownBy(
            () ->
                ps.updateProgramDefinition(
                    1L,
                    Locale.US,
                    "new description",
                    "name",
                    "description",
                    "",
                    DisplayMode.PUBLIC.getValue()))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessage("Program not found for ID: 1");
  }

  @Test
  public void updateProgram_updatesProgram() throws Exception {
    ProgramDefinition originalProgram =
        ProgramBuilder.newDraftProgram("original", "original description").buildDefinition();
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateProgramDefinition(
            originalProgram.id(),
            Locale.US,
            "new description",
            "name",
            "description",
            "",
            DisplayMode.PUBLIC.getValue());

    assertThat(result.hasResult()).isTrue();
    ProgramDefinition updatedProgram = result.getResult();

    ProgramDefinition found = ps.getProgramDefinition(updatedProgram.id());

    assertThat(ps.getActiveAndDraftPrograms().getDraftSize()).isEqualTo(1);
    assertThat(found.adminName()).isEqualTo(updatedProgram.adminName());
    assertThat(found.lastModifiedTime().isPresent()).isTrue();
    assertThat(originalProgram.lastModifiedTime().isPresent()).isTrue();
    assertThat(found.lastModifiedTime().get().isAfter(originalProgram.lastModifiedTime().get()))
        .isTrue();
  }

  @Test
  public void updateProgram_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();
    ps.addQuestionsToBlock(program.id(), 1L, ImmutableList.of(question.getId()));

    ProgramDefinition found =
        ps.updateProgramDefinition(
                program.id(),
                Locale.US,
                "new description",
                "name",
                "description",
                "",
                DisplayMode.PUBLIC.getValue())
            .getResult();

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void updateProgram_returnsErrors() throws Exception {
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateProgramDefinition(
            program.id(), Locale.US, "", "", "", "", DisplayMode.PUBLIC.getValue());

    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsOnly(
            CiviFormError.of("program admin description cannot be blank"),
            CiviFormError.of("program display name cannot be blank"),
            CiviFormError.of("program display description cannot be blank"));
  }

  @Test
  public void getProgramDefinition() throws Exception {
    ProgramDefinition programDefinition = ProgramBuilder.newDraftProgram().buildDefinition();
    ProgramDefinition found = ps.getProgramDefinition(programDefinition.id());

    assertThat(found.adminName()).isEqualTo(programDefinition.adminName());
  }

  @Test
  public void getProgramDefinition_throwsWhenProgramNotFound() {
    assertThatThrownBy(() -> ps.getProgramDefinition(1L))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("Program not found for ID");
  }

  @Test
  public void getProgramDefinition_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();
    ps.addQuestionsToBlock(program.id(), 1L, ImmutableList.of(question.getId()));

    ProgramDefinition found = ps.getProgramDefinition(program.id());

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void getProgramDefinitionAsync_getsRequestedProgram() {
    ProgramDefinition programDefinition = ProgramBuilder.newDraftProgram().buildDefinition();

    CompletionStage<ProgramDefinition> found = ps.getProgramDefinitionAsync(programDefinition.id());

    assertThat(found.toCompletableFuture().join().adminName())
        .isEqualTo(programDefinition.adminName());
  }

  @Test
  public void getProgramDefinitionAsync_cannotFindRequestedProgram_throwsException() {
    ProgramDefinition programDefinition = ProgramBuilder.newDraftProgram().buildDefinition();

    CompletionStage<ProgramDefinition> found =
        ps.getProgramDefinitionAsync(programDefinition.id() + 1);

    assertThatThrownBy(() -> found.toCompletableFuture().join())
        .isInstanceOf(CompletionException.class)
        .hasCauseInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("Program not found for ID");
  }

  @Test
  public void getProgramDefinitionAsync_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .buildDefinition();

    ProgramDefinition found =
        ps.getProgramDefinitionAsync(program.id()).toCompletableFuture().join();

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void addBlockToProgram_noProgram_throwsProgramNotFoundException() {
    assertThatThrownBy(() -> ps.addBlockToProgram(1L))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessage("Program not found for ID: 1");
  }

  @Test
  public void addBlockToProgram_emptyBlock_returnsProgramDefinitionWithBlock() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram().withBlock("Screen 1").buildDefinition();
    ErrorAnd<ProgramBlockAdditionResult, CiviFormError> result =
        ps.addBlockToProgram(programDefinition.id());

    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();
    ProgramDefinition updatedProgramDefinition = result.getResult().program();
    assertThat(result.getResult().maybeAddedBlock()).isNotEmpty();
    BlockDefinition addedBlock = result.getResult().maybeAddedBlock().get();

    ProgramDefinition found = ps.getProgramDefinition(programDefinition.id());

    assertThat(found.blockDefinitions()).hasSize(2);
    assertThat(found.blockDefinitions())
        .containsExactlyElementsOf(updatedProgramDefinition.blockDefinitions());

    BlockDefinition emptyBlock = found.blockDefinitions().get(0);
    assertThat(emptyBlock.name()).isEqualTo("Screen 1");
    assertThat(emptyBlock.description()).isEqualTo("");
    assertThat(emptyBlock.programQuestionDefinitions()).hasSize(0);

    BlockDefinition newBlock = found.blockDefinitions().get(1);
    assertThat(newBlock.id()).isEqualTo(addedBlock.id());
    assertThat(newBlock.name()).isEqualTo("Screen 2");
    assertThat(newBlock.description()).isNotEmpty();
    assertThat(newBlock.programQuestionDefinitions()).hasSize(0);
  }

  @Test
  public void addBlockToProgram_returnsProgramDefinitionWithBlock() throws Exception {
    ProgramDefinition programDefinition = ProgramBuilder.newDraftProgram().buildDefinition();
    long programId = programDefinition.id();

    ErrorAnd<ProgramBlockAdditionResult, CiviFormError> result =
        ps.addBlockToProgram(programDefinition.id());

    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();
    ProgramDefinition updatedProgramDefinition = result.getResult().program();
    assertThat(result.getResult().maybeAddedBlock()).isNotEmpty();
    BlockDefinition addedBlock = result.getResult().maybeAddedBlock().get();

    ProgramDefinition found = ps.getProgramDefinition(programId);

    assertThat(found.blockDefinitions()).hasSize(2);
    assertThat(found.blockDefinitions())
        .containsExactlyElementsOf(updatedProgramDefinition.blockDefinitions());
    assertThat(found.blockDefinitions().get(1).id()).isEqualTo(addedBlock.id());
  }

  @Test
  public void addRepeatedBlockToProgram() throws Exception {
    Program program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMembers())
            .withRepeatedBlock()
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMemberJobs())
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build();

    ErrorAnd<ProgramBlockAdditionResult, CiviFormError> result =
        ps.addRepeatedBlockToProgram(program.id, 1L);

    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();
    ProgramDefinition updatedProgramDefinition = result.getResult().program();
    assertThat(result.getResult().maybeAddedBlock()).isNotEmpty();
    BlockDefinition addedBlock = result.getResult().maybeAddedBlock().get();

    ProgramDefinition found = ps.getProgramDefinition(program.id);

    assertThat(found.blockDefinitions()).hasSize(4);
    assertThat(found.getBlockDefinitionByIndex(0).get().isEnumerator()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(0).get().isRepeated()).isFalse();
    assertThat(found.getBlockDefinitionByIndex(0).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMembers().getQuestionDefinition());

    assertThat(found.getBlockDefinitionByIndex(1).get().isEnumerator()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(1).get().isRepeated()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(1).get().enumeratorId()).contains(1L);
    assertThat(found.getBlockDefinitionByIndex(1).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMemberJobs().getQuestionDefinition());

    // The newly added block.
    assertThat(found.getBlockDefinitionByIndex(2).get().isRepeated()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(2).get().enumeratorId()).contains(1L);
    assertThat(found.getBlockDefinitionByIndex(2).get().getQuestionCount()).isEqualTo(0);
    assertThat(found.getBlockDefinitionByIndex(2).get().id()).isEqualTo(addedBlock.id());

    assertThat(found.getBlockDefinitionByIndex(3).get().isRepeated()).isFalse();
    assertThat(found.getBlockDefinitionByIndex(3).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantFavoriteColor().getQuestionDefinition());

    assertThat(found.blockDefinitions())
        .containsExactlyElementsOf(updatedProgramDefinition.blockDefinitions());
  }

  @Test
  public void addRepeatedBlockToProgram_toEndOfBlockList() throws Exception {
    Program program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMembers())
            .withRepeatedBlock()
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMemberJobs())
            .build();

    ErrorAnd<ProgramBlockAdditionResult, CiviFormError> result =
        ps.addRepeatedBlockToProgram(program.id, 2L);

    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();
    ProgramDefinition updatedProgramDefinition = result.getResult().program();
    assertThat(result.getResult().maybeAddedBlock()).isNotEmpty();
    BlockDefinition addedBlock = result.getResult().maybeAddedBlock().get();

    ProgramDefinition found = ps.getProgramDefinition(program.id);

    assertThat(found.blockDefinitions()).hasSize(4);
    assertThat(found.getBlockDefinitionByIndex(0).get().isEnumerator()).isFalse();
    assertThat(found.getBlockDefinitionByIndex(0).get().isRepeated()).isFalse();
    assertThat(found.getBlockDefinitionByIndex(0).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantFavoriteColor().getQuestionDefinition());

    assertThat(found.getBlockDefinitionByIndex(1).get().isEnumerator()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(1).get().isRepeated()).isFalse();
    assertThat(found.getBlockDefinitionByIndex(1).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMembers().getQuestionDefinition());

    assertThat(found.getBlockDefinitionByIndex(2).get().isEnumerator()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(2).get().isRepeated()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(2).get().enumeratorId()).contains(2L);
    assertThat(found.getBlockDefinitionByIndex(2).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.applicantHouseholdMemberJobs().getQuestionDefinition());

    // The newly added block.
    assertThat(found.getBlockDefinitionByIndex(3).get().isRepeated()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(3).get().enumeratorId()).contains(2L);
    assertThat(found.getBlockDefinitionByIndex(3).get().getQuestionCount()).isEqualTo(0);
    assertThat(found.blockDefinitions())
        .containsExactlyElementsOf(updatedProgramDefinition.blockDefinitions());
    assertThat(found.getBlockDefinitionByIndex(3).get().id()).isEqualTo(addedBlock.id());
  }

  @Test
  public void addRepeatedBlockToProgram_invalidProgramId_throwsProgramNotFoundException() {
    assertThatThrownBy(() -> ps.addRepeatedBlockToProgram(1L, 1L))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void
      addRepeatedBlockToProgram_invalidEnumeratorId_throwsProgramBlockDefinitionNotFoundException() {
    Program program = ProgramBuilder.newActiveProgram().build();

    assertThatThrownBy(() -> ps.addRepeatedBlockToProgram(program.id, 5L))
        .isInstanceOf(ProgramBlockDefinitionNotFoundException.class);
  }

  @Test
  public void updateBlock_noProgram_throwsProgramNotFoundException() {
    assertThatThrownBy(() -> ps.updateBlock(1L, 1L, new BlockForm("block", "description")))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessage("Program not found for ID: 1");
  }

  @Test
  public void updateBlock_invalidBlock_returnsErrors() throws Exception {
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateBlock(program.id(), 1L, new BlockForm());

    // Returns the unmodified program definition.
    assertThat(result.hasResult()).isTrue();
    assertThat(result.getResult().adminName()).isEqualTo(program.adminName());
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsOnly(
            CiviFormError.of("screen name cannot be blank"),
            CiviFormError.of("screen description cannot be blank"));
  }

  @Test
  public void updateBlock() throws Exception {
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();
    BlockForm blockForm = new BlockForm();
    blockForm.setName("new screen name");
    blockForm.setDescription("new description");

    ErrorAnd<ProgramDefinition, CiviFormError> result = ps.updateBlock(program.id(), 1L, blockForm);
    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();

    ProgramDefinition found = ps.getProgramDefinition(program.id());

    assertThat(found.blockDefinitions()).hasSize(1);
    assertThat(found.getBlockDefinition(1L).name()).isEqualTo("new screen name");
    assertThat(found.getBlockDefinition(1L).description()).isEqualTo("new description");
  }

  @Test
  public void setBlockQuestions_updatesBlock() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition programDefinition = ProgramBuilder.newDraftProgram().buildDefinition();
    Long programId = programDefinition.id();

    ps.setBlockQuestions(
        programId,
        1L,
        ImmutableList.of(ProgramQuestionDefinition.create(question, Optional.of(programId))));
    ProgramDefinition found = ps.getProgramDefinition(programId);

    assertThat(found.blockDefinitions()).hasSize(1);

    BlockDefinition foundBlock = found.blockDefinitions().get(0);
    assertThat(foundBlock.programQuestionDefinitions()).hasSize(1);

    ProgramQuestionDefinition foundPqd =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0);
    assertThat(foundPqd.id()).isEqualTo(question.getId());
    assertThat(foundPqd.getQuestionDefinition()).isInstanceOf(NameQuestionDefinition.class);
    assertThat(foundPqd.getQuestionDefinition().getName()).isEqualTo("applicant name");
  }

  @Test
  public void setBlockQuestions_withBogusBlockId_throwsProgramBlockDefinitionNotFoundException() {
    ProgramDefinition p =
        ps.createProgramDefinition(
                "name", "description", "name", "description", "", DisplayMode.PUBLIC.getValue())
            .getResult();
    assertThatThrownBy(() -> ps.setBlockQuestions(p.id(), 100L, ImmutableList.of()))
        .isInstanceOf(ProgramBlockDefinitionNotFoundException.class)
        .hasMessage(
            String.format(
                "Block not found in Program (ID %d) for block definition ID 100", p.id()));
  }

  @Test
  public void setBlockQuestions_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition programDefinition = ProgramBuilder.newDraftProgram().buildDefinition();
    Long programId = programDefinition.id();

    ProgramDefinition found =
        ps.setBlockQuestions(
            programId,
            1L,
            ImmutableList.of(ProgramQuestionDefinition.create(question, Optional.of(programId))));
    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void setBlockQuestions_overwritesPredicateQuestion_throwsException() {
    QuestionDefinition question = nameQuestion;
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    question.getId(), Scalar.FIRST_NAME, Operator.EQUAL_TO, PredicateValue.of(""))),
            PredicateAction.HIDE_BLOCK);
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .withBlock()
            .withPredicate(predicate)
            .buildDefinition();

    // Overwriting the question in the first block invalidates the predicate in the second block.
    assertThatExceptionOfType(IllegalPredicateOrderingException.class)
        .isThrownBy(
            () ->
                ps.setBlockQuestions(
                    program.id(),
                    1L,
                    ImmutableList.of(
                        ProgramQuestionDefinition.create(
                            addressQuestion, Optional.of(program.id())))))
        .withMessage("This action would invalidate a block condition");
  }

  @Test
  public void
      addQuestionsToBlock_withDuplicatedQuestions_throwsDuplicateProgramQuestionException() {
    QuestionDefinition questionA = nameQuestion;

    Program program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(questionA)
            .build();

    assertThatThrownBy(
            () -> ps.addQuestionsToBlock(program.id, 1L, ImmutableList.of(questionA.getId())))
        .isInstanceOf(DuplicateProgramQuestionException.class)
        .hasMessage(
            String.format(
                "Question (ID %d) already exists in Program (ID %d)",
                questionA.getId(), program.id));
    ;
  }

  @Test
  public void addQuestionsToBlock_addsQuestionsToTheBlock() throws Exception {
    QuestionDefinition questionA = nameQuestion;
    QuestionDefinition questionB = addressQuestion;

    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(questionA)
            .buildDefinition();

    program = ps.addQuestionsToBlock(program.id(), 1L, ImmutableList.of(questionB.getId()));

    assertThat(program.hasQuestion(questionA)).isTrue();
    assertThat(program.hasQuestion(questionB)).isTrue();
  }

  @Test
  public void removeQuestionsFromBlock_withoutQuestion_throwsQuestionNotFoundException()
      throws Exception {
    QuestionDefinition questionA = nameQuestion;
    Program program = ProgramBuilder.newDraftProgram().withBlock().build();

    assertThatThrownBy(
            () -> ps.removeQuestionsFromBlock(program.id, 1L, ImmutableList.of(questionA.getId())))
        .isInstanceOf(QuestionNotFoundException.class)
        .hasMessage(
            String.format(
                "Question (ID %d) not found in Program (ID %d)", questionA.getId(), program.id));
    ;
  }

  @Test
  public void removeQuestionsFromBlock_removesQuestionsFromTheBlock() throws Exception {
    QuestionDefinition questionA = nameQuestion;
    QuestionDefinition questionB = addressQuestion;

    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(questionA)
            .withRequiredQuestionDefinition(questionB)
            .buildDefinition();

    program = ps.removeQuestionsFromBlock(program.id(), 1L, ImmutableList.of(questionB.getId()));

    assertThat(program.hasQuestion(questionA)).isTrue();
    assertThat(program.hasQuestion(questionB)).isFalse();
  }

  @Test
  public void removeQuestionsFromBlock_invalidatesPredicate_throwsException() {
    QuestionDefinition question = nameQuestion;
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    question.getId(), Scalar.FIRST_NAME, Operator.EQUAL_TO, PredicateValue.of(""))),
            PredicateAction.HIDE_BLOCK);
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .withBlock()
            .withPredicate(predicate)
            .buildDefinition();

    assertThatExceptionOfType(IllegalPredicateOrderingException.class)
        .isThrownBy(
            () -> ps.removeQuestionsFromBlock(program.id(), 1L, ImmutableList.of(question.getId())))
        .withMessage("This action would invalidate a block condition");
  }

  @Test
  public void setBlockPredicate_updatesBlock() throws Exception {
    Question question = testQuestionBank.applicantAddress();
    Program program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(question)
            .withBlock()
            .build();

    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    question.id, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of(""))),
            PredicateAction.HIDE_BLOCK);
    ps.setBlockPredicate(program.id, 2L, predicate);

    ProgramDefinition found = ps.getProgramDefinition(program.id);

    assertThat(found.blockDefinitions().get(1).visibilityPredicate()).hasValue(predicate);
  }

  @Test
  public void setBlockPredicate_withBogusBlockId_throwsProgramBlockDefinitionNotFoundException() {
    ProgramDefinition p = ProgramBuilder.newDraftProgram().buildDefinition();
    assertThatThrownBy(
            () ->
                ps.setBlockPredicate(
                    p.id(),
                    100L,
                    PredicateDefinition.create(
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.create(
                                1L, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of(""))),
                        PredicateAction.HIDE_BLOCK)))
        .isInstanceOf(ProgramBlockDefinitionNotFoundException.class)
        .hasMessage(
            String.format(
                "Block not found in Program (ID %d) for block definition ID 100", p.id()));
  }

  @Test
  public void setBlockPredicate_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .withBlock()
            .buildDefinition();
    Long programId = programDefinition.id();

    ProgramDefinition found =
        ps.setBlockPredicate(
            programId,
            2L,
            PredicateDefinition.create(
                PredicateExpressionNode.create(
                    LeafOperationExpressionNode.create(
                        question.getId(), Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of(""))),
                PredicateAction.HIDE_BLOCK));

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void setBlockPredicate_illegalPredicate_throwsException() {
    QuestionDefinition question = nameQuestion;
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    question.getId(), Scalar.FIRST_NAME, Operator.EQUAL_TO, PredicateValue.of(""))),
            PredicateAction.HIDE_BLOCK);
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(addressQuestion)
            .withBlock()
            .buildDefinition();

    // This predicate depends on a question that doesn't exist in a prior block.
    assertThatExceptionOfType(IllegalPredicateOrderingException.class)
        .isThrownBy(() -> ps.setBlockPredicate(program.id(), 2L, predicate))
        .withMessage("This action would invalidate a block condition");
  }

  @Test
  public void removeBlockPredicate() throws Exception {
    Program program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(addressQuestion)
            .withBlock()
            .build();

    // First set the predicate and assert its presence.
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    addressQuestion.getId(),
                    Scalar.CITY,
                    Operator.EQUAL_TO,
                    PredicateValue.of(""))),
            PredicateAction.HIDE_BLOCK);
    ps.setBlockPredicate(program.id, 2L, predicate);

    ProgramDefinition foundWithPredicate = ps.getProgramDefinition(program.id);
    assertThat(foundWithPredicate.blockDefinitions().get(1).visibilityPredicate())
        .hasValue(predicate);

    // Then remove that predicate and assert its absence.
    ps.removeBlockPredicate(program.id, 2L);

    ProgramDefinition foundWithoutPredicate = ps.getProgramDefinition(program.id);
    assertThat(foundWithoutPredicate.blockDefinitions().get(1).visibilityPredicate()).isEmpty();
  }

  @Test
  public void setProgramQuestionDefinitionOptionality() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .buildDefinition();
    Long programId = programDefinition.id();

    assertThat(
            programDefinition
                .getBlockDefinitionByIndex(0)
                .get()
                .programQuestionDefinitions()
                .get(0)
                .optional())
        .isFalse();

    programDefinition =
        ps.setProgramQuestionDefinitionOptionality(programId, 1L, nameQuestion.getId(), true);
    assertThat(
            programDefinition
                .getBlockDefinitionByIndex(0)
                .get()
                .programQuestionDefinitions()
                .get(0)
                .optional())
        .isTrue();

    programDefinition =
        ps.setProgramQuestionDefinitionOptionality(programId, 1L, nameQuestion.getId(), false);
    assertThat(
            programDefinition
                .getBlockDefinitionByIndex(0)
                .get()
                .programQuestionDefinitions()
                .get(0)
                .optional())
        .isFalse();

    // Checking that there's no problem
    assertThatThrownBy(
            () ->
                ps.setProgramQuestionDefinitionOptionality(
                    programId, 1L, nameQuestion.getId() + 1, false))
        .isInstanceOf(ProgramQuestionDefinitionNotFoundException.class);
  }

  @Test
  public void deleteBlock_invalidProgram_throwsProgramNotfoundException() {
    assertThatThrownBy(() -> ps.deleteBlock(1L, 2L))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessage("Program not found for ID: 1");
  }

  @Test
  public void deleteBlock_lastBlock_throwsProgramNeedsABlockException() throws Exception {
    Program program = ProgramBuilder.newDraftProgram().build();

    assertThatThrownBy(() -> ps.deleteBlock(program.id, 1L))
        .isInstanceOf(ProgramNeedsABlockException.class);
  }

  @Test
  public void deleteBlock_removesPredicateQuestion_throwsException() {
    QuestionDefinition question = nameQuestion;
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    question.getId(), Scalar.FIRST_NAME, Operator.EQUAL_TO, PredicateValue.of(""))),
            PredicateAction.HIDE_BLOCK);
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(addressQuestion)
            .withBlock()
            .withPredicate(predicate)
            .buildDefinition();

    // This predicate depends on a question that doesn't exist in a prior block.
    assertThatExceptionOfType(IllegalPredicateOrderingException.class)
        .isThrownBy(() -> ps.deleteBlock(program.id(), 1L))
        .withMessage("This action would invalidate a block condition");
  }

  @Test
  public void deleteBlock_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram()
            .withBlock("screen one")
            .withRequiredQuestionDefinition(question)
            .withBlock()
            .buildDefinition();
    Long programId = programDefinition.id();

    ProgramDefinition result = ps.deleteBlock(programId, 2L);

    assertThat(result.blockDefinitions()).hasSize(1);

    BlockDefinition blockResult = result.blockDefinitions().get(0);
    assertThat(blockResult.name()).isEqualTo("screen one");
    assertThat(blockResult.programQuestionDefinitions()).hasSize(1);

    QuestionDefinition questionResult =
        blockResult.programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(questionResult).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void newProgramFromExisting() throws Exception {
    Program program = ProgramBuilder.newActiveProgram().build();
    program.save();

    ProgramDefinition newDraft = ps.newDraftOf(program.id);
    assertThat(newDraft.adminName()).isEqualTo(program.getProgramDefinition().adminName());
    assertThat(newDraft.blockDefinitions())
        .isEqualTo(program.getProgramDefinition().blockDefinitions());
    assertThat(newDraft.localizedDescription())
        .isEqualTo(program.getProgramDefinition().localizedDescription());
    assertThat(newDraft.id()).isNotEqualTo(program.getProgramDefinition().id());

    ProgramDefinition secondNewDraft = ps.newDraftOf(program.id);
    assertThat(secondNewDraft.id()).isEqualTo(newDraft.id());
  }

  @Test
  public void updateLocalizations_addsNewLocale() throws Exception {
    Program program = ProgramBuilder.newDraftProgram().build();

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateLocalization(program.id, Locale.GERMAN, "German Name", "German Description");

    assertThat(result.isError()).isFalse();
    ProgramDefinition definition = result.getResult();
    assertThat(definition.localizedName().get(Locale.GERMAN)).isEqualTo("German Name");
    assertThat(definition.localizedDescription().get(Locale.GERMAN))
        .isEqualTo("German Description");
  }

  @Test
  public void updateLocalizations_updatesExistingLocale() throws Exception {
    Program program = ProgramBuilder.newDraftProgram("English name", "English description").build();

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateLocalization(program.id, Locale.US, "new name", "new description");

    assertThat(result.isError()).isFalse();
    ProgramDefinition definition = result.getResult();
    assertThat(definition.localizedName().get(Locale.US)).isEqualTo("new name");
    assertThat(definition.localizedDescription().get(Locale.US)).isEqualTo("new description");
  }

  @Test
  public void updateLocalizations_returnsErrorMessages() throws Exception {
    Program program = ProgramBuilder.newDraftProgram().build();

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateLocalization(program.id, Locale.US, "", "");

    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsExactly(
            CiviFormError.of("program display name cannot be blank"),
            CiviFormError.of("program display description cannot be blank"));
  }

  @Test
  public void updateLocalizations_programNotFound_throws() {
    assertThatThrownBy(() -> ps.updateLocalization(1000L, Locale.US, "", ""))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("Program not found for ID: 1000");
  }

  @Test
  public void getNotificationEmailAddresses() {
    String programName = "administered program";
    Program program = resourceCreator.insertActiveProgram(programName);
    program.save();

    // If there are no admins (uncommon), return empty.
    assertThat(ps.getNotificationEmailAddresses(programName)).isEmpty();

    String globalAdminEmail = "global@admin";
    Account globalAdmin = new Account();
    globalAdmin.setEmailAddress(globalAdminEmail);
    globalAdmin.setGlobalAdmin(true);
    globalAdmin.save();

    // If there are no program admins, return global admins.
    assertThat(ps.getNotificationEmailAddresses(programName)).containsExactly(globalAdminEmail);

    String programAdminEmail = "program@admin";
    Account programAdmin = new Account();
    programAdmin.setEmailAddress(programAdminEmail);
    programAdmin.addAdministeredProgram(program.getProgramDefinition());
    programAdmin.save();

    // Return program admins when there are.
    assertThat(ps.getNotificationEmailAddresses(programName)).containsExactly(programAdminEmail);
  }

  @Test
  public void getProgramDefinitionAsync_reordersBlocksOnRead() throws Exception {
    long programId = ProgramBuilder.newActiveProgram().build().id;
    ImmutableList<BlockDefinition> unorderedBlockDefinitions =
        ImmutableList.<BlockDefinition>builder()
            .add(
                BlockDefinition.builder()
                    .setId(1L)
                    .setName("enumerator")
                    .setDescription("description")
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank.applicantHouseholdMembers().getQuestionDefinition(),
                            Optional.of(programId)))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(2L)
                    .setName("top level")
                    .setDescription("description")
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank.applicantEmail().getQuestionDefinition(),
                            Optional.of(programId)))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(3L)
                    .setName("nested enumerator")
                    .setDescription("description")
                    .setEnumeratorId(Optional.of(1L))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank.applicantHouseholdMemberJobs().getQuestionDefinition(),
                            Optional.of(programId)))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(4L)
                    .setName("repeated")
                    .setDescription("description")
                    .setEnumeratorId(Optional.of(1L))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank.applicantHouseholdMemberName().getQuestionDefinition(),
                            Optional.of(programId)))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(5L)
                    .setName("nested repeated")
                    .setDescription("description")
                    .setEnumeratorId(Optional.of(3L))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank
                                .applicantHouseholdMemberDaysWorked()
                                .getQuestionDefinition(),
                            Optional.of(programId)))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(6L)
                    .setName("top level 2")
                    .setDescription("description")
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank.applicantName().getQuestionDefinition(),
                            Optional.of(programId)))
                    .build())
            .build();
    ObjectMapper mapper =
        new ObjectMapper().registerModule(new GuavaModule()).registerModule(new Jdk8Module());

    // Directly update the table with DB.sqlUpdate and execute. We can't save it through
    // the ebean model because the preupdate method will correct block ordering, and we
    // want to test that legacy block order is corrected on read.
    String updateString =
        String.format(
            "UPDATE programs SET block_definitions='%s' WHERE id=%d;",
            mapper.writeValueAsString(unorderedBlockDefinitions), programId);
    DB.sqlUpdate(updateString).execute();

    ProgramDefinition found = ps.getProgramDefinitionAsync(programId).toCompletableFuture().get();

    assertThat(found.hasOrderedBlockDefinitions()).isTrue();
  }

  @Test
  public void getStatuses_programNotFound_throws() throws Exception {
    assertThatThrownBy(() -> ps.getStatuses(1000L))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("Program not found for ID: 1000");
  }

  @Test
  public void getStatuses_none() throws Exception {
    Program program = ProgramBuilder.newActiveProgram().build();
    assertThat(ps.getStatuses(program.id).getStatuses()).isEmpty();
  }

  @Test
  public void updateStatuses_programNotFound_throws() throws Exception {

    assertThatThrownBy(() -> ps.setStatuses(1000L, new StatusDefinitions()))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("Program not found for ID: 1000");
  }

  @Test
  public void updateAndGetStatuses() throws Exception {
    Program program = ProgramBuilder.newActiveProgram().build();

    var status =
        StatusDefinitions.Status.builder()
            .setStatusText("Approved")
            .setLocalizedStatusText(LocalizedStrings.of(Locale.US, "Approved"))
            .setEmailBodyText("I'm an email!")
            .setLocalizedEmailBodyText(LocalizedStrings.of(Locale.US, "I'm a US email!"))
            .build();

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.setStatuses(program.id, new StatusDefinitions(ImmutableList.of(status)));

    assertThat(result.isError()).isFalse();
    StatusDefinitions gotStatusDef = result.getResult().statusDefinitions();
    assertThat(gotStatusDef.getStatuses()).containsExactly(status);
  }
}
