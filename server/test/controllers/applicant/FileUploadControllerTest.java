package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.OK;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import controllers.WithMockedProfiles;
import models.ApplicantModel;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import repository.StoredFileRepository;
import repository.VersionRepository;
import services.applicant.ApplicantService;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.questiontypes.FileUploadQuestionPartialView;

public class FileUploadControllerTest extends WithMockedProfiles {

  private FileUploadController subject;
  private ApplicantModel applicant;
  private SettingsManifest settingsManifest;

  @Before
  public void setUp() {
    resetDatabase();
    applicant = createApplicantWithMockedProfile();

    settingsManifest = mock(SettingsManifest.class);
    subject =
        new FileUploadController(
            instanceOf(ApplicantService.class),
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(FormFactory.class),
            instanceOf(StoredFileRepository.class),
            instanceOf(ProfileUtils.class),
            instanceOf(VersionRepository.class),
            settingsManifest,
            instanceOf(FileUploadQuestionPartialView.class));
  }

  @Test
  public void hxSelectFileForUpload_generatesUuidFileKeyAndStoresOriginalName() {
    var fileUploadQuestion = testQuestionBank().fileUploadApplicantFile();
    ProgramModel program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(fileUploadQuestion)
            .build();

    RequestBuilder requestBuilder = fakeRequestBuilder();
    Request request =
        requestBuilder
            .bodyMultipart(
                java.util.Map.of(),
                java.util.List.of(
                    new play.mvc.Http.MultipartFormData.FilePart<>(
                        "file", "my-document.pdf", "application/pdf", "applicant-test-file-key")))
            .build();
    when(settingsManifest.getFileUploadQuestionImprovementsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .hxSelectFileForUpload(
                request,
                program.id,
                /* blockId= */ "1",
                fileUploadQuestion.getQuestionDefinition().getId())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);

    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).contains("my-document.pdf");
    assertThat(applicantData).contains("applicant-");
    assertThat(applicantData).contains(".pdf");
    assertThat(applicantData).doesNotContain("my-document.pdf\",\"file_key_list");
  }
}
