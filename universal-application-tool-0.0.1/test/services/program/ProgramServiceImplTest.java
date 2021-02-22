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

public class ProgramServiceImplTest extends WithPostgresContainer {

  ProgramServiceImpl ps;

  @Before
  public void setProgramServiceImpl() {
    ps = app.injector().instanceOf(ProgramServiceImpl.class);
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
  public void createProgram_setsId() {
    assertThat(ps.listProgramDefinitions()).isEmpty();

    ProgramDefinition programDefinition =
        ps.createProgramDefinition("ProgramService", "description");

    assertThat(programDefinition.id()).isNotNull();
  }

  @Test
  public void updateProgram_withNoProgram_throwsProgramNotFoundException()
      throws ProgramNotFoundException {
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
  public void addBlockToProgram_noProgram_throwsProgramNotFoundException() {
    assertThatThrownBy(() -> ps.addBlockToProgram(1L, "name", "desc"))
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
  public void setBlockQuestions_updatesBlock()
      throws ProgramNotFoundException, ProgramBlockNotFoundException {
    QuestionDefinition questionDefinition =
        new QuestionDefinition(
            1L,
            1L,
            "name question",
            "applicant.name",
            "The name of the applicant.",
            ImmutableMap.of(Locale.US, "What is your name?"),
            Optional.empty());
    ProgramDefinition programDefinition =
        ps.createProgramDefinition("Program With Block", "This program has a block.");
    Long programId = programDefinition.id();
    ps.addBlockToProgram(programId, "the block", "the block for the program");
    ps.setBlockQuestions(programId, 1L, ImmutableList.of(questionDefinition));

    ProgramDefinition found = ps.getProgramDefinition(programId).orElseThrow();

    assertThat(found.blockDefinitions().get(0).questionDefinitions()).hasSize(1);
    assertThat(found.blockDefinitions().get(0).questionDefinitions().get(0).getName())
        .isEqualTo("name question");
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
