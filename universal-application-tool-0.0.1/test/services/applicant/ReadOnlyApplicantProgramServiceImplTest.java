package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;

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

  ProgramDefinition programDefinition = ProgramDefinition.builder()
      .setId(123L)
      .setName("The Program")
      .setDescription("This program is for testing.")
      .addBlockDefinition(blockA)
      .addBlockDefinition(blockB)
      .build();


  ReadOnlyApplicantProgramService subject;

  @Before
  public void setUp() {
    subject = new ReadOnlyApplicantProgramServiceImpl(
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
    subject = new ReadOnlyApplicantProgramServiceImpl(new Applicant().getApplicantData(),
        ProgramDefinition.builder()
            .setId(123L)
            .setName("The Program")
            .setDescription("This program is for testing.")
            .build());

    Optional<Block> maybeBlock = subject.getBlockAfter(321L);

    assertThat(maybeBlock).isEmpty();
  }
}
