package services.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import forms.BlockForm;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import models.LifecycleStage;
import models.Program;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.CiviFormError;
import services.ErrorAnd;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.TextQuestionDefinition;
import support.ProgramBuilder;

public class ProgramServiceImplTest extends WithPostgresContainer {

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
  public void listProgramDefinitions_hasNoResults() {
    ImmutableList<ProgramDefinition> programDefinitions = ps.listProgramDefinitions();

    assertThat(programDefinitions).isEmpty();
  }

  @Test
  public void listProgramDefinitions_hasResults() {
    ProgramDefinition first = ProgramBuilder.newProgram("first").buildDefinition();
    ProgramDefinition second = ProgramBuilder.newProgram("second").buildDefinition();

    ImmutableList<ProgramDefinition> programDefinitions = ps.listProgramDefinitions();

    assertThat(programDefinitions).containsExactly(first, second);
  }

  @Test
  public void listProgramDefinitions_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    Program program = ProgramBuilder.newProgram().build();
    ps.addQuestionsToBlock(program.id, 1L, ImmutableList.of(question.getId()));

    ImmutableList<ProgramDefinition> programDefinitions = ps.listProgramDefinitions();

    QuestionDefinition foundQuestion =
        programDefinitions
            .get(0)
            .blockDefinitions()
            .get(0)
            .programQuestionDefinitions()
            .get(0)
            .getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void listProgramDefinitionsAsync_hasNoResults() {
    CompletionStage<ImmutableList<ProgramDefinition>> completionStage =
        ps.listProgramDefinitionsAsync();

    assertThat(completionStage.toCompletableFuture().join()).isEmpty();
  }

  @Test
  public void listProgramDefinitionsAsync_hasResults() {
    ProgramDefinition first = ProgramBuilder.newProgram("first").buildDefinition();
    ProgramDefinition second = ProgramBuilder.newProgram("second").buildDefinition();

    CompletionStage<ImmutableList<ProgramDefinition>> completionStage =
        ps.listProgramDefinitionsAsync();

    assertThat(completionStage.toCompletableFuture().join()).containsExactly(first, second);
  }

  @Test
  public void listProgramDefinitionsAsync_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition program = ProgramBuilder.newProgram().buildDefinition();
    ps.addQuestionsToBlock(program.id(), 1L, ImmutableList.of(question.getId()));

    ImmutableList<ProgramDefinition> programDefinitions =
        ps.listProgramDefinitionsAsync().toCompletableFuture().join();

    QuestionDefinition foundQuestion =
        programDefinitions
            .get(0)
            .blockDefinitions()
            .get(0)
            .programQuestionDefinitions()
            .get(0)
            .getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void syncQuestions_constructsAllQuestionDefinitions() throws Exception {
    QuestionDefinition questionOne = nameQuestion;
    QuestionDefinition questionTwo = addressQuestion;
    QuestionDefinition questionThree = colorQuestion;

    ProgramBuilder.newProgram()
        .withBlock()
        .withQuestionDefinition(questionOne)
        .withQuestionDefinition(questionTwo)
        .withBlock()
        .withQuestionDefinition(questionThree)
        .buildDefinition();
    ProgramBuilder.newProgram()
        .withBlock()
        .withQuestionDefinition(questionTwo)
        .withBlock()
        .withQuestionDefinition(questionOne)
        .buildDefinition();

    ImmutableList<ProgramDefinition> programDefinitions = ps.listProgramDefinitions();

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
    assertThat(ps.listProgramDefinitions()).isEmpty();

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition("ProgramService", "description", "name", "description");

    assertThat(result.hasResult()).isTrue();
    assertThat(result.getResult().id()).isNotNull();
  }

  @Test
  public void createProgram_hasEmptyBlock() {
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition("ProgramService", "description", "name", "description");

    assertThat(result.hasResult()).isTrue();
    assertThat(result.getResult().blockDefinitions()).hasSize(1);
    assertThat(result.getResult().getBlockDefinitionByIndex(0).get().id()).isEqualTo(1L);
    assertThat(result.getResult().getBlockDefinitionByIndex(0).get().name()).isEqualTo("Block 1");
  }

  @Test
  public void createProgram_returnsErrors() {
    ErrorAnd<ProgramDefinition, CiviFormError> result = ps.createProgramDefinition("", "", "", "");

    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsOnly(
            CiviFormError.of("program admin name cannot be blank"),
            CiviFormError.of("program admin description cannot be blank"),
            CiviFormError.of("program display name cannot be blank"),
            CiviFormError.of("program display description cannot be blank"));
  }

  @Test
  public void updateProgram_withNoProgram_throwsProgramNotFoundException() {
    assertThatThrownBy(
            () ->
                ps.updateProgramDefinition(1L, Locale.US, "new description", "name", "description"))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessage("Program not found for ID: 1");
  }

  @Test
  public void updateProgram_updatesProgram() throws Exception {
    ProgramDefinition originalProgram =
        ProgramBuilder.newProgram("original", "original description").buildDefinition();
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateProgramDefinition(
            originalProgram.id(), Locale.US, "new description", "name", "description");

    assertThat(result.hasResult()).isTrue();
    ProgramDefinition updatedProgram = result.getResult();

    ProgramDefinition found = ps.getProgramDefinition(updatedProgram.id());

    assertThat(ps.listProgramDefinitions()).hasSize(1);
    assertThat(found).isEqualTo(updatedProgram);
  }

  @Test
  public void updateProgram_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition program = ProgramBuilder.newProgram().buildDefinition();
    ps.addQuestionsToBlock(program.id(), 1L, ImmutableList.of(question.getId()));

    ProgramDefinition found =
        ps.updateProgramDefinition(
                program.id(), Locale.US, "new description", "name", "description")
            .getResult();

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void updateProgram_returnsErrors() throws Exception {
    ProgramDefinition program = ProgramBuilder.newProgram().buildDefinition();

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateProgramDefinition(program.id(), Locale.US, "", "", "");

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
    ProgramDefinition programDefinition = ProgramBuilder.newProgram().buildDefinition();
    ProgramDefinition found = ps.getProgramDefinition(programDefinition.id());

    assertThat(found).isEqualTo(programDefinition);
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
    ProgramDefinition program = ProgramBuilder.newProgram().buildDefinition();
    ps.addQuestionsToBlock(program.id(), 1L, ImmutableList.of(question.getId()));

    ProgramDefinition found = ps.getProgramDefinition(program.id());

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void getProgramDefinitionAsync_getsRequestedProgram() {
    ProgramDefinition programDefinition = ProgramBuilder.newProgram().buildDefinition();

    CompletionStage<ProgramDefinition> found = ps.getProgramDefinitionAsync(programDefinition.id());

    assertThat(found.toCompletableFuture().join()).isEqualTo(programDefinition);
  }

  @Test
  public void getProgramDefinitionAsync_cannotFindRequestedProgram_throwsException() {
    ProgramDefinition programDefinition = ProgramBuilder.newProgram().buildDefinition();

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
        ProgramBuilder.newProgram().withBlock().withQuestionDefinition(question).buildDefinition();

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
        ProgramBuilder.newProgram().withBlock("Block 1").buildDefinition();
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.addBlockToProgram(programDefinition.id());

    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();
    ProgramDefinition updatedProgramDefinition = result.getResult();

    ProgramDefinition found = ps.getProgramDefinition(programDefinition.id());

    assertThat(found.blockDefinitions()).hasSize(2);
    assertThat(found.blockDefinitions())
        .containsExactlyElementsOf(updatedProgramDefinition.blockDefinitions());

    BlockDefinition emptyBlock = found.blockDefinitions().get(0);
    assertThat(emptyBlock.name()).isEqualTo("Block 1");
    assertThat(emptyBlock.description()).isEqualTo("");
    assertThat(emptyBlock.programQuestionDefinitions()).hasSize(0);

    BlockDefinition newBlock = found.blockDefinitions().get(1);
    assertThat(newBlock.name()).isEqualTo("Block 2");
    assertThat(newBlock.description()).isNotEmpty();
    assertThat(newBlock.programQuestionDefinitions()).hasSize(0);
  }

  @Test
  public void addBlockToProgram_returnsProgramDefinitionWithBlock() throws Exception {
    ProgramDefinition programDefinition = ProgramBuilder.newProgram().buildDefinition();
    long programId = programDefinition.id();

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.addBlockToProgram(programDefinition.id());

    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();
    ProgramDefinition updatedProgramDefinition = result.getResult();

    ProgramDefinition found = ps.getProgramDefinition(programId);

    assertThat(found.blockDefinitions()).hasSize(2);
    assertThat(found.blockDefinitions())
        .containsExactlyElementsOf(updatedProgramDefinition.blockDefinitions());
  }

  @Test
  public void addRepeatedBlockToProgram() throws Exception {
    Question repeatedQuestion = testQuestionBank.applicantHouseholdMembers();
    Program program =
        ProgramBuilder.newProgram().withBlock().withQuestion(repeatedQuestion).build();

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.addRepeatedBlockToProgram(program.id, 1L);

    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();
    ProgramDefinition updatedProgramDefinition = result.getResult();

    ProgramDefinition found = ps.getProgramDefinition(program.id);

    assertThat(found.blockDefinitions()).hasSize(2);
    assertThat(found.getBlockDefinitionByIndex(0).get().isRepeater()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(1).get().isRepeated()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(1).get().repeaterId()).contains(1L);
    assertThat(found.blockDefinitions())
        .containsExactlyElementsOf(updatedProgramDefinition.blockDefinitions());
  }

  @Test
  public void addRepeatedBlockToProgram_invalidProgramId_throwsProgramNotFoundException() {
    assertThatThrownBy(() -> ps.addRepeatedBlockToProgram(1L, 1L))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void addRepeatedBlockToProgram_invalidRepeaterId_throwsProgramBlockNotFoundException() {
    Program program = ProgramBuilder.newProgram().build();

    assertThatThrownBy(() -> ps.addRepeatedBlockToProgram(program.id, 5L))
        .isInstanceOf(ProgramBlockNotFoundException.class);
  }

  @Test
  public void updateBlock_noProgram_throwsProgramNotFoundException() {
    assertThatThrownBy(() -> ps.updateBlock(1L, 1L, new BlockForm("block", "description")))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessage("Program not found for ID: 1");
  }

  @Test
  public void updateBlock_invalidBlock_returnsErrors() throws Exception {
    ProgramDefinition program = ProgramBuilder.newProgram().buildDefinition();
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateBlock(program.id(), 1L, new BlockForm());

    // Returns the unmodified program definition.
    assertThat(result.hasResult()).isTrue();
    assertThat(result.getResult()).isEqualTo(program);
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsOnly(
            CiviFormError.of("block name cannot be blank"),
            CiviFormError.of("block description cannot be blank"));
  }

  @Test
  public void updateBlock() throws Exception {
    ProgramDefinition program = ProgramBuilder.newProgram().buildDefinition();
    BlockForm blockForm = new BlockForm();
    blockForm.setName("new block name");
    blockForm.setDescription("new description");

    ErrorAnd<ProgramDefinition, CiviFormError> result = ps.updateBlock(program.id(), 1L, blockForm);
    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();

    ProgramDefinition found = ps.getProgramDefinition(program.id());

    assertThat(found.blockDefinitions()).hasSize(1);
    assertThat(found.getBlockDefinition(1L).name()).isEqualTo("new block name");
    assertThat(found.getBlockDefinition(1L).description()).isEqualTo("new description");
  }

  @Test
  public void setBlockQuestions_updatesBlock() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition programDefinition = ProgramBuilder.newProgram().buildDefinition();
    Long programId = programDefinition.id();

    ps.setBlockQuestions(
        programId, 1L, ImmutableList.of(ProgramQuestionDefinition.create(question)));
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
  public void setBlockQuestions_withBogusBlockId_throwsProgramBlockNotFoundException() {
    ProgramDefinition p =
        ps.createProgramDefinition("name", "description", "name", "description").getResult();
    assertThatThrownBy(() -> ps.setBlockQuestions(p.id(), 100L, ImmutableList.of()))
        .isInstanceOf(ProgramBlockNotFoundException.class)
        .hasMessage(
            String.format(
                "Block not found in Program (ID %d) for block definition ID 100", p.id()));
  }

  @Test
  public void setBlockQuestions_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition programDefinition = ProgramBuilder.newProgram().buildDefinition();
    Long programId = programDefinition.id();

    ProgramDefinition found =
        ps.setBlockQuestions(
            programId, 1L, ImmutableList.of(ProgramQuestionDefinition.create(question)));
    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void
      addQuestionsToBlock_withDuplicatedQuestions_throwsDuplicateProgramQuestionException() {
    QuestionDefinition questionA = nameQuestion;

    Program program =
        ProgramBuilder.newProgram().withBlock().withQuestionDefinition(questionA).build();

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
        ProgramBuilder.newProgram().withBlock().withQuestionDefinition(questionA).buildDefinition();

    program = ps.addQuestionsToBlock(program.id(), 1L, ImmutableList.of(questionB.getId()));

    assertThat(program.hasQuestion(questionA)).isTrue();
    assertThat(program.hasQuestion(questionB)).isTrue();
  }

  @Test
  public void removeQuestionsFromBlock_withoutQuestion_throwsQuestionNotFoundException()
      throws Exception {
    QuestionDefinition questionA = nameQuestion;
    Program program = ProgramBuilder.newProgram().withBlock().build();

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
        ProgramBuilder.newProgram()
            .withBlock()
            .withQuestionDefinition(questionA)
            .withQuestionDefinition(questionB)
            .buildDefinition();

    program = ps.removeQuestionsFromBlock(program.id(), 1L, ImmutableList.of(questionB.getId()));

    assertThat(program.hasQuestion(questionA)).isTrue();
    assertThat(program.hasQuestion(questionB)).isFalse();
  }

  @Test
  public void setBlockHidePredicate_updatesBlock() throws Exception {
    Program program = ProgramBuilder.newProgram().build();

    Predicate predicate = Predicate.create("hide predicate");
    ps.setBlockHidePredicate(program.id, 1L, predicate);

    ProgramDefinition found = ps.getProgramDefinition(program.id);

    assertThat(found.blockDefinitions().get(0).hidePredicate()).hasValue(predicate);
  }

  @Test
  public void setBlockHidePredicate_withBogusBlockId_throwsProgramBlockNotFoundException() {
    ProgramDefinition p = ProgramBuilder.newProgram().buildDefinition();
    assertThatThrownBy(() -> ps.setBlockHidePredicate(p.id(), 100L, Predicate.create("")))
        .isInstanceOf(ProgramBlockNotFoundException.class)
        .hasMessage(
            String.format(
                "Block not found in Program (ID %d) for block definition ID 100", p.id()));
  }

  @Test
  public void setBlockHidePredicate_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram().withBlock().withQuestionDefinition(question).buildDefinition();
    Long programId = programDefinition.id();

    ProgramDefinition found =
        ps.setBlockHidePredicate(programId, 1L, Predicate.create("predicate"));

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void setBlockOptionalPredicate_updatesBlock() throws Exception {
    ProgramDefinition programDefinition = ProgramBuilder.newProgram().buildDefinition();
    Long programId = programDefinition.id();
    Predicate predicate = Predicate.create("hide predicate");
    ps.setBlockOptionalPredicate(programId, 1L, predicate);

    ProgramDefinition found = ps.getProgramDefinition(programId);

    assertThat(found.blockDefinitions().get(0).optionalPredicate()).hasValue(predicate);
  }

  @Test
  public void setBlockOptionalPredicate_withBogusBlockId_throwsProgramBlockNotFoundException() {
    Program program = ProgramBuilder.newProgram().build();
    assertThatThrownBy(() -> ps.setBlockOptionalPredicate(program.id, 100L, Predicate.create("")))
        .isInstanceOf(ProgramBlockNotFoundException.class)
        .hasMessage(
            String.format(
                "Block not found in Program (ID %d) for block definition ID 100", program.id));
  }

  @Test
  public void setBlockOptionalPredicate_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram().withBlock().withQuestionDefinition(question).buildDefinition();
    Long programId = programDefinition.id();

    ProgramDefinition found =
        ps.setBlockOptionalPredicate(programId, 1L, Predicate.create("predicate"));

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void deleteBlock_invalidProgram_throwsProgramNotfoundException() {
    assertThatThrownBy(() -> ps.deleteBlock(1L, 2L))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessage("Program not found for ID: 1");
  }

  @Test
  public void deleteBlock_lastBlock_throwsProgramNeedsABlockException() throws Exception {
    Program program = ProgramBuilder.newProgram().build();

    assertThatThrownBy(() -> ps.deleteBlock(program.id, 1L))
        .isInstanceOf(ProgramNeedsABlockException.class);
  }

  @Test
  public void deleteBlock_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram()
            .withBlock("block one")
            .withQuestionDefinition(question)
            .withBlock()
            .buildDefinition();
    Long programId = programDefinition.id();

    ProgramDefinition result = ps.deleteBlock(programId, 2L);

    assertThat(result.blockDefinitions()).hasSize(1);

    BlockDefinition blockResult = result.blockDefinitions().get(0);
    assertThat(blockResult.name()).isEqualTo("block one");
    assertThat(blockResult.programQuestionDefinitions()).hasSize(1);

    QuestionDefinition questionResult =
        blockResult.programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(questionResult).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void newProgramFromExisting() throws Exception {
    Program program = ProgramBuilder.newProgram().build();
    program.setLifecycleStage(LifecycleStage.ACTIVE);
    program.save();

    ProgramDefinition newDraft = ps.newDraftOf(program.id);
    assertThat(newDraft.lifecycleStage()).isEqualTo(LifecycleStage.DRAFT);
    assertThat(program.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    assertThat(newDraft.adminName()).isEqualTo(program.getProgramDefinition().adminName());
    assertThat(newDraft.blockDefinitions())
        .isEqualTo(program.getProgramDefinition().blockDefinitions());
    assertThat(newDraft.localizedDescription())
        .isEqualTo(program.getProgramDefinition().localizedDescription());
    assertThat(newDraft.id()).isNotEqualTo(program.getProgramDefinition().id());

    ProgramDefinition secondNewDraft = ps.newDraftOf(program.id);
    assertThat(secondNewDraft.id()).isEqualTo(newDraft.id());
    assertThat(secondNewDraft.lifecycleStage()).isEqualTo(LifecycleStage.DRAFT);
    assertThat(program.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
  }
}
