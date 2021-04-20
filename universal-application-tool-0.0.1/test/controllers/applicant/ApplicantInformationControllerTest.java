package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import repository.ApplicantRepository;

public class ApplicantInformationControllerTest extends WithMockedApplicantProfiles {

  private Applicant currentApplicant;
  private ApplicantRepository applicantRepository;
  private ApplicantInformationController controller;

  @Before
  public void setup() {
    resourceCreator().clearDatabase();
    controller = instanceOf(ApplicantInformationController.class);
    applicantRepository = instanceOf(ApplicantRepository.class);
    currentApplicant = createApplicantWithMockedProfile();
  }

  @Test
  public void edit_differentApplicant_returnsUnauthorizedResult() {
    Result result =
        controller
            .edit(fakeRequest().build(), currentApplicant.id + 1)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void update_differentApplicant_returnsUnauthorizedResult() {
    Result result =
        controller
            .update(fakeRequest().build(), currentApplicant.id + 1)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void edit_usesHumanReadableLanguagesInsteadOfIsoTags() {
    Result result =
        controller
            .edit(addCSRFToken(fakeRequest()).build(), currentApplicant.id)
            .toCompletableFuture()
            .join();
    assertThat(contentAsString(result)).contains("English");
    assertThat(contentAsString(result)).contains("español");
  }

  @Test
  public void update_redirectsToProgramIndex_withNonEnglishLocale() {
    Http.Request request =
        addCSRFToken(
                fakeRequest(routes.ApplicantInformationController.update(currentApplicant.id))
                    .bodyForm(ImmutableMap.of("locale", "es-US")))
            .build();

    Result result = controller.update(request, currentApplicant.id).toCompletableFuture().join();

    currentApplicant =
        applicantRepository.lookupApplicant(currentApplicant.id).toCompletableFuture().join().get();
    assertThat(currentApplicant.getApplicantData().preferredLocale())
        .isEqualTo(Locale.forLanguageTag("es-US"));
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.cookie("PLAY_LANG").get().value()).isEqualTo("es-US");
  }

  @Test
  public void update_ignoresExistingLangCookie() {
    Http.Request request =
        addCSRFToken(
                fakeRequest(routes.ApplicantInformationController.update(currentApplicant.id))
                    .bodyForm(ImmutableMap.of("locale", "es-US")))
            .langCookie(Locale.US, stubMessagesApi())
            .build();

    Result result = controller.update(request, currentApplicant.id).toCompletableFuture().join();

    currentApplicant =
        applicantRepository.lookupApplicant(currentApplicant.id).toCompletableFuture().join().get();
    assertThat(currentApplicant.getApplicantData().preferredLocale())
        .isEqualTo(Locale.forLanguageTag("es-US"));
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.cookie("PLAY_LANG").get().value()).isEqualTo("es-US");
  }
}
