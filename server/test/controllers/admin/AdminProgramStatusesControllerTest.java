package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.mvc.Result;
import play.test.Helpers;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.StatusDefinitions;
import support.ProgramBuilder;
import views.style.ReferenceClasses;

@RunWith(JUnitParamsRunner.class)
public class AdminProgramStatusesControllerTest extends ResetPostgres {

  private ProgramService programService;
  private AdminProgramStatusesController controller;

  private static final StatusDefinitions.Status APPROVED_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Approved")
          .setDefaultStatus(Optional.of(false))
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Approved"))
          .setLocalizedEmailBodyText(
              Optional.of(LocalizedStrings.withDefaultValue("Approved email body")))
          .build();

  private static final StatusDefinitions.Status REJECTED_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Rejected")
          .setDefaultStatus(Optional.of(false))
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Rejected"))
          .setLocalizedEmailBodyText(
              Optional.of(LocalizedStrings.withDefaultValue("Rejected email body")))
          .build();

  private static final StatusDefinitions.Status WITH_STATUS_TRANSLATIONS =
      StatusDefinitions.Status.builder()
          .setStatusText("With translations")
          .setDefaultStatus(Optional.of(false))
          .setLocalizedStatusText(
              LocalizedStrings.create(
                  ImmutableMap.of(
                      Locale.US, "With translations",
                      Locale.FRENCH, "With translations (French)")))
          .setLocalizedEmailBodyText(
              Optional.of(
                  LocalizedStrings.create(
                      ImmutableMap.of(
                          Locale.US, "A translatable email body",
                          Locale.FRENCH, "A translatable email body (French)"))))
          .build();

  private static final ImmutableList<StatusDefinitions.Status> ORIGINAL_STATUSES =
      ImmutableList.of(APPROVED_STATUS, REJECTED_STATUS, WITH_STATUS_TRANSLATIONS);

  @Before
  public void setup() {
    programService = instanceOf(ProgramService.class);
    controller = instanceOf(AdminProgramStatusesController.class);
  }

  @Test
  public void index_ok_get() throws ProgramNotFoundException {
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();

    Result result = controller.index(addCSRFToken(fakeRequest().method("GET")).build(), program.id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("No statuses have been created yet");
    assertThat(contentAsString(result)).doesNotContain(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);
  }

  @Test
  public void index_ok_noEmailForStatus() throws ProgramNotFoundException {
    Program program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(
                new StatusDefinitions(
                    ImmutableList.of(
                        StatusDefinitions.Status.builder()
                            .setStatusText("Status with no email")
                            .setLocalizedStatusText(
                                LocalizedStrings.withDefaultValue("Status with no email"))
                            .build())))
            .build();

    Result result = controller.index(addCSRFToken(fakeRequest().method("GET")).build(), program.id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Status with no email");
    assertThat(contentAsString(result)).doesNotContain(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);
  }

  @Test
  public void update_createNewStatus() throws ProgramNotFoundException {
    Program program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();

    Result result =
        makeCreateOrUpdateRequest(
            program.id,
            ImmutableMap.of(
                "configuredStatusText", "",
                "statusText", "foo",
                "emailBody", "some email content"));

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().data())
        .containsExactlyEntriesOf(ImmutableMap.of("success", "Status created"));
    assertThat(contentAsString(result)).doesNotContain(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);

    // Load the updated program and ensure status the status is present.
    ProgramDefinition updatedProgram = programService.getProgramDefinition(program.id);
    assertThat(updatedProgram.statusDefinitions().getStatuses())
        .isEqualTo(
            ImmutableList.of(
                APPROVED_STATUS,
                REJECTED_STATUS,
                WITH_STATUS_TRANSLATIONS,
                StatusDefinitions.Status.builder()
                    .setStatusText("foo")
                    .setDefaultStatus(Optional.of(false))
                    .setLocalizedStatusText(LocalizedStrings.withDefaultValue("foo"))
                    .setLocalizedEmailBodyText(
                        Optional.of(LocalizedStrings.withDefaultValue("some email content")))
                    .build()));
  }

  @Test
  public void update_editExistingStatus() throws ProgramNotFoundException {
    Program program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();

    Result result =
        makeCreateOrUpdateRequest(
            program.id,
            ImmutableMap.of(
                "configuredStatusText", "Approved",
                "statusText", "Foo",
                "emailBody", "Updated email content"));

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().data())
        .containsExactlyEntriesOf(ImmutableMap.of("success", "Status updated"));
    assertThat(contentAsString(result)).doesNotContain(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);

    StatusDefinitions.Status expectedStatus =
        StatusDefinitions.Status.builder()
            .setStatusText("Foo")
            .setDefaultStatus(Optional.of(false))
            .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Foo"))
            .setLocalizedEmailBodyText(
                Optional.of(LocalizedStrings.withDefaultValue("Updated email content")))
            .build();

    // Load the updated program and ensure status the status is present.
    assertThat(programService.getProgramDefinition(program.id).statusDefinitions().getStatuses())
        .isEqualTo(ImmutableList.of(expectedStatus, REJECTED_STATUS, WITH_STATUS_TRANSLATIONS));
  }

  @Test
  public void update_editExistingStatusPreservesNonDefaultLocaleTranslations()
      throws ProgramNotFoundException, TranslationNotFoundException {
    Program program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();

    Result result =
        makeCreateOrUpdateRequest(
            program.id,
            ImmutableMap.of(
                "configuredStatusText", "With translations",
                "statusText", "Foo",
                "emailBody", "Updated email content"));

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().data())
        .containsExactlyEntriesOf(ImmutableMap.of("success", "Status updated"));
    assertThat(contentAsString(result)).doesNotContain(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);

    String originalFrenchStatusText =
        WITH_STATUS_TRANSLATIONS.localizedStatusText().get(Locale.FRENCH);
    String originalFrenchEmailBodyText =
        WITH_STATUS_TRANSLATIONS.localizedEmailBodyText().get().get(Locale.FRENCH);
    StatusDefinitions.Status expectedStatus =
        StatusDefinitions.Status.builder()
            .setStatusText("Foo")
            .setDefaultStatus(Optional.of(false))
            .setLocalizedStatusText(
                LocalizedStrings.create(
                    ImmutableMap.of(Locale.US, "Foo", Locale.FRENCH, originalFrenchStatusText)))
            .setLocalizedEmailBodyText(
                Optional.of(
                    LocalizedStrings.create(
                        ImmutableMap.of(
                            Locale.US,
                            "Updated email content",
                            Locale.FRENCH,
                            originalFrenchEmailBodyText))))
            .build();

    // Load the updated program and ensure the status is present.
    assertThat(programService.getProgramDefinition(program.id).statusDefinitions().getStatuses())
        .isEqualTo(ImmutableList.of(APPROVED_STATUS, REJECTED_STATUS, expectedStatus));
  }

  @Test
  public void update_editExistingStatusClearEmailClearsTranslatedEmailContent()
      throws ProgramNotFoundException, TranslationNotFoundException {
    Program program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();

    Result result =
        makeCreateOrUpdateRequest(
            program.id,
            ImmutableMap.of(
                "configuredStatusText", "With translations",
                "statusText", "Foo",
                "emailBody", ""));

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().data())
        .containsExactlyEntriesOf(ImmutableMap.of("success", "Status updated"));
    assertThat(contentAsString(result)).doesNotContain(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);

    String originalFrenchStatusText =
        WITH_STATUS_TRANSLATIONS.localizedStatusText().get(Locale.FRENCH);
    StatusDefinitions.Status expectedStatus =
        StatusDefinitions.Status.builder()
            .setStatusText("Foo")
            .setDefaultStatus(Optional.of(false))
            .setLocalizedStatusText(
                LocalizedStrings.create(
                    ImmutableMap.of(Locale.US, "Foo", Locale.FRENCH, originalFrenchStatusText)))
            // Explicitly not calling setLocalizedEmailBodyText since we expect
            // the value to be cleared entirely when an empty English email is
            // provided from the request.
            .build();

    // Load the updated program and ensure the status is present.
    assertThat(programService.getProgramDefinition(program.id).statusDefinitions().getStatuses())
        .isEqualTo(ImmutableList.of(APPROVED_STATUS, REJECTED_STATUS, expectedStatus));
  }

  @Test
  public void update_emptyStatusParam() throws ProgramNotFoundException {
    Program program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();

    Result result =
        makeCreateOrUpdateRequest(
            program.id,
            ImmutableMap.of(
                "configuredStatusText", APPROVED_STATUS.statusText(),
                "statusText", "",
                "emailBody", "Some email body"));

    assertThat(result.status()).isEqualTo(OK);
    assertThat(Helpers.contentAsString(result)).contains("This field is required");
    assertThat(contentAsString(result)).containsOnlyOnce(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);

    assertThat(programService.getProgramDefinition(program.id).statusDefinitions().getStatuses())
        .isEqualTo(ORIGINAL_STATUSES);
  }

  @Test
  public void update_editStatusNameAlreadyExists() throws ProgramNotFoundException {
    Program program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();

    Result result =
        makeCreateOrUpdateRequest(
            program.id,
            ImmutableMap.of(
                "configuredStatusText", APPROVED_STATUS.statusText(),
                "statusText", REJECTED_STATUS.statusText(),
                "emailBody", "Some email body"));

    assertThat(result.status()).isEqualTo(OK);
    assertThat(Helpers.contentAsString(result))
        .contains("A status with name Rejected already exists");
    assertThat(contentAsString(result)).containsOnlyOnce(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);

    assertThat(programService.getProgramDefinition(program.id).statusDefinitions().getStatuses())
        .isEqualTo(ORIGINAL_STATUSES);
  }

  @Test
  public void update_editUnrecognizedStatusName() throws ProgramNotFoundException {
    Program program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();

    Result result =
        makeCreateOrUpdateRequest(
            program.id,
            ImmutableMap.of(
                "configuredStatusText", "non-existent-original-status",
                "statusText", "Updated status",
                "emailBody", "Some email body"));

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().data().getOrDefault("error", ""))
        .contains("The status being edited no longer exists");
    assertThat(contentAsString(result)).doesNotContain(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);

    assertThat(programService.getProgramDefinition(program.id).statusDefinitions().getStatuses())
        .isEqualTo(ORIGINAL_STATUSES);
  }

  @Test
  public void update_createStatusNameAlreadyExists() throws ProgramNotFoundException {
    Program program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();

    Result result =
        makeCreateOrUpdateRequest(
            program.id,
            ImmutableMap.of(
                "configuredStatusText", "",
                "statusText", REJECTED_STATUS.statusText(),
                "emailBody", "Some email body"));

    assertThat(result.status()).isEqualTo(OK);
    assertThat(Helpers.contentAsString(result))
        .contains("A status with name Rejected already exists");
    assertThat(contentAsString(result)).containsOnlyOnce(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);

    assertThat(programService.getProgramDefinition(program.id).statusDefinitions().getStatuses())
        .isEqualTo(ORIGINAL_STATUSES);
  }

  @Test
  @Parameters({"GET", "POST"})
  public void index_missingProgram(String httpMethod) {
    assertThatThrownBy(
            () ->
                controller.index(
                    addCSRFToken(fakeRequest().method(httpMethod)).build(), Long.MAX_VALUE))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  @Parameters({"GET", "POST"})
  public void index_nonDraftProgram(String httpMethod) {
    Program program = ProgramBuilder.newActiveProgram("test name", "test description").build();

    assertThatThrownBy(
            () ->
                controller.index(
                    addCSRFToken(fakeRequest().method(httpMethod)).build(), program.id))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void delete_ok() throws ProgramNotFoundException {
    Program program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();

    Result result =
        makeDeleteRequest(
            program.id, ImmutableMap.of("deleteStatusText", REJECTED_STATUS.statusText()));

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().data())
        .containsExactlyEntriesOf(ImmutableMap.of("success", "Status deleted"));
    assertThat(contentAsString(result)).doesNotContain(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);

    // Load the updated program and ensure status the status is present.
    ProgramDefinition updatedProgram = programService.getProgramDefinition(program.id);
    assertThat(updatedProgram.statusDefinitions().getStatuses())
        .isEqualTo(ImmutableList.of(APPROVED_STATUS, WITH_STATUS_TRANSLATIONS));
  }

  @Test
  public void delete_unrecognizedStatusParam() throws ProgramNotFoundException {
    Program program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();

    Result result = makeDeleteRequest(program.id, ImmutableMap.of("deleteStatusText", "oldStatus"));

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().data()).containsKey("error");
    assertThat(result.flash().data().getOrDefault("error", ""))
        .contains("The status being deleted no longer exists");
    assertThat(contentAsString(result)).doesNotContain(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);

    // Load the updated program and ensure statuses weren't updated.
    ProgramDefinition updatedProgram = programService.getProgramDefinition(program.id);
    assertThat(updatedProgram.statusDefinitions().getStatuses()).isEqualTo(ORIGINAL_STATUSES);
  }

  @Test
  public void delete_missingStatusParam() throws ProgramNotFoundException {
    Program program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();

    Result result = makeDeleteRequest(program.id, ImmutableMap.of("deleteStatusText", ""));

    assertThat(result.status()).isEqualTo(BAD_REQUEST);

    // Load the updated program and ensure statuses weren't updated.
    ProgramDefinition updatedProgram = programService.getProgramDefinition(program.id);
    assertThat(updatedProgram.statusDefinitions().getStatuses()).isEqualTo(ORIGINAL_STATUSES);
  }

  @Test
  public void delete_missingProgram() {
    assertThatThrownBy(() -> makeDeleteRequest(Long.MAX_VALUE, ImmutableMap.of()))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void delete_nonDraftProgram() {
    Program program = ProgramBuilder.newActiveProgram("test name", "test description").build();

    assertThatThrownBy(() -> makeDeleteRequest(program.id, ImmutableMap.of()))
        .isInstanceOf(NotChangeableException.class);
  }

  private Result makeCreateOrUpdateRequest(Long programId, ImmutableMap<String, String> formData)
      throws ProgramNotFoundException {
    return controller.createOrUpdate(
        addCSRFToken(fakeRequest().method("POST").bodyForm(formData)).build(), programId);
  }

  private Result makeDeleteRequest(Long programId, ImmutableMap<String, String> formData)
      throws ProgramNotFoundException {
    return controller.delete(
        addCSRFToken(fakeRequest().method("POST").bodyForm(formData)).build(), programId);
  }
}
