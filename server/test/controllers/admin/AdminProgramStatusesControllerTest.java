package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.data.FormFactory;
import play.mvc.Result;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.StatusDefinitions;
import support.ProgramBuilder;
import views.admin.programs.ProgramStatusesView;

@RunWith(JUnitParamsRunner.class)
public class AdminProgramStatusesControllerTest extends ResetPostgres {

  private ProgramService programService;
  private AdminProgramStatusesController controller;

  @Before
  public void setup() {
    programService = instanceOf(ProgramService.class);
    controller = instanceOf(AdminProgramStatusesController.class);
  }

  @Test
  @Parameters({"GET", "POST"})
  public void index_flagDisabled(String httpMethod) throws ProgramNotFoundException {
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();

    // Initialize the controller explicitly to override status tracking enablement.
    controller =
        new AdminProgramStatusesController(
            instanceOf(ProgramService.class),
            instanceOf(ProgramStatusesView.class),
            instanceOf(RequestChecker.class),
            instanceOf(FormFactory.class),
            /* statusTrackingEnabled= */ false);

    Result result =
        controller.index(addCSRFToken(fakeRequest().method(httpMethod)).build(), program.id);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void index_ok_get() throws ProgramNotFoundException {
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();

    Result result = controller.index(addCSRFToken(fakeRequest().method("GET")).build(), program.id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("No statuses have been created yet");
  }

  @Test
  public void index_createNewStatus() throws ProgramNotFoundException {
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();

    Result result =
        controller.index(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(
                            ImmutableMap.of(
                                "originalStatusText", "",
                                "statusText", "foo",
                                "emailBody", "some email content")))
                .build(),
            program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().data())
        .containsExactlyEntriesOf(ImmutableMap.of("success", "Status created"));

    // Load the updated program and ensure status the status is present.
    ProgramDefinition updatedProgram = programService.getProgramDefinition(program.id);
    assertThat(updatedProgram.statusDefinitions().getStatuses())
        .isEqualTo(
            ImmutableList.of(
                StatusDefinitions.Status.builder()
                    .setStatusText("foo")
                    .setLocalizedStatusText(LocalizedStrings.withDefaultValue("foo"))
                    .setEmailBodyText("some email content")
                    .setLocalizedEmailBodyText(
                        LocalizedStrings.withDefaultValue("some email content"))
                    .build()));
  }

  private static final StatusDefinitions.Status APPROVED_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Approved")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Approved"))
          .setEmailBodyText("Approved email body")
          .setLocalizedEmailBodyText(LocalizedStrings.withDefaultValue("Approved email body"))
          .build();

  private static final StatusDefinitions.Status REJECTED_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Rejected")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Rejected"))
          .setEmailBodyText("Rejected email body")
          .setLocalizedEmailBodyText(LocalizedStrings.withDefaultValue("Rejected email body"))
          .build();

  private static final ImmutableList<StatusDefinitions.Status> ORIGINAL_STATUSES =
      ImmutableList.of(APPROVED_STATUS, REJECTED_STATUS);

  @Test
  public void index_editExistingStatus() throws ProgramNotFoundException {
    Program program =
        ProgramBuilder.newDraftProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();

    Result result =
        controller.index(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(
                            ImmutableMap.of(
                                "originalStatusText", "Approved",
                                "statusText", "Foo",
                                "emailBody", "Updated email content")))
                .build(),
            program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().data())
        .containsExactlyEntriesOf(ImmutableMap.of("success", "Status updated"));

    // Load the updated program and ensure status the status is present.
    ProgramDefinition updatedProgram = programService.getProgramDefinition(program.id);
    assertThat(updatedProgram.statusDefinitions().getStatuses())
        .isEqualTo(
            ImmutableList.of(
                StatusDefinitions.Status.builder()
                    .setStatusText("Foo")
                    .setLocalizedStatusText(APPROVED_STATUS.localizedStatusText())
                    .setEmailBodyText("Updated email content")
                    .setLocalizedEmailBodyText(APPROVED_STATUS.localizedEmailBodyText().get())
                    .build(),
                REJECTED_STATUS));
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
        controller.delete(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(
                            ImmutableMap.of("deleteStatusText", REJECTED_STATUS.statusText())))
                .build(),
            program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().data())
        .containsExactlyEntriesOf(ImmutableMap.of("success", "Status deleted"));

    // Load the updated program and ensure status the status is present.
    ProgramDefinition updatedProgram = programService.getProgramDefinition(program.id);
    assertThat(updatedProgram.statusDefinitions().getStatuses())
        .isEqualTo(ImmutableList.of(APPROVED_STATUS));
  }

  @Test
  public void delete_missingProgram() {
    assertThatThrownBy(
            () ->
                controller.index(
                    addCSRFToken(fakeRequest().method("POST")).build(), Long.MAX_VALUE))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void delete_nonDraftProgram() {
    Program program = ProgramBuilder.newActiveProgram("test name", "test description").build();

    assertThatThrownBy(
            () -> controller.index(addCSRFToken(fakeRequest().method("POST")).build(), program.id))
        .isInstanceOf(NotChangeableException.class);
  }
}
