package views.applicant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS;
import static views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS_WITH_MODAL_PREVIOUS;
import static views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS_WITH_MODAL_REVIEW;
import static views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import controllers.applicant.ApplicantRoutes;
import java.util.Set;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import repository.AccountRepository;
import repository.DatabaseExecutionContext;
import repository.ResetPostgres;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.Block;
import services.cloud.ApplicantStorageClient;
import services.settings.SettingsManifest;
import views.ApplicationBaseView;
import views.questiontypes.ApplicantQuestionRendererParams;

public class EditOrDiscardAnswersModalCreatorTest extends ResetPostgres {
    private final EditOrDiscardAnswersModalCreator modalCreator =
            new EditOrDiscardAnswersModalCreator();

    @Test
    public void createModal_modeDisplayErrors_throws() {
        ApplicationBaseView.Params params = makeParamsWithMode(DISPLAY_ERRORS);
        assertThatThrownBy(() -> modalCreator.createModal(params))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void createModal_modeHideErrors_throws() {
        ApplicationBaseView.Params params = makeParamsWithMode(HIDE_ERRORS);
        assertThatThrownBy(() -> modalCreator.createModal(params))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void createModal_modeDisplayErrorsWithModalReview_ok() {
        ApplicationBaseView.Params params = makeParamsWithMode(DISPLAY_ERRORS_WITH_MODAL_REVIEW);
        modalCreator.createModal(params);
        // No assert, just need no crash
    }

    @Test
    public void createModal_modeDisplayErrorsWithModalPrevious_ok() {
        ApplicationBaseView.Params params = makeParamsWithMode(DISPLAY_ERRORS_WITH_MODAL_PREVIOUS);
        modalCreator.createModal(params);
        // No assert, just need no crash
    }

    private ApplicationBaseView.Params makeParamsWithMode(
            ApplicantQuestionRendererParams.ErrorDisplayMode errorDisplayMode) {
        return ApplicationBaseView.Params.builder()
                .setErrorDisplayMode(errorDisplayMode)
                .setRequest(fakeRequest().build())
                .setMessages(
                        fakeApplication()
                                .injector()
                                .instanceOf(MessagesApi.class)
                                .preferred(Set.of(Lang.defaultLang())))
                .setApplicantId(1L)
                .setProgramTitle("Title")
                .setProgramId(1L)
                .setBlock(mock(Block.class))
                .setInReview(false)
                .setBlockIndex(0)
                .setTotalBlockCount(1)
                .setApplicantPersonalInfo(mock(ApplicantPersonalInfo.class))
                .setPreferredLanguageSupported(true)
                .setApplicantStorageClient(mock(ApplicantStorageClient.class))
                .setBaseUrl("baseUrl.com")
                .setApplicantRoutes(new ApplicantRoutes())
                .setProfile(
                        new CiviFormProfile(
                                instanceOf(DatabaseExecutionContext.class),
                                instanceOf(HttpExecutionContext.class),
                                instanceOf(CiviFormProfileData.class),
                                instanceOf(SettingsManifest.class),
                                instanceOf(AccountRepository.class)))
                .build();
    }
}