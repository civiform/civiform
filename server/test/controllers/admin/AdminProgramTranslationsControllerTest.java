package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import repository.ProgramRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import support.ProgramBuilder;

public class AdminProgramTranslationsControllerTest extends ResetPostgres {

  private static Locale ES_LOCALE = Locale.forLanguageTag("es-US");

  private static String ENGLISH_DISPLAY_NAME = "english program display name";
  private static String ENGLISH_DESCRIPTION = "english program description";
  private static String SPANISH_DISPLAY_NAME = "spanish program display name";
  private static String SPANISH_DESCRIPTION = "spanish program description";

  private ProgramRepository programRepository;
  private AdminProgramTranslationsController controller;

  @Before
  public void setup() {
    programRepository = instanceOf(ProgramRepository.class);
    controller = instanceOf(AdminProgramTranslationsController.class);
  }

  @Test
  public void edit_defaultLocaleRedirectsWithError() throws ProgramNotFoundException {
    Program program = createDraftProgramEnglishAndSpanish();

    Result result = controller.edit(addCSRFToken(fakeRequest()).build(), program.id, "en-US");

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(routes.AdminProgramController.index().url());
    assertThat(result.flash().get("error")).isPresent();
    assertThat(result.flash().get("error").get()).isEqualTo("The en-US locale is not supported");
  }

  @Test
  public void edit_rendersFormWithExistingNameAndDescription_otherLocale()
      throws ProgramNotFoundException {
    Program program = createDraftProgramEnglishAndSpanish();

    Result result = controller.edit(addCSRFToken(fakeRequest()).build(), program.id, "es-US");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(
            String.format("Manage program translations: Internal program name"),
            "Spanish",
            SPANISH_DISPLAY_NAME,
            SPANISH_DESCRIPTION);
    assertThat(contentAsString(result))
        .contains("Default text:", ENGLISH_DISPLAY_NAME, ENGLISH_DESCRIPTION);
  }

  @Test
  public void edit_programNotFound_returnsNotFound() {
    assertThatThrownBy(() -> controller.edit(addCSRFToken(fakeRequest()).build(), 1000L, "es-US"))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void update_savesNewFields() throws Exception {
    Program program = ProgramBuilder.newDraftProgram().build();

    Http.RequestBuilder requestBuilder =
        fakeRequest()
            .bodyForm(
                ImmutableMap.of(
                    "displayName",
                    "updated spanish program display name",
                    "displayDescription",
                    "updated spanish program description"));

    Result result = controller.update(addCSRFToken(requestBuilder).build(), program.id, "es-US");

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    ProgramDefinition updatedProgram =
        programRepository
            .lookupProgram(program.id)
            .toCompletableFuture()
            .join()
            .get()
            .getProgramDefinition();
    assertThat(updatedProgram.localizedName().get(ES_LOCALE))
        .isEqualTo("updated spanish program display name");
    assertThat(updatedProgram.localizedDescription().get(ES_LOCALE))
        .isEqualTo("updated spanish program description");
  }

  @Test
  public void update_programNotFound() {
    assertThatThrownBy(() -> controller.update(addCSRFToken(fakeRequest()).build(), 1000L, "es-US"))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void update_validationErrors_rendersEditFormWithMessages()
      throws ProgramNotFoundException {
    Program program = createDraftProgramEnglishAndSpanish();

    Http.RequestBuilder requestBuilder =
        fakeRequest().bodyForm(ImmutableMap.of("displayName", "", "displayDescription", ""));

    Result result = controller.update(addCSRFToken(requestBuilder).build(), program.id, "es-US");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(
            String.format("Manage program translations: Internal program name"),
            "program display name cannot be blank",
            "program display description cannot be blank");
  }

  private Program createDraftProgramEnglishAndSpanish() {
    Program initialProgram = ProgramBuilder.newDraftProgram("Internal program name").build();
    // ProgamBuilder initializes the localized name and doesn't currently support providing
    // overrides. Here we manually update the localized string in a separate update.
    Program program =
        initialProgram.getProgramDefinition().toBuilder()
            .setLocalizedName(
                LocalizedStrings.withDefaultValue(ENGLISH_DISPLAY_NAME)
                    .updateTranslation(ES_LOCALE, SPANISH_DISPLAY_NAME))
            .setLocalizedDescription(
                LocalizedStrings.withDefaultValue(ENGLISH_DESCRIPTION)
                    .updateTranslation(ES_LOCALE, SPANISH_DESCRIPTION))
            .build()
            .toProgram();
    program.update();
    return program;
  }
}
