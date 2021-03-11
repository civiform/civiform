package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.inject.Bindings.bind;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.stubMessagesApi;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import play.api.i18n.DefaultLangs;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.mvc.Result;
import repository.WithPostgresContainer;
import services.program.ProgramDefinition;
import support.TestConstants;

public class ApplicantProgramBlocksControllerTest extends WithPostgresContainer {

  private ApplicantProgramBlocksController subject;
  private ProgramDefinition program;
  private Applicant applicant;

  @Before
  public void setUp() {
    subject = instanceOf(ApplicantProgramBlocksController.class);
    program = resourceCreator().insertProgramWithOneBlock("Test program");
    applicant = resourceCreator().insertApplicant();
  }

  @Test
  public void edit_toAnExistingBlock_rendersTheBlock() {
    Http.Request request =
        addCSRFToken(
                fakeRequest(
                    routes.ApplicantProgramBlocksController.edit(applicant.id, program.id(), 1L)))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id(), 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void edit_toABlockThatDoesNotExist_returns404() {
    Http.Request request =
        fakeRequest(routes.ApplicantProgramBlocksController.edit(applicant.id, program.id(), 2L))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id(), 2L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_withMessages_returnsCorrectButtonText() {
    // Stub the MessagesApi with fake translations.
    Langs langs = new Langs(new DefaultLangs());
    Map<String, String> translations = Collections.singletonMap("button.nextBlock", "Different");
    Map<String, Map<String, String>> messageMap =
        ImmutableMap.of(Lang.defaultLang().code(), translations);
    MessagesApi messagesApi = stubMessagesApi(messageMap, langs);

    // Override the injected MessagesApi in the controller.
    subject =
        new GuiceApplicationBuilder()
            .configure(TestConstants.TEST_DATABASE_CONFIG)
            .overrides(bind(MessagesApi.class).toInstance(messagesApi))
            .build()
            .injector()
            .instanceOf(ApplicantProgramBlocksController.class);

    Http.Request request =
        addCSRFToken(
                fakeRequest(
                        routes.ApplicantProgramBlocksController.edit(
                            applicant.id, program.id(), 1L))
                    .langCookie(Lang.defaultLang(), messagesApi))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id(), 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Different");
  }
}
