package views.applicant;



/*
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

 */
