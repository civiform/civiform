package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import models.ApplicationStep;
import models.CategoryModel;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import services.DateConverter;
import services.LocalizedStrings;
import services.cloud.PublicStorageClient;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramType;
import services.question.types.QuestionDefinition;
import views.admin.programs.ProgramAdminIndexPageViewModel;
import views.admin.programs.ProgramAdminIndexPageViewModel.ProgramCard;

public final class ProgramAdminIndexPageMapperTest {

  private static final String BASE_URL = "https://civiform.example.com";
  private static final Instant PUBLISHED_AT = Instant.parse("2024-01-01T17:00:00Z");
  private static final Instant PUBLISHED_EARLIER = Instant.parse("2023-06-15T17:00:00Z");
  private static final String IMAGE_FILE_KEY = "program-summary-image/program-1/image.png";

  private ProgramAdminIndexPageMapper mapper;
  private DateConverter dateConverter;
  private PublicStorageClient publicStorageClient;

  @Before
  public void setup() {
    // The mapper resolves image alt text against the JVM default locale, like the legacy view.
    Locale.setDefault(Locale.US);

    mapper = new ProgramAdminIndexPageMapper();

    dateConverter = mock(DateConverter.class);
    when(dateConverter.renderDateTimeHumanReadable(PUBLISHED_AT))
        .thenReturn("2024/01/01 at 9:00 AM PST");
    when(dateConverter.renderDate(PUBLISHED_AT)).thenReturn("2024/01/01");

    publicStorageClient = mock(PublicStorageClient.class);
    when(publicStorageClient.getPublicDisplayUrl(IMAGE_FILE_KEY))
        .thenReturn("https://storage.example.com/image.png");
  }

  private static ProgramDefinition.Builder programBuilder(long id, String adminName) {
    return ProgramDefinition.builder()
        .setId(id)
        .setAdminName(adminName)
        .setAdminDescription("")
        .setLocalizedName(LocalizedStrings.withDefaultValue("Utility Discount"))
        .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
        .setLocalizedShortDescription(LocalizedStrings.withDefaultValue("Help with utilities"))
        .setExternalLink("")
        .setDisplayMode(DisplayMode.PUBLIC)
        .setProgramType(ProgramType.DEFAULT)
        .setEligibilityIsGating(true)
        .setLoginOnly(false)
        .setAcls(new ProgramAcls())
        .setCategories(ImmutableList.of())
        .setLastModifiedTime(PUBLISHED_AT)
        .setApplicationSteps(ImmutableList.of(new ApplicationStep("title", "description")))
        .setBridgeDefinitions(ImmutableMap.of());
  }

  private static ProgramDefinition program() {
    return programBuilder(1L, "utility-discount").build();
  }

  private static BlockDefinition block(long id, int questionCount) {
    ImmutableList.Builder<ProgramQuestionDefinition> questions = ImmutableList.builder();
    for (int i = 0; i < questionCount; i++) {
      QuestionDefinition question = mock(QuestionDefinition.class);
      when(question.getId()).thenReturn(id * 100 + i);
      questions.add(
          ProgramQuestionDefinition.create(question, /* programDefinitionId= */ Optional.empty()));
    }

    return BlockDefinition.builder()
        .setId(id)
        .setName("Screen " + id)
        .setDescription("Screen " + id + " description")
        .setLocalizedName(LocalizedStrings.withDefaultValue("Screen " + id))
        .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen " + id + " description"))
        .setProgramQuestionDefinitions(questions.build())
        .build();
  }

  private ProgramAdminIndexPageViewModel mapAsProgramAdmin(ProgramDefinition... programs) {
    return mapper.map(
        ImmutableList.copyOf(programs),
        /* administeredProgramNames= */ ImmutableList.of("utility-discount", "pre-screener"),
        /* isCiviFormAdmin= */ false,
        BASE_URL,
        dateConverter,
        publicStorageClient);
  }

  private ProgramAdminIndexPageViewModel mapAsCiviFormAdmin(ProgramDefinition... programs) {
    return mapper.map(
        ImmutableList.copyOf(programs),
        /* administeredProgramNames= */ ImmutableList.of("utility-discount"),
        /* isCiviFormAdmin= */ true,
        BASE_URL,
        dateConverter,
        publicStorageClient);
  }

  private ProgramCard onlyCard(ProgramAdminIndexPageViewModel model) {
    assertThat(model.getProgramCards()).hasSize(1);
    return model.getProgramCards().get(0);
  }

  @Test
  public void map_setsTitle() {
    ProgramAdminIndexPageViewModel result = mapAsProgramAdmin(program());

    assertThat(result.getTitle()).isEqualTo("Your programs");
  }

  @Test
  public void map_onlyIncludesAdministeredPrograms() {
    ProgramDefinition otherProgram = programBuilder(2L, "other-program").build();

    ProgramAdminIndexPageViewModel result = mapAsProgramAdmin(program(), otherProgram);

    assertThat(result.getProgramCards()).extracting(ProgramCard::getId).containsExactly(1L);
  }

  @Test
  public void map_sortsPreScreenerFirstThenLastModifiedThenName() {
    ProgramDefinition olderA =
        programBuilder(1L, "utility-discount")
            .setLocalizedName(LocalizedStrings.withDefaultValue("apple"))
            .setLastModifiedTime(PUBLISHED_EARLIER)
            .build();
    ProgramDefinition olderB =
        programBuilder(2L, "utility-discount")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Banana"))
            .setLastModifiedTime(PUBLISHED_EARLIER)
            .build();
    ProgramDefinition newer =
        programBuilder(3L, "utility-discount")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Zebra"))
            .setLastModifiedTime(PUBLISHED_AT)
            .build();
    ProgramDefinition preScreener =
        programBuilder(4L, "pre-screener")
            .setProgramType(ProgramType.PRE_SCREENER_FORM)
            .setLastModifiedTime(PUBLISHED_EARLIER)
            .build();

    ProgramAdminIndexPageViewModel result = mapAsProgramAdmin(olderB, newer, olderA, preScreener);

    assertThat(result.getProgramCards())
        .extracting(ProgramCard::getId)
        .containsExactly(4L, 3L, 1L, 2L);
  }

  @Test
  public void map_setsCardTitleAndDescription() {
    ProgramCard card = onlyCard(mapAsProgramAdmin(program()));

    assertThat(card.getTitle()).isEqualTo("Utility Discount");
    assertThat(card.getShortDescription()).isEqualTo("Help with utilities");
  }

  @Test
  public void map_setsProgramType() {
    ProgramDefinition preScreener =
        programBuilder(1L, "pre-screener").setProgramType(ProgramType.PRE_SCREENER_FORM).build();

    ProgramCard card = onlyCard(mapAsProgramAdmin(preScreener));

    assertThat(card.getProgramType()).isEqualTo(ProgramType.PRE_SCREENER_FORM);
    assertThat(card.isPreScreenerForm()).isTrue();
  }

  @Test
  public void map_defaultProgramIsNotPreScreenerForm() {
    ProgramCard card = onlyCard(mapAsProgramAdmin(program()));

    assertThat(card.getProgramType()).isEqualTo(ProgramType.DEFAULT);
    assertThat(card.isPreScreenerForm()).isFalse();
  }

  @Test
  public void map_setsAdminNote() {
    ProgramDefinition program =
        programBuilder(1L, "utility-discount").setAdminDescription("Internal note").build();

    ProgramCard card = onlyCard(mapAsProgramAdmin(program));

    assertThat(card.getAdminNote()).isEqualTo("Internal note");
  }

  @Test
  public void map_blankAdminNoteIsNull() {
    ProgramDefinition program =
        programBuilder(1L, "utility-discount").setAdminDescription("   ").build();

    ProgramCard card = onlyCard(mapAsProgramAdmin(program));

    assertThat(card.getAdminNote()).isNull();
  }

  @Test
  public void map_setsVisibilityState() {
    ProgramDefinition program =
        programBuilder(1L, "utility-discount").setDisplayMode(DisplayMode.HIDDEN_IN_INDEX).build();

    ProgramCard card = onlyCard(mapAsProgramAdmin(program));

    assertThat(card.getVisibilityState()).isEqualTo("Hidden");
  }

  @Test
  public void map_noCategoriesRendersNone() {
    ProgramCard card = onlyCard(mapAsProgramAdmin(program()));

    assertThat(card.getCategoriesText()).isEqualTo("None");
  }

  @Test
  public void map_joinsSortedCategoryNames() {
    ProgramDefinition program =
        programBuilder(1L, "utility-discount")
            .setCategories(
                ImmutableList.of(
                    new CategoryModel(ImmutableMap.of(Locale.US, "Housing")),
                    new CategoryModel(ImmutableMap.of(Locale.US, "Childcare"))))
            .build();

    ProgramCard card = onlyCard(mapAsProgramAdmin(program));

    assertThat(card.getCategoriesText()).isEqualTo("Childcare, Housing");
  }

  @Test
  public void map_setsPublishedDates() {
    ProgramCard card = onlyCard(mapAsProgramAdmin(program()));

    assertThat(card.getPublishedDateTime()).isEqualTo("2024/01/01 at 9:00 AM PST");
    assertThat(card.getPublishedDate()).isEqualTo("2024/01/01");
  }

  @Test
  public void map_missingLastModifiedTimeRendersUnknown() {
    ProgramDefinition program =
        programBuilder(1L, "utility-discount")
            .setLastModifiedTime(/* lastModifiedTime= */ null)
            .build();

    ProgramCard card = onlyCard(mapAsProgramAdmin(program));

    assertThat(card.getPublishedDateTime()).isEqualTo("unknown");
    assertThat(card.getPublishedDate()).isEqualTo("unknown");
  }

  @Test
  public void map_setsBlockAndQuestionCounts() {
    ProgramDefinition program =
        programBuilder(1L, "utility-discount")
            .setBlockDefinitions(ImmutableList.of(block(1L, 2), block(2L, 3)))
            .build();

    ProgramCard card = onlyCard(mapAsProgramAdmin(program));

    assertThat(card.getBlockCount()).isEqualTo(2);
    assertThat(card.getQuestionCount()).isEqualTo(5);
  }

  @Test
  public void map_programAdminHasNoImageColumn() {
    ProgramDefinition program =
        programBuilder(1L, "utility-discount")
            .setSummaryImageFileKey(Optional.of(IMAGE_FILE_KEY))
            .build();

    ProgramCard card = onlyCard(mapAsProgramAdmin(program));

    assertThat(card.isShowImageColumn()).isFalse();
    assertThat(card.getImageUrl()).isNull();
    assertThat(card.getImageAltText()).isNull();
  }

  @Test
  public void map_civiFormAdminSeesProgramImage() {
    ProgramDefinition program =
        programBuilder(1L, "utility-discount")
            .setSummaryImageFileKey(Optional.of(IMAGE_FILE_KEY))
            .setLocalizedSummaryImageDescription(
                Optional.of(LocalizedStrings.withDefaultValue("A family at home")))
            .build();

    ProgramCard card = onlyCard(mapAsCiviFormAdmin(program));

    assertThat(card.isShowImageColumn()).isTrue();
    assertThat(card.getImageUrl()).isEqualTo("https://storage.example.com/image.png");
    assertThat(card.getImageAltText()).isEqualTo("A family at home");
  }

  @Test
  public void map_civiFormAdminImageAltTextFallsBackToProgramName() {
    ProgramDefinition program =
        programBuilder(1L, "utility-discount")
            .setSummaryImageFileKey(Optional.of(IMAGE_FILE_KEY))
            .build();

    ProgramCard card = onlyCard(mapAsCiviFormAdmin(program));

    assertThat(card.getImageAltText()).isEqualTo("Utility Discount");
  }

  @Test
  public void map_civiFormAdminWithoutImageGetsPlaceholder() {
    ProgramCard card = onlyCard(mapAsCiviFormAdmin(program()));

    assertThat(card.isShowImageColumn()).isTrue();
    assertThat(card.getImageUrl()).isNull();
  }

  @Test
  public void map_civiFormAdminIgnoresNonPublicImageFileKey() {
    ProgramDefinition program =
        programBuilder(1L, "utility-discount")
            .setSummaryImageFileKey(Optional.of("applicant-1/private.png"))
            .build();

    ProgramCard card = onlyCard(mapAsCiviFormAdmin(program));

    assertThat(card.getImageUrl()).isNull();
  }

  @Test
  public void map_setsRowActionUrlsForDefaultProgram() {
    ProgramCard card = onlyCard(mapAsProgramAdmin(program()));

    assertThat(card.getProgramLink())
        .isEqualTo("https://civiform.example.com/programs/utility-discount");
    assertThat(card.getApplicationsUrl()).isEqualTo("/admin/programs/1/applications");
  }

  @Test
  public void map_setsRowActionUrlsForPreScreenerForm() {
    ProgramDefinition preScreener =
        programBuilder(1L, "pre-screener").setProgramType(ProgramType.PRE_SCREENER_FORM).build();

    ProgramCard card = onlyCard(mapAsProgramAdmin(preScreener));

    assertThat(card.getProgramLink())
        .isEqualTo("https://civiform.example.com/programs/pre-screener");
    assertThat(card.getApplicationsUrl()).isEqualTo("/admin/programs/1/applications");
  }

  @Test
  public void map_externalProgramHasNoRowActions() {
    ProgramDefinition external =
        programBuilder(1L, "utility-discount").setProgramType(ProgramType.EXTERNAL).build();

    ProgramCard card = onlyCard(mapAsProgramAdmin(external));

    assertThat(card.getProgramLink()).isNull();
    assertThat(card.getApplicationsUrl()).isNull();
  }
}
