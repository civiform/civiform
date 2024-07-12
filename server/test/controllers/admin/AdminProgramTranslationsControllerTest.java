package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.BadRequestException;
import controllers.FlashKey;
import java.util.Locale;
import java.util.Optional;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import repository.ProgramRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.program.StatusDefinitions;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import support.ProgramBuilder;

public class AdminProgramTranslationsControllerTest extends ResetPostgres {

  private static final Locale ES_LOCALE = Locale.forLanguageTag("es-US");

  private static final String ENGLISH_DISPLAY_NAME = "english program display name";
  private static final String ENGLISH_DESCRIPTION = "english program description";
  private static final String SPANISH_DISPLAY_NAME = "spanish program display name";
  private static final String SPANISH_DESCRIPTION = "spanish program description";

  private static final String ENGLISH_FIRST_STATUS_TEXT = "english first status text";
  private static final String ENGLISH_FIRST_STATUS_EMAIL = "english first status email";
  private static final String ENGLISH_SECOND_STATUS_TEXT = "english second status text";
  private static final String ENGLISH_SECOND_STATUS_EMAIL = "english second status email";

  private static final String SPANISH_FIRST_STATUS_TEXT = "spanish first status text";
  private static final String SPANISH_FIRST_STATUS_EMAIL = "spanish first status email";
  private static final String SPANISH_SECOND_STATUS_TEXT = "spanish second status text";
  private static final String SPANISH_SECOND_STATUS_EMAIL = "spanish second status email";

  private ProgramRepository programRepository;
  private AdminProgramTranslationsController controller;

  @Before
  public void setup() {
    programRepository = instanceOf(ProgramRepository.class);
    controller = instanceOf(AdminProgramTranslationsController.class);
  }

  @Test
  public void edit_defaultLocaleRedirectsWithError() throws ProgramNotFoundException {
    ProgramModel program = createDraftProgramEnglishAndSpanish(STATUSES_WITH_EMAIL);

    Result result =
        controller.edit(
            addCSRFToken(fakeRequest()).build(),
            program.getProgramDefinition().adminName(),
            "en-US");

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(routes.AdminProgramController.index().url());
    assertThat(result.flash().get(FlashKey.ERROR)).isPresent();
    assertThat(result.flash().get(FlashKey.ERROR).get())
        .isEqualTo("The en-US locale is not supported");
  }

  @Test
  public void edit_rendersFormWithExistingContent_otherLocale() throws ProgramNotFoundException {
    ProgramModel program = createDraftProgramEnglishAndSpanish(STATUSES_WITH_EMAIL);

    Result result =
        controller.edit(
            addCSRFToken(fakeRequest()).build(),
            program.getProgramDefinition().adminName(),
            "es-US");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(
            String.format("Manage program translations: %s", ENGLISH_DISPLAY_NAME),
            "Spanish",
            SPANISH_DISPLAY_NAME,
            SPANISH_DESCRIPTION);
    assertThat(contentAsString(result))
        .contains("English text:", ENGLISH_DISPLAY_NAME, ENGLISH_DESCRIPTION);
    assertThat(contentAsString(result))
        .contains(
            SPANISH_FIRST_STATUS_TEXT, SPANISH_FIRST_STATUS_EMAIL,
            SPANISH_SECOND_STATUS_TEXT, SPANISH_SECOND_STATUS_EMAIL);
    assertThat(contentAsString(result))
        .contains(
            "English text:",
            ENGLISH_FIRST_STATUS_TEXT,
            ENGLISH_FIRST_STATUS_EMAIL,
            ENGLISH_SECOND_STATUS_TEXT,
            ENGLISH_SECOND_STATUS_EMAIL);
  }

  @Test
  public void edit_rendersFormWithExistingContent_noStatusEmail_otherLocale()
      throws ProgramNotFoundException {
    ProgramModel program = createDraftProgramEnglishAndSpanish(STATUSES_WITH_NO_EMAIL);

    Result result =
        controller.edit(
            addCSRFToken(fakeRequest()).build(),
            program.getProgramDefinition().adminName(),
            "es-US");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(
            String.format("Manage program translations: %s", ENGLISH_DISPLAY_NAME),
            "Spanish",
            SPANISH_DISPLAY_NAME,
            SPANISH_DESCRIPTION);
    assertThat(contentAsString(result))
        .contains("English text:", ENGLISH_DISPLAY_NAME, ENGLISH_DESCRIPTION);
    assertThat(contentAsString(result))
        .contains(SPANISH_FIRST_STATUS_TEXT, SPANISH_SECOND_STATUS_TEXT);
    assertThat(contentAsString(result))
        .doesNotContain(SPANISH_FIRST_STATUS_EMAIL, SPANISH_SECOND_STATUS_EMAIL);
    assertThat(contentAsString(result))
        .contains("English text:", ENGLISH_FIRST_STATUS_TEXT, ENGLISH_SECOND_STATUS_TEXT);
    assertThat(contentAsString(result))
        .doesNotContain(ENGLISH_FIRST_STATUS_EMAIL, ENGLISH_SECOND_STATUS_EMAIL);
  }

  @Test
  public void edit_programNotFound_returnsNotFound() {
    assertThatThrownBy(
            () ->
                controller.edit(
                    addCSRFToken(fakeRequest()).build(), "non-existent program name", "es-US"))
        .hasMessage("No draft found for program: \"non-existent program name\"")
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void update_savesNewFields() throws Exception {
    ProgramModel program = createDraftProgramEnglishAndSpanish(STATUSES_WITH_EMAIL);

    Http.RequestBuilder requestBuilder =
        fakeRequest()
            .bodyForm(
                ImmutableMap.<String, String>builder()
                    .put("displayName", "updated spanish program display name")
                    .put("displayDescription", "updated spanish program description")
                    .put("status-key-to-update-0", ENGLISH_FIRST_STATUS_TEXT)
                    .put("localized-status-0", "updated spanish first status text")
                    .put("localized-email-0", "updated spanish first status email")
                    .put("status-key-to-update-1", ENGLISH_SECOND_STATUS_TEXT)
                    .put("localized-status-1", "updated spanish second status text")
                    .put("localized-email-1", "updated spanish second status email")
                    .build());

    Result result =
        controller.update(
            addCSRFToken(requestBuilder).build(),
            program.getProgramDefinition().adminName(),
            "es-US");
    assertThat(result.status()).isEqualTo(OK);

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
    assertThat(updatedProgram.statusDefinitions().getStatuses())
        .isEqualTo(
            ImmutableList.of(
                StatusDefinitions.Status.builder()
                    .setStatusText(STATUSES_WITH_EMAIL.get(0).statusText())
                    .setLocalizedStatusText(
                        STATUSES_WITH_EMAIL
                            .get(0)
                            .localizedStatusText()
                            .updateTranslation(ES_LOCALE, "updated spanish first status text"))
                    .setLocalizedEmailBodyText(
                        Optional.of(
                            STATUSES_WITH_EMAIL
                                .get(0)
                                .localizedEmailBodyText()
                                .get()
                                .updateTranslation(
                                    ES_LOCALE, "updated spanish first status email")))
                    .build(),
                StatusDefinitions.Status.builder()
                    .setStatusText(STATUSES_WITH_EMAIL.get(1).statusText())
                    .setLocalizedStatusText(
                        STATUSES_WITH_EMAIL
                            .get(1)
                            .localizedStatusText()
                            .updateTranslation(ES_LOCALE, "updated spanish second status text"))
                    .setLocalizedEmailBodyText(
                        Optional.of(
                            STATUSES_WITH_EMAIL
                                .get(1)
                                .localizedEmailBodyText()
                                .get()
                                .updateTranslation(
                                    ES_LOCALE, "updated spanish second status email")))
                    .build()));
  }

  @Test
  public void update_programNotFound() {
    assertThatThrownBy(
            () ->
                controller.update(
                    addCSRFToken(fakeRequest()).build(), "non-existent program name", "es-US"))
        .hasMessage("No draft found for program: \"non-existent program name\"")
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void update_validationErrors_rendersEditFormWithMessages()
      throws ProgramNotFoundException {
    ProgramModel program = createDraftProgramEnglishAndSpanish(STATUSES_WITH_EMAIL);

    Http.RequestBuilder requestBuilder =
        fakeRequest()
            .bodyForm(
                ImmutableMap.<String, String>builder()
                    .put("displayName", "")
                    .put("displayDescription", "")
                    // Initialize fields for the two existing status values to new values and
                    // assert that they're preserved when rendering the error.
                    .put("status-key-to-update-0", ENGLISH_FIRST_STATUS_TEXT)
                    .put("localized-status-0", "new first status text")
                    .put("localized-email-0", "new first status email")
                    .put("status-key-to-update-1", ENGLISH_SECOND_STATUS_TEXT)
                    .put("localized-status-1", "new second status text")
                    .put("localized-email-1", "new second status email")
                    .build());

    Result result =
        controller.update(
            addCSRFToken(requestBuilder).build(),
            program.getProgramDefinition().adminName(),
            "es-US");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(
            String.format("Manage program translations: %s", ENGLISH_DISPLAY_NAME),
            "program display name cannot be blank",
            "program display description cannot be blank",
            "new first status text",
            "new first status email",
            "new second status text",
            "new second status email");

    assertProgramNotChanged(program);
  }

  @Test
  public void update_outOfSyncFormData_redirectsToFreshData() throws Exception {
    ProgramModel program = createDraftProgramEnglishAndSpanish(STATUSES_WITH_EMAIL);

    Http.RequestBuilder requestBuilder =
        fakeRequest()
            .bodyForm(
                ImmutableMap.<String, String>builder()
                    .put("displayName", "updated spanish program display name")
                    .put("displayDescription", "updated spanish program description")
                    // We intentionally mix up the order of the list to simulate a situation
                    // where the tab is out of sync with the configured program definition.
                    .put("status-key-to-update-0", ENGLISH_SECOND_STATUS_TEXT)
                    .put("localized-status-0", "updated spanish first status text")
                    .put("localized-email-0", "updated spanish first status email")
                    .put("status-key-to-update-1", ENGLISH_FIRST_STATUS_TEXT)
                    .put("localized-status-1", "updated spanish second status text")
                    .put("localized-email-1", "updated spanish second status email")
                    .build());

    Result result =
        controller.update(
            addCSRFToken(requestBuilder).build(),
            program.getProgramDefinition().adminName(),
            "es-US");

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation().orElse(""))
        .isEqualTo(
            controllers.admin.routes.AdminProgramTranslationsController.edit(
                    program.getProgramDefinition().adminName(), "es-US")
                .url());
    assertThat(result.flash().get(FlashKey.ERROR).get())
        .isEqualTo("The program's associated statuses are out of date.");

    assertProgramNotChanged(program);
  }

  private void assertProgramNotChanged(ProgramModel initialProgram) {
    ProgramDefinition freshProgram =
        programRepository
            .lookupProgram(initialProgram.id)
            .toCompletableFuture()
            .join()
            .get()
            .getProgramDefinition();
    assertThat(freshProgram.localizedName())
        .isEqualTo(initialProgram.getProgramDefinition().localizedName());
    assertThat(freshProgram.localizedDescription())
        .isEqualTo(initialProgram.getProgramDefinition().localizedDescription());
    assertThat(freshProgram.statusDefinitions().getStatuses())
        .isEqualTo(initialProgram.getProgramDefinition().statusDefinitions().getStatuses());
  }

  private static final ImmutableList<StatusDefinitions.Status> STATUSES_WITH_EMAIL =
      ImmutableList.of(
          StatusDefinitions.Status.builder()
              .setStatusText(ENGLISH_FIRST_STATUS_TEXT)
              .setLocalizedStatusText(
                  LocalizedStrings.withDefaultValue(ENGLISH_FIRST_STATUS_TEXT)
                      .updateTranslation(ES_LOCALE, SPANISH_FIRST_STATUS_TEXT))
              .setLocalizedEmailBodyText(
                  Optional.of(
                      LocalizedStrings.withDefaultValue(ENGLISH_FIRST_STATUS_EMAIL)
                          .updateTranslation(ES_LOCALE, SPANISH_FIRST_STATUS_EMAIL)))
              .build(),
          StatusDefinitions.Status.builder()
              .setStatusText(ENGLISH_SECOND_STATUS_TEXT)
              .setLocalizedStatusText(
                  LocalizedStrings.withDefaultValue(ENGLISH_SECOND_STATUS_TEXT)
                      .updateTranslation(ES_LOCALE, SPANISH_SECOND_STATUS_TEXT))
              .setLocalizedEmailBodyText(
                  Optional.of(
                      LocalizedStrings.withDefaultValue(ENGLISH_SECOND_STATUS_EMAIL)
                          .updateTranslation(ES_LOCALE, SPANISH_SECOND_STATUS_EMAIL)))
              .build());

  private static final ImmutableList<StatusDefinitions.Status> STATUSES_WITH_NO_EMAIL =
      ImmutableList.of(
          StatusDefinitions.Status.builder()
              .setStatusText(ENGLISH_FIRST_STATUS_TEXT)
              .setLocalizedStatusText(
                  LocalizedStrings.withDefaultValue(ENGLISH_FIRST_STATUS_TEXT)
                      .updateTranslation(ES_LOCALE, SPANISH_FIRST_STATUS_TEXT))
              .build(),
          StatusDefinitions.Status.builder()
              .setStatusText(ENGLISH_SECOND_STATUS_TEXT)
              .setLocalizedStatusText(
                  LocalizedStrings.withDefaultValue(ENGLISH_SECOND_STATUS_TEXT)
                      .updateTranslation(ES_LOCALE, SPANISH_SECOND_STATUS_TEXT))
              .build());

  private ProgramModel createDraftProgramEnglishAndSpanish(
      ImmutableList<StatusDefinitions.Status> statuses) {
    ProgramModel initialProgram = ProgramBuilder.newDraftProgram("Internal program name").build();
    // ProgamBuilder initializes the localized name and doesn't currently support providing
    // overrides. Here we manually update the localized string in a separate update.
    ProgramModel program =
        initialProgram.getProgramDefinition().toBuilder()
            .setLocalizedName(
                LocalizedStrings.withDefaultValue(ENGLISH_DISPLAY_NAME)
                    .updateTranslation(ES_LOCALE, SPANISH_DISPLAY_NAME))
            .setLocalizedDescription(
                LocalizedStrings.withDefaultValue(ENGLISH_DESCRIPTION)
                    .updateTranslation(ES_LOCALE, SPANISH_DESCRIPTION))
            .setStatusDefinitions(new StatusDefinitions(statuses))
            .build()
            .toProgram();
    program.update();
    return program;
  }
}
