package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequestBuilder;
import static support.FakeRequestBuilder.fakeRequestNew;
import static support.cloud.FakePublicStorageClient.FAKE_BUCKET_NAME;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import support.ProgramBuilder;
import support.cloud.FakePublicStorageClient;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramImageView;

@RunWith(JUnitParamsRunner.class)
public class AdminProgramImageControllerTest extends ResetPostgres {
  private static final String VALID_FILE_KEY = "program-summary-image/program-1/myImage.png";

  private ProgramService programService;
  private AdminProgramImageController controller;

  @Before
  public void setup() {
    programService = instanceOf(ProgramService.class);
    controller =
        new AdminProgramImageController(
            new FakePublicStorageClient(),
            programService,
            instanceOf(ProgramImageView.class),
            instanceOf(RequestChecker.class),
            instanceOf(FormFactory.class),
            instanceOf(ProfileUtils.class),
            instanceOf(VersionRepository.class));
  }

  @Test
  public void index_ok_get() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    Result result =
        controller.index(
            addCSRFToken(fakeRequestBuilder().method("GET")).build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Image upload");
  }

  @Test
  public void index_programNotDraft_throws() {
    ProgramModel program = ProgramBuilder.newActiveProgram().build();

    assertThatThrownBy(
            () ->
                controller.index(
                    addCSRFToken(fakeRequestBuilder().method("GET")).build(),
                    program.id,
                    ProgramEditStatus.CREATION.name()))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void index_missingProgram_throws() {
    assertThatThrownBy(
            () ->
                controller.index(
                    addCSRFToken(fakeRequestBuilder().method("GET")).build(),
                    Long.MAX_VALUE,
                    ProgramEditStatus.CREATION.name()))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void index_programHasDescription_displayed() throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(Locale.US, "fake summary description"))
            .build();

    Result result =
        controller.index(
            addCSRFToken(fakeRequestBuilder().method("GET")).build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("fake summary description");
  }

  @Test
  public void updateDescription_programNotDraft_throws() {
    ProgramModel program = ProgramBuilder.newActiveProgram().build();

    assertThatThrownBy(
            () ->
                controller.updateDescription(
                    addCSRFToken(fakeRequestBuilder().method("POST")).build(),
                    program.id,
                    ProgramEditStatus.CREATION.name()))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void updateDescription_missingProgram_throws() {
    assertThatThrownBy(
            () ->
                controller.updateDescription(
                    addCSRFToken(fakeRequestBuilder().method("POST")).build(),
                    Long.MAX_VALUE,
                    ProgramEditStatus.CREATION.name()))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void updateDescription_createsNewDescription()
      throws ProgramNotFoundException, TranslationNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequestBuilder()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "fake description")))
                .build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(updatedProgram.localizedSummaryImageDescription().isPresent()).isTrue();
    assertThat(updatedProgram.localizedSummaryImageDescription().get().get(Locale.US))
        .isEqualTo("fake description");
  }

  @Test
  public void updateDescription_editsExistingDescription()
      throws ProgramNotFoundException, TranslationNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(Locale.US, "first description"))
            .build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequestBuilder()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "second description")))
                .build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(updatedProgram.localizedSummaryImageDescription().isPresent()).isTrue();
    assertThat(updatedProgram.localizedSummaryImageDescription().get().get(Locale.US))
        .isEqualTo("second description");
  }

  @Test
  public void updateDescription_editExisting_preservesNonDefaultLocaleTranslations()
      throws ProgramNotFoundException, TranslationNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(
                    Locale.US,
                    "US description",
                    Locale.FRENCH,
                    "French description",
                    Locale.ITALIAN,
                    "Italian description"))
            .build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequestBuilder()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "new US description")))
                .build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(updatedProgram.localizedSummaryImageDescription().isPresent()).isTrue();
    assertThat(updatedProgram.localizedSummaryImageDescription().get().get(Locale.US))
        .isEqualTo("new US description");
    assertThat(updatedProgram.localizedSummaryImageDescription().get().get(Locale.FRENCH))
        .isEqualTo("French description");
    assertThat(updatedProgram.localizedSummaryImageDescription().get().get(Locale.ITALIAN))
        .isEqualTo("Italian description");
  }

  @Test
  public void updateDescription_empty_noImageFile_removesDescription()
      throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(Locale.US, "original description"))
            .build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequestBuilder()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "")))
                .build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(updatedProgram.localizedSummaryImageDescription().isEmpty()).isTrue();
  }

  @Test
  public void updateDescription_empty_hasImageFile_descriptionNotRemoved()
      throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(Locale.US, "original description"))
            .build();
    setValidFileKeyOnProgram(program);

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequestBuilder()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "")))
                .build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(updatedProgram.localizedSummaryImageDescription().isPresent()).isTrue();
    assertThat(updatedProgram.localizedSummaryImageDescription().get().getDefault())
        .isEqualTo("original description");
  }

  @Test
  public void updateDescription_blank_hasImageFile_descriptionNotRemoved()
      throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(Locale.US, "original description"))
            .build();
    setValidFileKeyOnProgram(program);

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequestBuilder()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "    ")))
                .build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(updatedProgram.localizedSummaryImageDescription().isPresent()).isTrue();
    assertThat(updatedProgram.localizedSummaryImageDescription().get().getDefault())
        .isEqualTo("original description");
  }

  @Test
  public void updateDescription_blank_noImageFile_removesDescription()
      throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(Locale.US, "original description"))
            .build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequestBuilder()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "    ")))
                .build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(updatedProgram.localizedSummaryImageDescription().isEmpty()).isTrue();
  }

  @Test
  public void updateDescription_empty_removesNonDefaultLocaleTranslations()
      throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(
                    Locale.US,
                    "US description",
                    Locale.FRENCH,
                    "French description",
                    Locale.ITALIAN,
                    "Italian description"))
            .build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequestBuilder()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "")))
                .build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(updatedProgram.localizedSummaryImageDescription().isEmpty()).isTrue();
  }

  @Test
  public void updateDescription_nonEmpty_toastsSuccess() {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequestBuilder()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "fake description")))
                .build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertThat(result.flash().data()).containsOnlyKeys("success");
    assertThat(result.flash().data().get("success")).contains("set to fake description");
  }

  @Test
  public void updateDescription_empty_noImageFile_toastsSuccess() {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(Locale.US, "original description"))
            .build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequestBuilder()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "")))
                .build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertThat(result.flash().data()).containsOnlyKeys("success");
    assertThat(result.flash().data().get("success")).contains("removed");
  }

  @Test
  public void updateDescription_empty_hasImageFile_toastsError() throws ProgramNotFoundException {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("test name")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(Locale.US, "original description"))
            .build();
    setValidFileKeyOnProgram(program);

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequestBuilder()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "")))
                .build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertThat(result.flash().data()).containsOnlyKeys("error");
    assertThat(result.flash().data().get("error"))
        .contains("Description can't be removed because an image is present");
  }

  @Test
  public void updateDescription_redirectIncludesSameEditStatus() {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    Result result =
        controller.updateDescription(
            addCSRFToken(
                    fakeRequestBuilder()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("summaryImageDescription", "fake description")))
                .build(),
            program.id,
            ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramImageController.index(program.id, ProgramEditStatus.EDIT.name())
                .url());
  }

  @Test
  public void updateFileKey_programNotDraft_throws() {
    ProgramModel program = ProgramBuilder.newActiveProgram().build();

    Http.Request request =
        addCSRFToken(
                fakeRequestBuilder()
                    .method("POST")
                    .bodyForm(ImmutableMap.of("bucket", FAKE_BUCKET_NAME, "key", "fakeFileKey")))
            .build();

    assertThatExceptionOfType(NotChangeableException.class)
        .isThrownBy(
            () -> controller.updateFileKey(request, program.id, ProgramEditStatus.CREATION.name()));
  }

  @Test
  public void updateFileKey_missingProgram_throws() {
    Http.Request request =
        addCSRFToken(
                fakeRequestBuilder()
                    .method("POST")
                    .bodyForm(ImmutableMap.of("bucket", FAKE_BUCKET_NAME, "key", "fakeFileKey")))
            .build();

    assertThatExceptionOfType(NotChangeableException.class)
        .isThrownBy(
            () ->
                controller.updateFileKey(
                    request, /* programId= */ Long.MAX_VALUE, ProgramEditStatus.CREATION.name()));
  }

  @Test
  public void updateFileKey_noBucket_throwsException() {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    Http.Request request =
        addCSRFToken(
                fakeRequestBuilder()
                    .method("POST")
                    .uri(createUriWithQueryString(ImmutableMap.of("key", "fakeFileKey"))))
            .build();

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> controller.updateFileKey(request, program.id, ProgramEditStatus.CREATION.name()))
        .withMessageContaining("must contain bucket");
  }

  @Test
  public void updateFileKey_noKey_throwsException() {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    Http.Request request =
        addCSRFToken(
                fakeRequestBuilder()
                    .method("POST")
                    .uri(createUriWithQueryString(ImmutableMap.of("bucket", FAKE_BUCKET_NAME))))
            .build();

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> controller.updateFileKey(request, program.id, ProgramEditStatus.CREATION.name()))
        .withMessageContaining("must contain file key");
  }

  @Test
  public void updateFileKey_bucketDoesNotMatchPublicBucket_throwsException() {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    Http.Request request =
        addCSRFToken(
                fakeRequestBuilder()
                    .method("POST")
                    .uri(
                        createUriWithQueryString(
                            ImmutableMap.of("bucket", FAKE_BUCKET_NAME + "abc"))))
            .build();

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> controller.updateFileKey(request, program.id, ProgramEditStatus.CREATION.name()))
        .withMessageContaining("doesn't match the public bucket name");
  }

  @Test
  public void updateFileKey_keyIncorrectlyFormatted_throwsException() {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    Http.Request request =
        addCSRFToken(
                fakeRequestBuilder()
                    .method("POST")
                    .uri(
                        createUriWithQueryString(
                            ImmutableMap.of(
                                "bucket", FAKE_BUCKET_NAME, "key", "applicant-10/myFile"))))
            .build();

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> controller.updateFileKey(request, program.id, ProgramEditStatus.CREATION.name()))
        .withMessageContaining("Key incorrectly formatted");
  }

  @Test
  public void updateFileKey_hasBucketAndKey_keyUpdated() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    controller.updateFileKey(
        addCSRFToken(
                fakeRequestBuilder()
                    .method("POST")
                    .uri(
                        createUriWithQueryString(
                            ImmutableMap.of(
                                "bucket",
                                FAKE_BUCKET_NAME,
                                "key",
                                "program-summary-image/program-15/myImage.png"))))
            .build(),
        program.id,
        ProgramEditStatus.CREATION.name());

    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(updatedProgram.summaryImageFileKey()).isNotEmpty();
    assertThat(updatedProgram.summaryImageFileKey().get())
        .isEqualTo("program-summary-image/program-15/myImage.png");
  }

  @Test
  public void updateFileKey_hasBucketAndKey_toastsSuccess() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    Result result = setValidFileKeyOnProgram(program);

    assertThat(result.flash().data()).containsOnlyKeys("success");
    assertThat(result.flash().data().get("success")).contains("Image set");
  }

  @Test
  public void updateFileKey_setsNewKey_keyUpdated() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    controller.updateFileKey(
        addCSRFToken(
                fakeRequestBuilder()
                    .method("POST")
                    .uri(
                        createUriWithQueryString(
                            ImmutableMap.of(
                                "bucket",
                                FAKE_BUCKET_NAME,
                                "key",
                                "program-summary-image/program-15/oldImage.png"))))
            .build(),
        program.id,
        ProgramEditStatus.CREATION.name());

    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(updatedProgram.summaryImageFileKey()).isNotEmpty();
    assertThat(updatedProgram.summaryImageFileKey().get())
        .isEqualTo("program-summary-image/program-15/oldImage.png");

    // WHEN the key is updated
    controller.updateFileKey(
        addCSRFToken(
                fakeRequestBuilder()
                    .method("POST")
                    .uri(
                        createUriWithQueryString(
                            ImmutableMap.of(
                                "bucket",
                                FAKE_BUCKET_NAME,
                                "key",
                                "program-summary-image/program-15/newImage.png"))))
            .build(),
        program.id,
        ProgramEditStatus.CREATION.name());

    // THEN the database reflects the changes
    updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(updatedProgram.summaryImageFileKey()).isNotEmpty();
    assertThat(updatedProgram.summaryImageFileKey().get())
        .isEqualTo("program-summary-image/program-15/newImage.png");
  }

  @Test
  public void updateFileKey_redirectIncludesSameEditStatus() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    Result result =
        controller.updateFileKey(
            addCSRFToken(
                    fakeRequestBuilder()
                        .method("POST")
                        .uri(
                            createUriWithQueryString(
                                ImmutableMap.of(
                                    "bucket",
                                    FAKE_BUCKET_NAME,
                                    "key",
                                    "program-summary-image/program-15/newImage.png"))))
                .build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramImageController.index(program.id, ProgramEditStatus.CREATION.name())
                .url());
  }

  @Test
  public void deleteFileKey_programNotDraft_throws() {
    ProgramModel program = ProgramBuilder.newActiveProgram().build();

    assertThatExceptionOfType(NotChangeableException.class)
        .isThrownBy(
            () ->
                controller.deleteFileKey(
                    fakeRequestNew(), program.id, ProgramEditStatus.CREATION.name()));
  }

  @Test
  public void deleteFileKey_missingProgram_throws() {
    assertThatExceptionOfType(NotChangeableException.class)
        .isThrownBy(
            () ->
                controller.deleteFileKey(
                    fakeRequestNew(),
                    /* programId= */ Long.MAX_VALUE,
                    ProgramEditStatus.CREATION.name()));
  }

  @Test
  public void deleteFileKey_noFileKeyPresent_stillNoKey() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    controller.deleteFileKey(fakeRequestNew(), program.id, ProgramEditStatus.CREATION.name());

    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(updatedProgram.summaryImageFileKey().isEmpty()).isTrue();
  }

  @Test
  public void deleteFileKey_hadFileKey_keyRemoved() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();
    setValidFileKeyOnProgram(program);

    controller.deleteFileKey(fakeRequestNew(), program.id, ProgramEditStatus.CREATION.name());

    ProgramDefinition updatedProgram = programService.getFullProgramDefinition(program.id);
    assertThat(updatedProgram.summaryImageFileKey()).isEmpty();
  }

  @Test
  public void deleteFileKey_toastsSuccess() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();
    setValidFileKeyOnProgram(program);

    Result result =
        controller.deleteFileKey(fakeRequestNew(), program.id, ProgramEditStatus.CREATION.name());

    assertThat(result.flash().data()).containsOnlyKeys("success");
    assertThat(result.flash().data().get("success")).contains("Image removed");
  }

  @Test
  public void deleteFileKey_redirectIncludesSameEditStatus() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name").build();

    Result result =
        controller.deleteFileKey(fakeRequestNew(), program.id, ProgramEditStatus.CREATION.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramImageController.index(program.id, ProgramEditStatus.CREATION.name())
                .url());
  }

  private String createUriWithQueryString(ImmutableMap<String, String> query) {
    String queryString =
        query.entrySet().stream()
            .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining("&"));
    return "/?" + queryString;
  }

  private Result setValidFileKeyOnProgram(ProgramModel program) throws ProgramNotFoundException {
    Result result =
        controller.updateFileKey(
            addCSRFToken(
                    fakeRequestBuilder()
                        .method("POST")
                        .uri(
                            createUriWithQueryString(
                                ImmutableMap.of(
                                    "bucket", FAKE_BUCKET_NAME, "key", VALID_FILE_KEY))))
                .build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    ProgramDefinition programWithKey = programService.getFullProgramDefinition(program.id);
    assertThat(programWithKey.summaryImageFileKey()).isNotEmpty();
    assertThat(programWithKey.summaryImageFileKey().get()).isEqualTo(VALID_FILE_KEY);
    return result;
  }
}
