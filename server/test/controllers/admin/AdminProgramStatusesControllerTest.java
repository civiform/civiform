package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.mvc.Result;
import play.test.Helpers;
import repository.ApplicationStatusesRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.statuses.StatusDefinitions;
import support.ProgramBuilder;
import views.style.ReferenceClasses;

@RunWith(JUnitParamsRunner.class)
public class AdminProgramStatusesControllerTest extends ResetPostgres {

  private ProgramService programService;
  private AdminProgramStatusesController controller;
  private ApplicationStatusesRepository repo;

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
    repo = instanceOf(ApplicationStatusesRepository.class);
  }

  @Test
  public void index_ok_get() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name", "test description").build();

    Result result = controller.index(fakeRequestBuilder().method("GET").build(), program.id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("No statuses have been created yet");
    assertThat(contentAsString(result)).doesNotContain(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);
  }

  @Test
  public void index_ok_noEmailOrDefaultForStatus() throws ProgramNotFoundException {
    ProgramModel program =
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

    Result result = controller.index(fakeRequestBuilder().method("GET").build(), program.id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Status with no email");
    assertThat(contentAsString(result)).doesNotContain("Applicant notification email added");
    assertThat(contentAsString(result)).doesNotContain("Default status");
    assertThat(contentAsString(result)).doesNotContain(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);
  }

  @Test
  public void update_createNewStatus() throws ProgramNotFoundException {
    ProgramModel program =
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

    // Load the updated program and ensure the status is present.
    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(repo.lookupActiveStatusDefinitions(updatedProgram.adminName()).getStatuses())
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
  public void update_createNewStatusAsDefault() throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();

    Result result =
        makeCreateOrUpdateRequest(
            program.id,
            ImmutableMap.of(
                "configuredStatusText", "",
                "statusText", "foo",
                "emailBody", "some email content",
                "defaultStatusCheckbox", "on"));

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().data())
        .containsExactlyEntriesOf(
            ImmutableMap.of("success", "foo has been updated to the default status"));
    assertThat(contentAsString(result)).doesNotContain(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);

    // Load the updated program and ensure the status is present.
    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(repo.lookupActiveStatusDefinitions(updatedProgram.adminName()).getStatuses())
        .isEqualTo(
            ImmutableList.of(
                APPROVED_STATUS,
                REJECTED_STATUS,
                WITH_STATUS_TRANSLATIONS,
                StatusDefinitions.Status.builder()
                    .setStatusText("foo")
                    .setDefaultStatus(Optional.of(true))
                    .setLocalizedStatusText(LocalizedStrings.withDefaultValue("foo"))
                    .setLocalizedEmailBodyText(
                        Optional.of(LocalizedStrings.withDefaultValue("some email content")))
                    .build()));

    // Add another one as default and verify the previous default is no longer default
    Result newResult =
        makeCreateOrUpdateRequest(
            program.id,
            ImmutableMap.of(
                "configuredStatusText",
                "",
                "statusText",
                "bar",
                "emailBody",
                "more email content",
                "defaultStatusCheckbox",
                "on"));
    assertThat(newResult.status()).isEqualTo(SEE_OTHER);
    assertThat(newResult.flash().data())
        .containsExactlyEntriesOf(
            ImmutableMap.of("success", "bar has been updated to the default status"));
    assertThat(contentAsString(result)).doesNotContain(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);
    ProgramDefinition newlyUpdatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(repo.lookupActiveStatusDefinitions(newlyUpdatedProgram.adminName()).getStatuses())
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
                    .build(),
                StatusDefinitions.Status.builder()
                    .setStatusText("bar")
                    .setDefaultStatus(Optional.of(true))
                    .setLocalizedStatusText(LocalizedStrings.withDefaultValue("bar"))
                    .setLocalizedEmailBodyText(
                        Optional.of(LocalizedStrings.withDefaultValue("more email content")))
                    .build()));
  }

  @Test
  public void update_editExistingStatus() throws ProgramNotFoundException {
    ProgramModel program =
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

    // Load the updated program and ensure the status is present.
    assertThat(
            repo.lookupActiveStatusDefinitions(
                    programService.getFullProgramDefinition(program.id).adminName())
                .getStatuses())
        .isEqualTo(ImmutableList.of(expectedStatus, REJECTED_STATUS, WITH_STATUS_TRANSLATIONS));
  }

  @Test
  public void update_editExistingStatusPreservesNonDefaultLocaleTranslations()
      throws ProgramNotFoundException, TranslationNotFoundException {
    ProgramModel program =
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
    assertThat(
            repo.lookupActiveStatusDefinitions(
                    programService.getFullProgramDefinition(program.id).adminName())
                .getStatuses())
        .isEqualTo(ImmutableList.of(APPROVED_STATUS, REJECTED_STATUS, expectedStatus));
  }

  @Test
  public void update_editExistingStatusClearEmailClearsTranslatedEmailContent()
      throws ProgramNotFoundException, TranslationNotFoundException {
    ProgramModel program =
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
    assertThat(
            repo.lookupActiveStatusDefinitions(
                    programService.getFullProgramDefinition(program.id).adminName())
                .getStatuses())
        .isEqualTo(ImmutableList.of(APPROVED_STATUS, REJECTED_STATUS, expectedStatus));
  }

  @Test
  public void update_editExistingStatusMakeDefault() throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();

    Result result1 =
        makeCreateOrUpdateRequest(
            program.id,
            ImmutableMap.of(
                "configuredStatusText", "Approved",
                "statusText", "Foo",
                "emailBody", "Updated email content",
                "defaultStatusCheckbox", "on"));

    assertThat(result1.status()).isEqualTo(SEE_OTHER);
    assertThat(result1.flash().data())
        .containsExactlyEntriesOf(
            ImmutableMap.of("success", "Foo has been updated to the default status"));
    assertThat(contentAsString(result1)).doesNotContain(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);

    StatusDefinitions.Status expectedStatus =
        StatusDefinitions.Status.builder()
            .setStatusText("Foo")
            .setDefaultStatus(Optional.of(true))
            .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Foo"))
            .setLocalizedEmailBodyText(
                Optional.of(LocalizedStrings.withDefaultValue("Updated email content")))
            .build();

    // Load the updated program and ensure the status is present.
    assertThat(
            repo.lookupActiveStatusDefinitions(
                    programService.getFullProgramDefinition(program.id).adminName())
                .getStatuses())
        .isEqualTo(ImmutableList.of(expectedStatus, REJECTED_STATUS, WITH_STATUS_TRANSLATIONS));

    Result result2 =
        makeCreateOrUpdateRequest(
            program.id,
            ImmutableMap.of(
                "configuredStatusText", "Rejected",
                "statusText", "Bar",
                "emailBody", "Bar email",
                "defaultStatusCheckbox", "on"));

    assertThat(result2.status()).isEqualTo(SEE_OTHER);
    assertThat(result2.flash().data())
        .containsExactlyEntriesOf(
            ImmutableMap.of("success", "Bar has been updated to the default status"));
    assertThat(contentAsString(result2)).doesNotContain(ReferenceClasses.MODAL_DISPLAY_ON_LOAD);

    StatusDefinitions.Status foo =
        StatusDefinitions.Status.builder()
            .setStatusText("Foo")
            .setDefaultStatus(Optional.of(false))
            .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Foo"))
            .setLocalizedEmailBodyText(
                Optional.of(LocalizedStrings.withDefaultValue("Updated email content")))
            .build();
    StatusDefinitions.Status bar =
        StatusDefinitions.Status.builder()
            .setStatusText("Bar")
            .setDefaultStatus(Optional.of(true))
            .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Bar"))
            .setLocalizedEmailBodyText(Optional.of(LocalizedStrings.withDefaultValue("Bar email")))
            .build();

    // Load the updated program and ensure the status is present.
    assertThat(
            repo.lookupActiveStatusDefinitions(
                    programService.getFullProgramDefinition(program.id).adminName())
                .getStatuses())
        .isEqualTo(ImmutableList.of(foo, bar, WITH_STATUS_TRANSLATIONS));
  }

  @Test
  public void update_emptyStatusParam() throws ProgramNotFoundException {
    ProgramModel program =
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

    assertThat(
            repo.lookupActiveStatusDefinitions(
                    programService.getFullProgramDefinition(program.id).adminName())
                .getStatuses())
        .isEqualTo(ORIGINAL_STATUSES);
  }

  @Test
  public void update_editStatusNameAlreadyExists() throws ProgramNotFoundException {
    ProgramModel program =
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

    assertThat(
            repo.lookupActiveStatusDefinitions(
                    programService.getFullProgramDefinition(program.id).adminName())
                .getStatuses())
        .isEqualTo(ORIGINAL_STATUSES);
  }

  @Test
  public void update_editUnrecognizedStatusName() throws ProgramNotFoundException {
    ProgramModel program =
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

    assertThat(
            repo.lookupActiveStatusDefinitions(
                    programService.getFullProgramDefinition(program.id).adminName())
                .getStatuses())
        .isEqualTo(ORIGINAL_STATUSES);
  }

  @Test
  public void update_createStatusNameAlreadyExists() throws ProgramNotFoundException {
    ProgramModel program =
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

    assertThat(
            repo.lookupActiveStatusDefinitions(
                    programService.getFullProgramDefinition(program.id).adminName())
                .getStatuses())
        .isEqualTo(ORIGINAL_STATUSES);
  }

  @Test
  @Parameters({"GET", "POST"})
  public void index_missingProgram(String httpMethod) {
    assertThatThrownBy(
            () -> controller.index(fakeRequestBuilder().method(httpMethod).build(), Long.MAX_VALUE))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  @Parameters({"GET", "POST"})
  public void index_nonDraftProgram(String httpMethod) {
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();

    assertThatThrownBy(
            () -> controller.index(fakeRequestBuilder().method(httpMethod).build(), program.id))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void delete_ok() throws ProgramNotFoundException {
    ProgramModel program =
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

    // Load the updated program and ensure the status is present.
    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(repo.lookupActiveStatusDefinitions(updatedProgram.adminName()).getStatuses())
        .isEqualTo(ImmutableList.of(APPROVED_STATUS, WITH_STATUS_TRANSLATIONS));
  }

  @Test
  public void delete_unrecognizedStatusParam() throws ProgramNotFoundException {
    ProgramModel program =
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
    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(repo.lookupActiveStatusDefinitions(updatedProgram.adminName()).getStatuses())
        .isEqualTo(ORIGINAL_STATUSES);
  }

  @Test
  public void delete_missingStatusParam() throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();

    Result result = makeDeleteRequest(program.id, ImmutableMap.of("deleteStatusText", ""));

    assertThat(result.status()).isEqualTo(BAD_REQUEST);

    // Load the updated program and ensure statuses weren't updated.
    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(repo.lookupActiveStatusDefinitions(updatedProgram.adminName()).getStatuses())
        .isEqualTo(ORIGINAL_STATUSES);
  }

  @Test
  public void delete_missingProgram() {
    assertThatThrownBy(() -> makeDeleteRequest(Long.MAX_VALUE, ImmutableMap.of()))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void delete_nonDraftProgram() {
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();

    assertThatThrownBy(() -> makeDeleteRequest(program.id, ImmutableMap.of()))
        .isInstanceOf(NotChangeableException.class);
  }

  private Result makeCreateOrUpdateRequest(Long programId, ImmutableMap<String, String> formData)
      throws ProgramNotFoundException {
    return controller.createOrUpdate(
        fakeRequestBuilder().method("POST").bodyForm(formData).build(), programId);
  }

  private Result makeDeleteRequest(Long programId, ImmutableMap<String, String> formData)
      throws ProgramNotFoundException {
    return controller.delete(
        fakeRequestBuilder().method("POST").bodyForm(formData).build(), programId);
  }
}
