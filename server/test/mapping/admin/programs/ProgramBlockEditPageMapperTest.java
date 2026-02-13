package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import views.admin.programs.ProgramBlockEditPageViewModel;

public final class ProgramBlockEditPageMapperTest {

  private ProgramBlockEditPageMapper mapper;

  @Before
  public void setup() {
    mapper = new ProgramBlockEditPageMapper();
  }

  private ProgramDefinition buildProgram(BlockDefinition block) {
    return ProgramDefinition.builder()
        .setId(1L)
        .setAdminName("test-program")
        .setAdminDescription("admin desc")
        .setLocalizedName(LocalizedStrings.of(Locale.US, "Test Program"))
        .setLocalizedDescription(LocalizedStrings.of(Locale.US, "Description"))
        .setLocalizedShortDescription(LocalizedStrings.of(Locale.US, "Short"))
        .setExternalLink("")
        .setDisplayMode(DisplayMode.PUBLIC)
        .setProgramType(ProgramType.DEFAULT)
        .setLocalizedConfirmationMessage(LocalizedStrings.empty())
        .setAcls(new ProgramAcls())
        .setBlockDefinitions(ImmutableList.of(block))
        .build();
  }

  @Test
  public void map_setsProgramName() {
    BlockDefinition block =
        BlockDefinition.builder().setId(1L).setName("Block 1").setDescription("Desc").build();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockEditPageViewModel result =
        mapper.map(
            program,
            block,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            false,
            false,
            Optional.empty(),
            Optional.empty(),
            ImmutableList.of(),
            false);

    assertThat(result.getProgramName()).isEqualTo("Test Program");
  }

  @Test
  public void map_setsBlockDetails() {
    BlockDefinition block =
        BlockDefinition.builder()
            .setId(1L)
            .setName("Block 1")
            .setDescription("Block description")
            .build();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockEditPageViewModel result =
        mapper.map(
            program,
            block,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            false,
            false,
            Optional.empty(),
            Optional.empty(),
            ImmutableList.of(),
            false);

    assertThat(result.getBlockName()).isEqualTo("Block 1");
    assertThat(result.getBlockDescription()).isEqualTo("Block description");
    assertThat(result.getSelectedBlockId()).isEqualTo(1L);
    assertThat(result.getProgramId()).isEqualTo(1L);
  }

  @Test
  public void map_setsBlockList() {
    BlockDefinition block =
        BlockDefinition.builder().setId(1L).setName("Block 1").setDescription("Desc").build();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockEditPageViewModel result =
        mapper.map(
            program,
            block,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            false,
            false,
            Optional.empty(),
            Optional.empty(),
            ImmutableList.of(),
            false);

    assertThat(result.getBlockList()).hasSize(1);
    assertThat(result.getBlockList().get(0).getName()).isEqualTo("Block 1");
    assertThat(result.getBlockList().get(0).isSelected()).isTrue();
  }

  @Test
  public void map_singleBlock_showDeleteButtonFalse() {
    BlockDefinition block =
        BlockDefinition.builder().setId(1L).setName("Block 1").setDescription("Desc").build();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockEditPageViewModel result =
        mapper.map(
            program,
            block,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            false,
            false,
            Optional.empty(),
            Optional.empty(),
            ImmutableList.of(),
            false);

    assertThat(result.isShowDeleteButton()).isFalse();
  }

  @Test
  public void map_multipleBlocks_showDeleteButtonTrue() {
    BlockDefinition block1 =
        BlockDefinition.builder().setId(1L).setName("Block 1").setDescription("Desc").build();
    BlockDefinition block2 =
        BlockDefinition.builder().setId(2L).setName("Block 2").setDescription("Desc 2").build();
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(1L)
            .setAdminName("test-program")
            .setAdminDescription("admin desc")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "Test Program"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "Description"))
            .setLocalizedShortDescription(LocalizedStrings.of(Locale.US, "Short"))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setLocalizedConfirmationMessage(LocalizedStrings.empty())
            .setAcls(new ProgramAcls())
            .setBlockDefinitions(ImmutableList.of(block1, block2))
            .build();

    ProgramBlockEditPageViewModel result =
        mapper.map(
            program,
            block1,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            false,
            false,
            Optional.empty(),
            Optional.empty(),
            ImmutableList.of(),
            false);

    assertThat(result.isShowDeleteButton()).isTrue();
  }

  @Test
  public void map_nonExternalProgram_setsPreviewUrl() {
    BlockDefinition block =
        BlockDefinition.builder().setId(1L).setName("Block 1").setDescription("Desc").build();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockEditPageViewModel result =
        mapper.map(
            program,
            block,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            false,
            false,
            Optional.empty(),
            Optional.empty(),
            ImmutableList.of(),
            false);

    assertThat(result.isExternal()).isFalse();
    assertThat(result.getPreviewUrl()).isPresent();
    assertThat(result.getDownloadPdfUrl()).isPresent();
  }

  @Test
  public void map_externalProgram_noPreviewUrl() {
    BlockDefinition block =
        BlockDefinition.builder().setId(1L).setName("Block 1").setDescription("Desc").build();
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(1L)
            .setAdminName("ext-prog")
            .setAdminDescription("desc")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "External"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "Desc"))
            .setLocalizedShortDescription(LocalizedStrings.of(Locale.US, "Short"))
            .setExternalLink("http://example.com")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.EXTERNAL)
            .setLocalizedConfirmationMessage(LocalizedStrings.empty())
            .setAcls(new ProgramAcls())
            .setBlockDefinitions(ImmutableList.of(block))
            .build();

    ProgramBlockEditPageViewModel result =
        mapper.map(
            program,
            block,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            false,
            false,
            Optional.empty(),
            Optional.empty(),
            ImmutableList.of(),
            false);

    assertThat(result.isExternal()).isTrue();
    assertThat(result.getPreviewUrl()).isEmpty();
    assertThat(result.getDownloadPdfUrl()).isEmpty();
  }

  @Test
  public void map_setsQuestionBankVisibility() {
    BlockDefinition block =
        BlockDefinition.builder().setId(1L).setName("Block 1").setDescription("Desc").build();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockEditPageViewModel result =
        mapper.map(
            program,
            block,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            false,
            true,
            Optional.empty(),
            Optional.empty(),
            ImmutableList.of(),
            false);

    assertThat(result.isShowQuestionBank()).isTrue();
  }

  @Test
  public void map_setsSuccessAndErrorMessages() {
    BlockDefinition block =
        BlockDefinition.builder().setId(1L).setName("Block 1").setDescription("Desc").build();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockEditPageViewModel result =
        mapper.map(
            program,
            block,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            false,
            false,
            Optional.of("Saved!"),
            Optional.of("Failed!"),
            ImmutableList.of(),
            false);

    assertThat(result.getSuccessMessage()).contains("Saved!");
    assertThat(result.getErrorMessage()).contains("Failed!");
  }

  @Test
  public void map_noCategoriesProgram_setsCategoriesNone() {
    BlockDefinition block =
        BlockDefinition.builder().setId(1L).setName("Block 1").setDescription("Desc").build();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockEditPageViewModel result =
        mapper.map(
            program,
            block,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
            false,
            false,
            Optional.empty(),
            Optional.empty(),
            ImmutableList.of(),
            false);

    assertThat(result.getCategoriesText()).isEqualTo("None");
  }
}
