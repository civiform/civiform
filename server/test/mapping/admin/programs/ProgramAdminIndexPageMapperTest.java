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

  private ProgramAdminIndexPageMapper mapper;
  private DateConverter dateConverter;

  @Before
  public void setup() {
    mapper = new ProgramAdminIndexPageMapper();

    dateConverter = mock(DateConverter.class);
    when(dateConverter.renderDateTimeHumanReadable(PUBLISHED_AT))
        .thenReturn("2024/01/01 at 9:00 AM PST");
    when(dateConverter.renderDate(PUBLISHED_AT)).thenReturn("2024/01/01");
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

  private ProgramAdminIndexPageViewModel map(ProgramDefinition... programs) {
    return mapper.map(
        ImmutableList.copyOf(programs),
        /* administeredProgramNames= */ ImmutableList.of("utility-discount", "pre-screener"),
        BASE_URL,
        dateConverter);
  }

  private ProgramCard onlyCard(ProgramAdminIndexPageViewModel model) {
    assertThat(model.getProgramCards()).hasSize(1);
    return model.getProgramCards().get(0);
  }

  @Test
  public void map_defaultProgram_buildsFullCard() {
    ProgramDefinition program =
        programBuilder(1L, "utility-discount")
            .setAdminDescription("Internal note")
            .setBlockDefinitions(ImmutableList.of(block(1L, 2), block(2L, 3)))
            .build();

    ProgramAdminIndexPageViewModel result = map(program);

    assertThat(result.getTitle()).isEqualTo("Your programs");
    assertThat(onlyCard(result))
        .isEqualTo(
            ProgramCard.builder()
                .id(1L)
                .title("Utility Discount")
                .shortDescription("Help with utilities")
                .programType(ProgramType.DEFAULT)
                .preScreenerForm(false)
                .adminNote("Internal note")
                .visibilityState("Public")
                .categoriesText("None")
                .publishedDateTime("2024/01/01 at 9:00 AM PST")
                .publishedDate("2024/01/01")
                .blockCount(2)
                .questionCount(5)
                .programLink("https://civiform.example.com/programs/utility-discount")
                .applicationsUrl("/admin/programs/1/applications")
                .build());
  }

  @Test
  public void map_onlyIncludesAdministeredPrograms() {
    ProgramDefinition otherProgram = programBuilder(2L, "other-program").build();

    ProgramAdminIndexPageViewModel result = map(program(), otherProgram);

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

    ProgramAdminIndexPageViewModel result = map(olderB, newer, olderA, preScreener);

    assertThat(result.getProgramCards())
        .extracting(ProgramCard::getId)
        .containsExactly(4L, 3L, 1L, 2L);
  }

  @Test
  public void map_preScreenerForm_setsTypeAndRowActions() {
    ProgramDefinition preScreener =
        programBuilder(1L, "pre-screener").setProgramType(ProgramType.PRE_SCREENER_FORM).build();

    ProgramCard card = onlyCard(map(preScreener));

    assertThat(card.getProgramType()).isEqualTo(ProgramType.PRE_SCREENER_FORM);
    assertThat(card.isPreScreenerForm()).isTrue();
    assertThat(card.getProgramLink())
        .isEqualTo("https://civiform.example.com/programs/pre-screener");
    assertThat(card.getApplicationsUrl()).isEqualTo("/admin/programs/1/applications");
  }

  @Test
  public void map_externalProgram_hasNoRowActions() {
    ProgramDefinition external =
        programBuilder(1L, "utility-discount").setProgramType(ProgramType.EXTERNAL).build();

    ProgramCard card = onlyCard(map(external));

    assertThat(card.getProgramType()).isEqualTo(ProgramType.EXTERNAL);
    assertThat(card.getProgramLink()).isNull();
    assertThat(card.getApplicationsUrl()).isNull();
  }

  @Test
  public void map_missingOptionalFields_rendersPlaceholders() {
    ProgramDefinition program =
        programBuilder(1L, "utility-discount")
            .setAdminDescription("   ")
            .setLastModifiedTime(/* lastModifiedTime= */ null)
            .build();

    ProgramCard card = onlyCard(map(program));

    assertThat(card.getAdminNote()).isNull();
    assertThat(card.getPublishedDateTime()).isEqualTo("unknown");
    assertThat(card.getPublishedDate()).isEqualTo("unknown");
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

    ProgramCard card = onlyCard(map(program));

    assertThat(card.getCategoriesText()).isEqualTo("Childcare, Housing");
  }
}
