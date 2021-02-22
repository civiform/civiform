package services.program.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import services.program.*;
import services.question.QuestionDefinition;

public class FakeProgramServiceTest {

  private FakeProgramService service;

  @Before
  public void resetFake() {
    service = new FakeProgramService();
  }

  @Test
  public void listProgramDefinitions_noPrograms() {
    assertThat(service.listProgramDefinitions()).isEmpty();
  }

  @Test
  public void listProgramDefinitionsAsync_noPrograms() {
    assertThat(service.listProgramDefinitionsAsync()).isCompletedWithValue(ImmutableList.of());
  }

  @Test
  public void createAProgram_returnsProgramInList() {
    ProgramDefinition created = service.createProgramDefinition("hello", "world");

    assertThat(created.id()).isEqualTo(1L);
    assertThat(created.name()).isEqualTo("hello");
    assertThat(created.description()).isEqualTo("world");
    assertThat(service.listProgramDefinitions()).containsExactly(created);
  }

  @Test
  public void createMultiplePrograms_incrementsIds() {
    ProgramDefinition one = service.createProgramDefinition("one", "program");
    ProgramDefinition two = service.createProgramDefinition("two", "programs");

    assertThat(one.id()).isEqualTo(1L);
    assertThat(two.id()).isEqualTo(2L);
  }

  @Test
  public void updateProgram_withNoProgram_throwsProgramNotFoundException() {
    assertThatThrownBy(() -> service.updateProgramDefinition(1L, "new", "new description"))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessage("Program not found for ID: 1");
  }

  @Test
  public void updateProgram_updatesProgram() throws ProgramNotFoundException {
    ProgramDefinition originalProgram =
        service.createProgramDefinition("original", "original description");
    ProgramDefinition updatedProgram =
        service.updateProgramDefinition(originalProgram.id(), "new", "new description");

    Optional<ProgramDefinition> found = service.getProgramDefinition(updatedProgram.id());

    assertThat(service.listProgramDefinitions()).hasSize(1);
    assertThat(found).hasValue(updatedProgram);
  }

  @Test
  public void listMultiplePrograms() {
    ProgramDefinition one = service.createProgramDefinition("one", "program");
    ProgramDefinition two = service.createProgramDefinition("two", "programs");

    ImmutableList<ProgramDefinition> programs = service.listProgramDefinitions();

    assertThat(programs).containsExactly(one, two);
    assertThat(service.listProgramDefinitionsAsync()).isCompletedWithValue(programs);
  }

  @Test
  public void addBlockToProgram() throws Exception {
    ProgramDefinition program = service.createProgramDefinition("name", "desc");

    ProgramDefinition updatedProgram =
        service.addBlockToProgram(program.id(), "Block Name", "Block Description");

    assertThat(updatedProgram.blockDefinitions())
        .containsExactly(
            BlockDefinition.builder()
                .setId(1L)
                .setName("Block Name")
                .setDescription("Block Description")
                .build());
  }

  @Test
  public void addMultipleBlocksToProgram_blocksHaveDifferentIds() throws Exception {
    ProgramDefinition program = service.createProgramDefinition("name", "desc");

    service.addBlockToProgram(program.id(), "one", "block");
    ProgramDefinition updatedProgram = service.addBlockToProgram(program.id(), "two", "blocks");

    assertThat(updatedProgram.blockDefinitions())
        .containsExactly(
            BlockDefinition.builder().setId(1L).setName("one").setDescription("block").build(),
            BlockDefinition.builder().setId(2L).setName("two").setDescription("blocks").build());
  }

  @Test
  public void addBlockToProgram_throwsForUnknownProgram() {
    assertThatExceptionOfType(ProgramNotFoundException.class)
        .isThrownBy(() -> service.addBlockToProgram(123L, "Block Name", "Block Description"))
        .withMessageContaining("Program not found for ID: 123");
  }

  @Test
  public void setBlockQuestions() throws Exception {
    ProgramDefinition program = service.createProgramDefinition("name", "desc");
    program = service.addBlockToProgram(program.id(), "block", "withNoQuestions");
    long blockId = program.blockDefinitions().get(0).id();

    QuestionDefinition question =
        new QuestionDefinition(1L, 1L, "", "", "", ImmutableMap.of(), Optional.empty());
    program = service.setBlockQuestions(program.id(), blockId, ImmutableList.of(question));

    assertThat(program.blockDefinitions().get(0).questionDefinitions()).containsExactly(question);
  }

  @Test
  public void setBlockQuestions_throwsExceptionWhenBlockNotFound() throws Exception {
    ProgramDefinition program = service.createProgramDefinition("name", "desc");

    assertThatExceptionOfType(ProgramBlockNotFoundException.class)
        .isThrownBy(() -> service.setBlockQuestions(program.id(), 123L, ImmutableList.of()))
        .withMessageContaining("Block not found in Program (ID 1) for block ID 123");
  }

  @Test
  public void setBlockHidePredicate() throws Exception {
    ProgramDefinition program = service.createProgramDefinition("name", "desc");
    program = service.addBlockToProgram(program.id(), "block", "withNoQuestions");
    long blockId = program.blockDefinitions().get(0).id();

    Predicate predicate = Predicate.create("hide me");
    program = service.setBlockHidePredicate(program.id(), blockId, predicate);

    assertThat(program.blockDefinitions().get(0).hidePredicate()).hasValue(predicate);
  }

  @Test
  public void setBlockHidePredicate_throwsExceptionWhenBlockNotFound() throws Exception {
    ProgramDefinition program = service.createProgramDefinition("name", "desc");

    assertThatExceptionOfType(ProgramBlockNotFoundException.class)
        .isThrownBy(() -> service.setBlockHidePredicate(program.id(), 123L, Predicate.create("")))
        .withMessageContaining("Block not found in Program (ID 1) for block ID 123");
  }

  @Test
  public void setBlockOptionalPredicate() throws Exception {
    ProgramDefinition program = service.createProgramDefinition("name", "desc");
    program = service.addBlockToProgram(program.id(), "block", "withNoQuestions");
    long blockId = program.blockDefinitions().get(0).id();

    Predicate predicate = Predicate.create("I'm optional");
    program = service.setBlockOptionalPredicate(program.id(), blockId, predicate);

    assertThat(program.blockDefinitions().get(0).optionalPredicate()).hasValue(predicate);
  }

  @Test
  public void setBlockOptionalPredicate_throwsExceptionWhenBlockNotFound() throws Exception {
    ProgramDefinition program = service.createProgramDefinition("name", "desc");

    assertThatExceptionOfType(ProgramBlockNotFoundException.class)
        .isThrownBy(
            () -> service.setBlockOptionalPredicate(program.id(), 123L, Predicate.create("")))
        .withMessageContaining("Block not found in Program (ID 1) for block ID 123");
  }
}
