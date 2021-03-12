package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
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

  private static final QuestionDefinition NAME_QUESTION =
      Questions.applicantName().getQuestionDefinition();
  private static final QuestionDefinition COLOR_QUESTION =
      Questions.applicantFavoriteColor().getQuestionDefinition();
  private static final QuestionDefinition ADDRESS_QUESTION =
      Questions.applicantAddress().getQuestionDefinition();

  private ProgramDefinition programDefinition =
      ProgramBuilder.newProgram()
          .withBlock("Block one")
          .withQuestionDefinition(NAME_QUESTION)
          .withBlock("Block two")
          .withQuestionDefinition(COLOR_QUESTION)
          .withQuestionDefinition(ADDRESS_QUESTION)
          .buildDefinition();

  private ApplicantData applicantData;
  private ReadOnlyApplicantProgramService subject;

  @Before
  public void setUp() {
    // Reset ApplicantData
    applicantData = new ApplicantData();
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
    // Answer the first question (name)
    applicantData.putString(NAME_QUESTION.getPath(), "my name");

    ImmutableList<Block> blockList = subject.getCurrentBlockList();

    assertThat(blockList).hasSize(1);
    assertThat(blockList.get(0).getName()).isEqualTo("Block two");
  }

  @Test
  public void getCurrentBlockList_returnsEmptyListIfAllBlocksComplete() {
    // Answer all questions
    applicantData.putString(NAME_QUESTION.getPath(), "my name");
    applicantData.putString(COLOR_QUESTION.getPath(), "mauve");

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
