package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.OptionalLong;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.Path;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.TextQuestionDefinition;

public class ReadOnlyApplicantProgramServiceImplTest extends WithPostgresContainer {

  BlockDefinition blockA =
      BlockDefinition.builder()
          .setId(123L)
          .setName("Block Name")
          .setDescription("Block Description")
          .build();

  BlockDefinition blockB =
      BlockDefinition.builder()
          .setId(321L)
          .setName("Block Name B")
          .setDescription("Block Description B")
          .build();

  ProgramDefinition programDefinition =
      ProgramDefinition.builder()
          .setId(123L)
          .setName("The Program")
          .setDescription("This program is for testing.")
          .addBlockDefinition(blockA)
          .addBlockDefinition(blockB)
          .build();

  ReadOnlyApplicantProgramService subject;

  @Before
  public void setUp() {
    subject =
        new ReadOnlyApplicantProgramServiceImpl(
            new Applicant().getApplicantData(), programDefinition);
  }

  @Test
  public void getCurrentBlockList_getsTheApplicantSpecificBlocksForTheProgram() {
    ImmutableList<Block> blockList = subject.getCurrentBlockList();

    assertThat(blockList.size()).isEqualTo(2);
    Block block = blockList.get(0);
    assertThat(block.getId()).isEqualTo(123L);
    assertThat(block.getName()).isEqualTo("Block Name");
    assertThat(block.getDescription()).isEqualTo("Block Description");
  }

  @Test
  public void getBlock_blockExists_returnsTheBlock() {
    Optional<Block> maybeBlock = subject.getBlock(123L);

    assertThat(maybeBlock).isPresent();
    assertThat(maybeBlock.get().getId()).isEqualTo(123L);
  }

  @Test
  public void getBlock_blockNotInList_returnsEmpty() {
    Optional<Block> maybeBlock = subject.getBlock(111L);

    assertThat(maybeBlock).isEmpty();
  }

  @Test
  public void getBlockAfter_thereExistsABlockAfter_returnsTheBlockAfterTheGivenBlock() {
    Optional<Block> maybeBlock = subject.getBlockAfter(123L);

    assertThat(maybeBlock).isPresent();
    assertThat(maybeBlock.get().getId()).isEqualTo(321L);
  }

  @Test
  public void getBlockAfter_argIsLastBlock_returnsEmpty() {
    Optional<Block> maybeBlock = subject.getBlockAfter(321L);

    assertThat(maybeBlock).isEmpty();
  }

  @Test
  public void getBlockAfter_emptyBlockList_returnsEmpty() {
    subject =
        new ReadOnlyApplicantProgramServiceImpl(
            new Applicant().getApplicantData(),
            ProgramDefinition.builder()
                .setId(123L)
                .setName("The Program")
                .setDescription("This program is for testing.")
                .build());

    Optional<Block> maybeBlock = subject.getBlockAfter(321L);

    assertThat(maybeBlock).isEmpty();
  }

  @Test
  public void getFirstIncompleteBlock_emptyBlockList_returnsEmpty() {
    subject =
        new ReadOnlyApplicantProgramServiceImpl(
            new Applicant().getApplicantData(),
            ProgramDefinition.builder()
                .setId(123L)
                .setName("The Program")
                .setDescription("This program is for testing.")
                .build());

    Optional<Block> maybeBlock = subject.getFirstIncompleteBlock();

    assertThat(maybeBlock).isEmpty();
  }

  @Test
  public void getFirstIncompleteBlock_firstBlockReturnedIfAllIncomplete() {
    BlockDefinition blockOne = blockWithQuestionPaths(Path.create("unanswered"));
    BlockDefinition blockTwo = blockWithQuestionPaths(Path.create("new"));
    subject =
        new ReadOnlyApplicantProgramServiceImpl(
            new Applicant().getApplicantData(),
            ProgramDefinition.builder()
                .setId(123L)
                .setName("The Program")
                .setDescription("This program is for testing.")
                .addBlockDefinition(blockOne)
                .addBlockDefinition(blockTwo)
                .build());

    Optional<Block> maybeBlock = subject.getFirstIncompleteBlock();

    assertThat(maybeBlock).hasValue(new Block(456L, blockOne, new ApplicantData()));
  }

  @Test
  public void getFirstIncompleteBlock_firstIncompleteBlockReturned() {
    Path blockOnePath = Path.create("applicant.complete");
    BlockDefinition blockOne = blockWithQuestionPaths(blockOnePath);
    BlockDefinition blockTwo = blockWithQuestionPaths(Path.create("applicant.unanswered"));
    // Answer the first block.
    ApplicantData applicantData = new ApplicantData();
    applicantData.putString(blockOnePath, "finished");

    subject =
        new ReadOnlyApplicantProgramServiceImpl(
            applicantData,
            ProgramDefinition.builder()
                .setId(123L)
                .setName("The Program")
                .setDescription("This program is for testing.")
                .addBlockDefinition(blockOne)
                .addBlockDefinition(blockTwo)
                .build());

    Optional<Block> maybeBlock = subject.getFirstIncompleteBlock();

    assertThat(maybeBlock).hasValue(new Block(456L, blockTwo, applicantData));
  }

  @Test
  public void getFirstIncompleteBlock_returnsEmptyForNoIncompleteBlocks() {
    Path blockOnePath = Path.create("applicant.complete");
    BlockDefinition blockOne = blockWithQuestionPaths(blockOnePath);
    // Answer the first block.
    ApplicantData applicantData = new ApplicantData();
    applicantData.putString(blockOnePath, "finished");

    subject =
        new ReadOnlyApplicantProgramServiceImpl(
            applicantData,
            ProgramDefinition.builder()
                .setId(123L)
                .setName("The Program")
                .setDescription("This program is for testing.")
                .addBlockDefinition(blockOne)
                .build());

    Optional<Block> maybeBlock = subject.getFirstIncompleteBlock();

    assertThat(maybeBlock).isEmpty();
  }

  private BlockDefinition blockWithQuestionPaths(Path... paths) {
    BlockDefinition.Builder builder =
        BlockDefinition.builder().setId(456L).setName("").setDescription("");
    long qid = 1L;
    for (Path path : paths) {
      builder.addQuestion(
          ProgramQuestionDefinition.create(
              new TextQuestionDefinition(
                  OptionalLong.of(qid++), 1L, "", path, "", ImmutableMap.of(), ImmutableMap.of())));
    }
    return builder.build();
  }
}
