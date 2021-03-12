package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.program.ProgramDefinition;
import services.question.QuestionDefinition;
import support.ProgramBuilder;
import support.Questions;

public class ReadOnlyApplicantProgramServiceImplTest extends WithPostgresContainer {

  private QuestionDefinition nameQuestion;
  private QuestionDefinition colorQuestion;
  private QuestionDefinition addressQuestion;
  private ApplicantData applicantData;
  private ReadOnlyApplicantProgramService subject;

  @Before
  public void setUp() {
    applicantData = new ApplicantData();
    nameQuestion = Questions.applicantName().getQuestionDefinition();
    colorQuestion = Questions.applicantFavoriteColor().getQuestionDefinition();
    addressQuestion = Questions.applicantAddress().getQuestionDefinition();
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram()
            .withBlock("Block one")
            .withQuestionDefinition(nameQuestion)
            .withBlock("Block two")
            .withQuestionDefinition(colorQuestion)
            .withQuestionDefinition(addressQuestion)
            .buildDefinition();
    subject = new ReadOnlyApplicantProgramServiceImpl(applicantData, programDefinition);
  }

  @Test
  public void getCurrentBlockList_getsTheApplicantSpecificBlocksForTheProgram() {
    ImmutableList<Block> blockList = subject.getCurrentBlockList();

    assertThat(blockList).hasSize(2);
    Block block = blockList.get(0);
    assertThat(block.getName()).isEqualTo("Block one");
  }

  @Test
  public void getCurrentBlockList_doesNotIncludeCompleteBlocks() {
    // Answer the first block's questions (name)
    applicantData.putString(nameQuestion.getPath(), "my name");

    ImmutableList<Block> blockList = subject.getCurrentBlockList();

    assertThat(blockList).hasSize(1);
    assertThat(blockList.get(0).getName()).isEqualTo("Block two");
  }

  @Test
  public void getCurrentBlockList_returnsEmptyListIfAllBlocksComplete() {
    // Answer all questions
    applicantData.putString(nameQuestion.getPath(), "my name");
    applicantData.putString(colorQuestion.getPath(), "mauve");
    applicantData.putObject(addressQuestion.getPath(), ImmutableMap.of("scalar", "value"));

    ImmutableList<Block> blockList = subject.getCurrentBlockList();

    assertThat(blockList).isEmpty();
  }

  @Test
  public void getBlock_blockExists_returnsTheBlock() {
    Optional<Block> maybeBlock = subject.getBlock(1L);

    assertThat(maybeBlock).isPresent();
    assertThat(maybeBlock.get().getId()).isEqualTo(1L);
  }

  @Test
  public void getBlock_blockNotInList_returnsEmpty() {
    Optional<Block> maybeBlock = subject.getBlock(111L);

    assertThat(maybeBlock).isEmpty();
  }

  @Test
  public void getBlockAfter_thereExistsABlockAfter_returnsTheBlockAfterTheGivenBlock() {
    Optional<Block> maybeBlock = subject.getBlockAfter(1L);

    assertThat(maybeBlock).isPresent();
    assertThat(maybeBlock.get().getId()).isEqualTo(2L);
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
            new Applicant().getApplicantData(), ProgramBuilder.newProgram().buildDefinition());

    Optional<Block> maybeBlock = subject.getFirstIncompleteBlock();

    assertThat(maybeBlock).isEmpty();
  }

  @Test
  public void getFirstIncompleteBlock_firstIncompleteBlockReturned() {
    Optional<Block> maybeBlock = subject.getFirstIncompleteBlock();

    assertThat(maybeBlock).isNotEmpty();
    assertThat(maybeBlock.get().getName()).isEqualTo("Block one");
  }
}
