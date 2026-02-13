package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import java.util.Locale;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.question.types.QuestionDefinition;
import views.admin.programs.ProgramBlockShowPageViewModel;

public final class ProgramBlockShowPageMapperTest {

  private ProgramBlockShowPageMapper mapper;

  @Before
  public void setup() {
    mapper = new ProgramBlockShowPageMapper();
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

  private BlockDefinition buildBlock() {
    return BlockDefinition.builder()
        .setId(1L)
        .setName("Block 1")
        .setDescription("Block description")
        .build();
  }

  private ProgramBlockShowPageViewModel mapDefault(
      ProgramDefinition program, BlockDefinition block) {
    return mapper.map(
        program,
        block,
        ImmutableList.of(),
        ImmutableList.of(),
        /* expandedFormLogicEnabled= */ false);
  }

  @Test
  public void map_setsProgramName() {
    BlockDefinition block = buildBlock();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockShowPageViewModel result = mapDefault(program, block);

    assertThat(result.getProgramName()).isEqualTo("Test Program");
  }

  @Test
  public void map_setsProgramDescription() {
    BlockDefinition block = buildBlock();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockShowPageViewModel result = mapDefault(program, block);

    assertThat(result.getProgramDescription()).isEqualTo("Description");
  }

  @Test
  public void map_setsBlockDetails() {
    BlockDefinition block = buildBlock();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockShowPageViewModel result = mapDefault(program, block);

    assertThat(result.getBlockName()).isEqualTo("Block 1");
    assertThat(result.getBlockDescription()).isEqualTo("Block description");
    assertThat(result.getSelectedBlockId()).isEqualTo(1L);
  }

  @Test
  public void map_setsBlockList() {
    BlockDefinition block = buildBlock();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockShowPageViewModel result = mapDefault(program, block);

    assertThat(result.getBlockList()).hasSize(1);
    assertThat(result.getBlockList().get(0).getName()).isEqualTo("Block 1");
    assertThat(result.getBlockList().get(0).isSelected()).isTrue();
  }

  @Test
  public void map_nonExternalProgram_setsPreviewUrl() {
    BlockDefinition block = buildBlock();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockShowPageViewModel result = mapDefault(program, block);

    assertThat(result.isExternal()).isFalse();
    assertThat(result.getPreviewUrl()).isPresent();
    assertThat(result.getDownloadPdfUrl()).isPresent();
  }

  @Test
  public void map_externalProgram_noPreviewUrl() {
    BlockDefinition block = buildBlock();
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

    ProgramBlockShowPageViewModel result = mapDefault(program, block);

    assertThat(result.isExternal()).isTrue();
    assertThat(result.getPreviewUrl()).isEmpty();
    assertThat(result.getDownloadPdfUrl()).isEmpty();
  }

  @Test
  public void map_noCategoriesProgram_setsCategoriesNone() {
    BlockDefinition block = buildBlock();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockShowPageViewModel result = mapDefault(program, block);

    assertThat(result.getCategoriesText()).isEqualTo("None");
  }

  @Test
  public void map_emptyBlockQuestions_setsEmptyQuestionsList() {
    BlockDefinition block = buildBlock();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockShowPageViewModel result =
        mapper.map(
            program,
            block,
            ImmutableList.<QuestionDefinition>of(),
            ImmutableList.<QuestionDefinition>of(),
            false);

    assertThat(result.getQuestions()).isEmpty();
  }

  @Test
  public void map_noVisibilityPredicate_setsPredicateNotPresent() {
    BlockDefinition block = buildBlock();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockShowPageViewModel result = mapDefault(program, block);

    assertThat(result.getVisibilityPredicate().isPresent()).isFalse();
  }

  @Test
  public void map_defaultProgram_showsEligibilitySection() {
    BlockDefinition block = buildBlock();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockShowPageViewModel result = mapDefault(program, block);

    assertThat(result.isShowEligibilitySection()).isTrue();
  }

  @Test
  public void map_preScreenerProgram_hidesEligibilitySection() {
    BlockDefinition block = buildBlock();
    ProgramDefinition program =
        ProgramDefinition.builder()
            .setId(1L)
            .setAdminName("prescreener")
            .setAdminDescription("desc")
            .setLocalizedName(LocalizedStrings.of(Locale.US, "Pre-screener"))
            .setLocalizedDescription(LocalizedStrings.of(Locale.US, "Desc"))
            .setLocalizedShortDescription(LocalizedStrings.of(Locale.US, "Short"))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.PRE_SCREENER_FORM)
            .setLocalizedConfirmationMessage(LocalizedStrings.empty())
            .setAcls(new ProgramAcls())
            .setBlockDefinitions(ImmutableList.of(block))
            .build();

    ProgramBlockShowPageViewModel result = mapDefault(program, block);

    assertThat(result.isShowEligibilitySection()).isFalse();
  }

  @Test
  public void map_noEligibilityPredicate_setsEligibilityPredicateNotPresent() {
    BlockDefinition block = buildBlock();
    ProgramDefinition program = buildProgram(block);

    ProgramBlockShowPageViewModel result = mapDefault(program, block);

    assertThat(result.getEligibilityPredicate().isPresent()).isFalse();
  }
}
