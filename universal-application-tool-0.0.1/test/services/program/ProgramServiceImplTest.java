package services.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.junit.Before;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.question.QuestionDefinition;
import services.question.QuestionService;
import services.question.TextQuestionDefinition;

public class ProgramServiceImplTest extends WithPostgresContainer {

  private ProgramServiceImpl ps;
  private QuestionService qs;
  private static final QuestionDefinition SIMPLE_QUESTION =
      new TextQuestionDefinition(
          1L,
          2L,
          "Name Question",
          "applicant.name",
          "The name of the applicant.",
          ImmutableMap.of(Locale.US, "What is your name?"),
          Optional.empty());

  @Before
  public void setProgramServiceImpl() {
    ps = instanceOf(ProgramServiceImpl.class);
    qs = instanceOf(QuestionService.class);
  }

  @Test
  public void listProgramDefinitions_hasNoResults() {
    ImmutableList<ProgramDefinition> programDefinitions = ps.listProgramDefinitions();

    assertThat(programDefinitions).isEmpty();
  }

  @Test
  public void listProgramDefinitions_hasResults() {
    ProgramDefinition first = ps.createProgramDefinition("first name", "first description");
    ProgramDefinition second = ps.createProgramDefinition("second name", "second description");

    ImmutableList<ProgramDefinition> programDefinitions = ps.listProgramDefinitions();

    assertThat(programDefinitions).containsExactly(first, second);
  }

  @Test
  public void listProgramDefinitions_createsQuestionDefinitions() throws ProgramNotFoundException {
    qs.create(SIMPLE_QUESTION);
    ProgramDefinition program = ps.createProgramDefinition("Program Name", "Program Description");
    ps.addBlockToProgram(
        program.id(),
        "Block",
        "Block Description",
        ImmutableList.of(ProgramQuestionDefinition.create(SIMPLE_QUESTION)));

    ImmutableList<ProgramDefinition> programDefinitions = ps.listProgramDefinitions();

    QuestionDefinition foundQuestion =
        programDefinitions
            .get(0)
            .blockDefinitions()
            .get(0)
            .programQuestionDefinitions()
            .get(0)
            .getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(TextQuestionDefinition.class);
  }

  @Test
  public void listProgramDefinitionsAsync_hasNoResults() {
    CompletionStage<ImmutableList<ProgramDefinition>> completionStage =
        ps.listProgramDefinitionsAsync();

    assertThat(completionStage.toCompletableFuture().join()).isEmpty();
  }

  @Test
  public void listProgramDefinitionsAsync_hasResults() {
    ProgramDefinition first = ps.createProgramDefinition("first name", "first description");
    ProgramDefinition second = ps.createProgramDefinition("second name", "second description");

    CompletionStage<ImmutableList<ProgramDefinition>> completionStage =
        ps.listProgramDefinitionsAsync();

    assertThat(completionStage.toCompletableFuture().join()).containsExactly(first, second);
  }

  @Test
  public void listProgramDefinitionsAsync_createsQuestionDefinitions()
      throws ProgramNotFoundException {
    qs.create(SIMPLE_QUESTION);
    ProgramDefinition program = ps.createProgramDefinition("Program Name", "Program Description");
    ps.addBlockToProgram(
        program.id(),
        "Block",
        "Block Description",
        ImmutableList.of(ProgramQuestionDefinition.create(SIMPLE_QUESTION)));

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
    assertThat(foundQuestion).isInstanceOf(TextQuestionDefinition.class);
  }

  @Test
  public void createProgram_setsId() {
    assertThat(ps.listProgramDefinitions()).isEmpty();

    ProgramDefinition programDefinition =
        ps.createProgramDefinition("ProgramService", "description");

    assertThat(programDefinition.id()).isNotNull();
  }

  @Test
  public void updateProgram_withNoProgram_throwsProgramNotFoundException() {
    assertThatThrownBy(() -> ps.updateProgramDefinition(1L, "new", "new description"))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessage("Program not found for ID: 1");
  }

  @Test
  public void updateProgram_updatesProgram() throws ProgramNotFoundException {
    ProgramDefinition originalProgram =
        ps.createProgramDefinition("original", "original description");
    ProgramDefinition updatedProgram =
        ps.updateProgramDefinition(originalProgram.id(), "new", "new description");

    Optional<ProgramDefinition> found = ps.getProgramDefinition(updatedProgram.id());

    assertThat(ps.listProgramDefinitions()).hasSize(1);
    assertThat(found).hasValue(updatedProgram);
  }

  @Test
  public void updateProgram_createsQuestionDefinitions() throws ProgramNotFoundException {
    qs.create(SIMPLE_QUESTION);
    ProgramDefinition program = ps.createProgramDefinition("Program Name", "Program Description");
    ps.addBlockToProgram(
        program.id(),
        "Block",
        "Block Description",
        ImmutableList.of(ProgramQuestionDefinition.create(SIMPLE_QUESTION)));

    ProgramDefinition found =
        ps.updateProgramDefinition(program.id(), "new name", "new description");

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(TextQuestionDefinition.class);
  }

  @Test
  public void getProgramDefinition_canGetANewProgram() {
    ProgramDefinition programDefinition = ps.createProgramDefinition("new program", "description");
    Optional<ProgramDefinition> found = ps.getProgramDefinition(programDefinition.id());

    assertThat(found).hasValue(programDefinition);
  }

  @Test
  public void getProgramDefinition_returnsEmptyOptionalWhenProgramNotFound() {
    ProgramDefinition programDefinition = ps.createProgramDefinition("new program", "description");
    Optional<ProgramDefinition> found = ps.getProgramDefinition(programDefinition.id() + 1);

    assertThat(found).isEmpty();
  }

  @Test
  public void getProgramDefinition_createsQuestionDefinitions() throws ProgramNotFoundException {
    qs.create(SIMPLE_QUESTION);
    ProgramDefinition program = ps.createProgramDefinition("Program Name", "Program Description");
    ps.addBlockToProgram(
        program.id(),
        "Block",
        "Block Description",
        ImmutableList.of(ProgramQuestionDefinition.create(SIMPLE_QUESTION)));

    ProgramDefinition found = ps.getProgramDefinition(program.id()).get();

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(TextQuestionDefinition.class);
  }

  @Test
  public void getProgramDefinitionAsync_getsRequestedProgram() {
    ProgramDefinition programDefinition = ps.createProgramDefinition("async", "program");

    CompletionStage<Optional<ProgramDefinition>> found =
        ps.getProgramDefinitionAsync(programDefinition.id());

    assertThat(found.toCompletableFuture().join()).hasValue(programDefinition);
  }

  @Test
  public void getProgramDefinitionAsync_cannotFindRequestedProgram_returnsEmptyOptional() {
    ProgramDefinition programDefinition = ps.createProgramDefinition("different", "program");

    CompletionStage<Optional<ProgramDefinition>> found =
        ps.getProgramDefinitionAsync(programDefinition.id() + 1);

    assertThat(found.toCompletableFuture().join()).isEmpty();
  }

  @Test
  public void getProgramDefinitionAsync_createsQuestionDefinitions()
      throws ProgramNotFoundException {
    qs.create(SIMPLE_QUESTION);
    ProgramDefinition program = ps.createProgramDefinition("Program Name", "Program Description");
    ps.addBlockToProgram(
        program.id(),
        "Block",
        "Block Description",
        ImmutableList.of(ProgramQuestionDefinition.create(SIMPLE_QUESTION)));

    ProgramDefinition found =
        ps.getProgramDefinitionAsync(program.id()).toCompletableFuture().join().get();

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(TextQuestionDefinition.class);
  }

  @Test
  public void addBlockToProgram_noProgram_throwsProgramNotFoundException() {
    assertThatThrownBy(() -> ps.addBlockToProgram(1L, "name", "desc"))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessage("Program not found for ID: 1");

    assertThatThrownBy(() -> ps.addBlockToProgram(1L, "name", "description", ImmutableList.of()))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessage("Program not found for ID: 1");
  }

  @Test
  public void addBlockToProgram_returnsProgramDefinitionWithBlock()
      throws ProgramNotFoundException {
    ProgramDefinition programDefinition =
        ps.createProgramDefinition("Program With Block", "This program has a block.");
    Long programId = programDefinition.id();
    ProgramDefinition updatedProgramDefinition =
        ps.addBlockToProgram(programDefinition.id(), "the block", "the block for the program");

    ProgramDefinition found = ps.getProgramDefinition(programId).orElseThrow();

    assertThat(found.blockDefinitions()).hasSize(1);
    assertThat(found.blockDefinitions())
        .containsExactlyElementsOf(updatedProgramDefinition.blockDefinitions());
  }

  @Test
  public void addBlockToProgramWithQuestions_returnsProgramDefinitionWithBlock() throws Exception {
    qs.create(SIMPLE_QUESTION);
    ProgramDefinition programDefinition = ps.createProgramDefinition("program", "description");
    long id = programDefinition.id();
    ProgramQuestionDefinition programQuestionDefinition =
        ProgramQuestionDefinition.create(SIMPLE_QUESTION);
    ProgramDefinition updated =
        ps.addBlockToProgram(id, "block", "desc", ImmutableList.of(programQuestionDefinition));

    assertThat(updated.blockDefinitions()).hasSize(1);
    BlockDefinition foundBlock = updated.blockDefinitions().get(0);

    assertThat(foundBlock.id()).isEqualTo(1L);
    assertThat(foundBlock.name()).isEqualTo("block");
    assertThat(foundBlock.description()).isEqualTo("desc");

    assertThat(foundBlock.programQuestionDefinitions()).hasSize(1);
    ProgramQuestionDefinition foundPqd = foundBlock.programQuestionDefinitions().get(0);

    assertThat(foundPqd.id()).isEqualTo(programQuestionDefinition.id());
    assertThat(foundPqd.getQuestionDefinition()).isInstanceOf(TextQuestionDefinition.class);
    assertThat(foundPqd.getQuestionDefinition().getName()).isEqualTo("Name Question");
  }

  @Test
  public void addBlockToProgram_createsQuestionDefinitions() throws ProgramNotFoundException {
    qs.create(SIMPLE_QUESTION);
    ProgramDefinition program = ps.createProgramDefinition("Program Name", "Program Description");

    program =
        ps.addBlockToProgram(
            program.id(),
            "Block",
            "Block Description",
            ImmutableList.of(ProgramQuestionDefinition.create(SIMPLE_QUESTION)));

    QuestionDefinition foundQuestion =
        program
            .blockDefinitions()
            .get(0)
            .programQuestionDefinitions()
            .get(0)
            .getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(TextQuestionDefinition.class);

    program = ps.addBlockToProgram(program.id(), "empty block", "this block has no questions");

    foundQuestion =
        program
            .blockDefinitions()
            .get(0)
            .programQuestionDefinitions()
            .get(0)
            .getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(TextQuestionDefinition.class);
  }

  @Test
  public void setBlockQuestions_updatesBlock()
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    qs.create(SIMPLE_QUESTION);

    ProgramDefinition programDefinition =
        ps.createProgramDefinition("Program With Block", "This program has a block.");
    Long programId = programDefinition.id();
    ps.addBlockToProgram(programId, "the block", "the block for the program");
    ps.setBlockQuestions(
        programId, 1L, ImmutableList.of(ProgramQuestionDefinition.create(SIMPLE_QUESTION)));

    ProgramDefinition found = ps.getProgramDefinition(programId).orElseThrow();
    assertThat(found.blockDefinitions()).hasSize(1);

    BlockDefinition foundBlock = found.blockDefinitions().get(0);
    assertThat(foundBlock.programQuestionDefinitions()).hasSize(1);

    ProgramQuestionDefinition foundPqd =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0);
    assertThat(foundPqd.id()).isEqualTo(SIMPLE_QUESTION.getId());
    assertThat(foundPqd.getQuestionDefinition()).isInstanceOf(TextQuestionDefinition.class);
    assertThat(foundPqd.getQuestionDefinition().getName()).isEqualTo("Name Question");
  }

  @Test
  public void setBlockQuestions_createsQuestionDefinitions()
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    qs.create(SIMPLE_QUESTION);
    ProgramDefinition programDefinition =
        ps.createProgramDefinition("Program With Block", "This program has a block.");
    Long programId = programDefinition.id();
    ps.addBlockToProgram(programId, "the block", "the block for the program");

    ProgramDefinition found =
        ps.setBlockQuestions(
            programId, 1L, ImmutableList.of(ProgramQuestionDefinition.create(SIMPLE_QUESTION)));
    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(TextQuestionDefinition.class);
  }

  @Test
  public void setBlockHidePredicate_updatesBlock()
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programDefinition =
        ps.createProgramDefinition("Program With Block", "This program has a block.");
    Long programId = programDefinition.id();
    ps.addBlockToProgram(programId, "the block", "the block for the program");
    Predicate predicate = Predicate.create("hide predicate");
    ps.setBlockHidePredicate(programId, 1L, predicate);

    ProgramDefinition found = ps.getProgramDefinition(programId).orElseThrow();

    assertThat(found.blockDefinitions().get(0).hidePredicate()).hasValue(predicate);
  }

  @Test
  public void setBlockOptionalPredicate_updatesBlock()
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    ProgramDefinition programDefinition =
        ps.createProgramDefinition("Program With Block", "This program has a block.");
    Long programId = programDefinition.id();
    ps.addBlockToProgram(programId, "the block", "the block for the program");
    Predicate predicate = Predicate.create("hide predicate");
    ps.setBlockOptionalPredicate(programId, 1L, predicate);

    ProgramDefinition found = ps.getProgramDefinition(programId).orElseThrow();

    assertThat(found.blockDefinitions().get(0).optionalPredicate()).hasValue(predicate);
  }

  @Test
  public void setBlockQuestions_withBogusBlockId_throwsProgramBlockNotFoundException() {
    ProgramDefinition p = ps.createProgramDefinition("name", "description");
    assertThatThrownBy(() -> ps.setBlockQuestions(p.id(), 1L, ImmutableList.of()))
        .isInstanceOf(ProgramBlockNotFoundException.class)
        .hasMessage(String.format("Block not found in Program (ID %d) for block ID 1", p.id()));
  }

  @Test
  public void setBlockHidePredicate_withBogusBlockId_throwsProgramBlockNotFoundException() {
    ProgramDefinition p = ps.createProgramDefinition("name", "description");
    assertThatThrownBy(() -> ps.setBlockHidePredicate(p.id(), 1L, Predicate.create("")))
        .isInstanceOf(ProgramBlockNotFoundException.class)
        .hasMessage(String.format("Block not found in Program (ID %d) for block ID 1", p.id()));
  }

  @Test
  public void setBlockOptionalPredicate_withBogusBlockId_throwsProgramBlockNotFoundException() {
    ProgramDefinition p = ps.createProgramDefinition("name", "description");
    assertThatThrownBy(() -> ps.setBlockOptionalPredicate(p.id(), 1L, Predicate.create("")))
        .isInstanceOf(ProgramBlockNotFoundException.class)
        .hasMessage(String.format("Block not found in Program (ID %d) for block ID 1", p.id()));
  }
}
