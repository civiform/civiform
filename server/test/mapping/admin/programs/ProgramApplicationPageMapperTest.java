package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import org.junit.Before;
import org.junit.Test;
import services.DateConverter;
import services.LocalizedStrings;
import services.Path;
import services.applicant.AnswerData;
import services.applicant.Block;
import services.applicant.question.ApplicantQuestion;
import services.program.EligibilityDefinition;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.question.types.QuestionDefinition;
import services.statuses.StatusDefinitions;
import views.admin.programs.ProgramApplicationPageViewModel;
import views.admin.programs.ProgramApplicationPageViewModel.Answer;
import views.admin.programs.ProgramApplicationPageViewModel.BlockCard;
import views.admin.programs.ProgramApplicationPageViewModel.FileLink;
import views.admin.programs.ProgramApplicationPageViewModel.StatusModal;
import views.admin.programs.ProgramApplicationPageViewModel.StatusOption;

public final class ProgramApplicationPageMapperTest {

  private static final long PROGRAM_ID = 5L;
  private static final long APPLICATION_ID = 42L;
  private static final String PROGRAM_NAME = "Utility Discount";
  private static final String APPLICANT_NAME = "Doe, Jane (42)";
  private static final Instant SUBMIT_TIME = Instant.parse("2024-01-01T17:00:00Z");
  private static final long ANSWER_TIMESTAMP = 1704128400000L;
  private static final ProgramApplicationPageMapper.ListFilters NO_FILTERS =
      new ProgramApplicationPageMapper.ListFilters(
          Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

  private ProgramApplicationPageMapper mapper;
  private DateConverter dateConverter;
  private ApplicationModel application;
  private AccountModel account;
  private ApplicantModel applicant;

  @Before
  public void setup() {
    mapper = new ProgramApplicationPageMapper();

    dateConverter = mock(DateConverter.class);
    when(dateConverter.renderDateTimeHumanReadable(SUBMIT_TIME))
        .thenReturn("2024/01/01 at 9:00 AM PST");
    when(dateConverter.renderDate(Instant.ofEpochMilli(ANSWER_TIMESTAMP))).thenReturn("2024/01/01");

    account = mock(AccountModel.class);
    applicant = mock(ApplicantModel.class);
    when(applicant.getAccount()).thenReturn(account);
    when(applicant.getEmailAddress()).thenReturn(Optional.empty());

    application = mock(ApplicationModel.class);
    application.id = APPLICATION_ID;
    when(application.getApplicant()).thenReturn(applicant);
    when(application.getSubmitTime()).thenReturn(SUBMIT_TIME);
    when(application.getLatestStatus()).thenReturn(Optional.empty());
  }

  private static StatusDefinitions.Status status(String text, Optional<String> emailBody) {
    return StatusDefinitions.Status.builder()
        .setStatusText(text)
        .setLocalizedStatusText(LocalizedStrings.withDefaultValue(text))
        .setLocalizedEmailBodyText(emailBody.map(LocalizedStrings::withDefaultValue))
        .build();
  }

  private static StatusDefinitions statuses(StatusDefinitions.Status... statuses) {
    return new StatusDefinitions(ImmutableList.copyOf(statuses));
  }

  private static Block block(String id, String name, String description) {
    Block block = mock(Block.class);
    when(block.getId()).thenReturn(id);
    when(block.getName()).thenReturn(name);
    when(block.getDescription()).thenReturn(description);
    when(block.getEligibilityDefinition()).thenReturn(Optional.empty());
    return block;
  }

  private static AnswerData.Builder answerBuilder(
      String blockId, long questionId, String questionName) {
    QuestionDefinition question = mock(QuestionDefinition.class);
    when(question.getId()).thenReturn(questionId);
    when(question.getName()).thenReturn(questionName);

    return AnswerData.builder()
        .setProgramId(PROGRAM_ID)
        .setBlockId(blockId)
        .setContextualizedPath(Path.create("applicant." + questionName))
        .setQuestionDefinition(question)
        .setApplicantQuestion(mock(ApplicantQuestion.class))
        .setRepeatedEntity(Optional.empty())
        .setQuestionIndex(0)
        .setQuestionText(questionName)
        .setQuestionTextForScreenReader(questionName)
        .setIsAnswered(true)
        .setIsEligible(false)
        .setEligibilityIsGating(true)
        .setLoginOnly(false)
        .setAnswerText("an answer")
        .setEncodedFileKey(Optional.empty())
        .setOriginalFileName(Optional.empty())
        .setEncodedFileKeys(ImmutableList.of())
        .setFileNames(ImmutableList.of())
        .setTimestamp(ANSWER_TIMESTAMP)
        .setIsPreviousResponse(false);
  }

  private static AnswerData answer(String blockId, long questionId, String questionName) {
    return answerBuilder(blockId, questionId, questionName).build();
  }

  private ProgramApplicationPageViewModel map(
      ImmutableList<Block> blocks,
      ImmutableList<AnswerData> answers,
      StatusDefinitions statusDefinitions,
      Optional<String> note,
      boolean hasEligibilityEnabled,
      boolean showDownloadButton,
      ProgramApplicationPageMapper.ListFilters listFilters,
      Optional<String> successMessage) {
    return mapper.map(
        PROGRAM_ID,
        PROGRAM_NAME,
        application,
        APPLICANT_NAME,
        blocks,
        answers,
        statusDefinitions,
        note,
        hasEligibilityEnabled,
        showDownloadButton,
        listFilters,
        successMessage,
        dateConverter);
  }

  private ProgramApplicationPageViewModel mapMinimal() {
    return map(
        ImmutableList.of(),
        ImmutableList.of(),
        statuses(),
        /* note= */ Optional.empty(),
        /* hasEligibilityEnabled= */ false,
        /* showDownloadButton= */ true,
        NO_FILTERS,
        /* successMessage= */ Optional.empty());
  }

  private ProgramApplicationPageViewModel mapWithStatuses(StatusDefinitions statusDefinitions) {
    return map(
        ImmutableList.of(),
        ImmutableList.of(),
        statusDefinitions,
        /* note= */ Optional.empty(),
        /* hasEligibilityEnabled= */ false,
        /* showDownloadButton= */ true,
        NO_FILTERS,
        /* successMessage= */ Optional.empty());
  }

  private ProgramApplicationPageViewModel mapWithAnswers(
      ImmutableList<Block> blocks, ImmutableList<AnswerData> answers) {
    return map(
        blocks,
        answers,
        statuses(),
        /* note= */ Optional.empty(),
        /* hasEligibilityEnabled= */ false,
        /* showDownloadButton= */ true,
        NO_FILTERS,
        /* successMessage= */ Optional.empty());
  }

  @Test
  public void map_setsProgramAndApplicationIdentity() {
    ProgramApplicationPageViewModel result = mapMinimal();

    assertThat(result.getProgramId()).isEqualTo(PROGRAM_ID);
    assertThat(result.getApplicationId()).isEqualTo(APPLICATION_ID);
    assertThat(result.getProgramName()).isEqualTo(PROGRAM_NAME);
    assertThat(result.getApplicantNameWithApplicationId()).isEqualTo(APPLICANT_NAME);
  }

  @Test
  public void map_noFilters_setsBackUrlToApplicationList() {
    ProgramApplicationPageViewModel result = mapMinimal();

    assertThat(result.getBackUrl()).isEqualTo("/admin/programs/5/applications");
  }

  @Test
  public void map_withFilters_echoesFiltersIntoBackUrl() {
    ProgramApplicationPageViewModel result =
        map(
            ImmutableList.of(),
            ImmutableList.of(),
            statuses(),
            /* note= */ Optional.empty(),
            /* hasEligibilityEnabled= */ false,
            /* showDownloadButton= */ true,
            new ProgramApplicationPageMapper.ListFilters(
                Optional.of("jane"),
                Optional.of("2024-01-01"),
                Optional.of("2024-02-01"),
                Optional.of(2),
                Optional.of("Approved")),
            /* successMessage= */ Optional.empty());

    assertThat(result.getBackUrl())
        .startsWith("/admin/programs/5/applications?")
        .contains("search=jane")
        .contains("page=2")
        .contains("fromDate=2024-01-01")
        .contains("untilDate=2024-02-01")
        .contains("applicationStatus=Approved");
  }

  @Test
  public void map_setsHumanReadableSubmitTime() {
    ProgramApplicationPageViewModel result = mapMinimal();

    assertThat(result.getSubmitTime()).isEqualTo("2024/01/01 at 9:00 AM PST");
  }

  @Test
  public void map_noSubmitTime_setsNullSubmitTime() {
    when(application.getSubmitTime()).thenReturn(null);

    ProgramApplicationPageViewModel result = mapMinimal();

    assertThat(result.getSubmitTime()).isNull();
  }

  @Test
  public void map_setsDownloadButton() {
    ProgramApplicationPageViewModel result = mapMinimal();

    assertThat(result.isShowDownloadButton()).isTrue();
    assertThat(result.getDownloadUrl()).isEqualTo("/admin/programs/5/applications/42/download");
  }

  @Test
  public void map_downloadHidden_setsShowDownloadButtonFalse() {
    ProgramApplicationPageViewModel result =
        map(
            ImmutableList.of(),
            ImmutableList.of(),
            statuses(),
            /* note= */ Optional.empty(),
            /* hasEligibilityEnabled= */ false,
            /* showDownloadButton= */ false,
            NO_FILTERS,
            /* successMessage= */ Optional.empty());

    assertThat(result.isShowDownloadButton()).isFalse();
  }

  @Test
  public void map_noStatuses_hidesStatusOptions() {
    ProgramApplicationPageViewModel result = mapMinimal();

    assertThat(result.isHasStatuses()).isFalse();
    assertThat(result.getStatusOptions()).isEmpty();
    assertThat(result.getStatusModals()).isEmpty();
  }

  @Test
  public void map_withStatuses_setsStatusOptionsInOrder() {
    ProgramApplicationPageViewModel result =
        mapWithStatuses(
            statuses(status("Approved", Optional.empty()), status("Rejected", Optional.empty())));

    assertThat(result.isHasStatuses()).isTrue();
    assertThat(result.getStatusSelectorId()).matches("[A-Za-z]{8}");
    assertThat(result.getStatusOptions())
        .extracting(StatusOption::getText)
        .containsExactly("Approved", "Rejected");
  }

  @Test
  public void map_noCurrentStatus_selectsPlaceholder() {
    ProgramApplicationPageViewModel result =
        mapWithStatuses(statuses(status("Approved", Optional.empty())));

    assertThat(result.isNoStatusSelected()).isTrue();
    assertThat(result.getStatusOptions())
        .extracting(StatusOption::isSelected)
        .containsExactly(false);
  }

  @Test
  public void map_currentStatus_selectsMatchingOption() {
    when(application.getLatestStatus()).thenReturn(Optional.of("Rejected"));

    ProgramApplicationPageViewModel result =
        mapWithStatuses(
            statuses(status("Approved", Optional.empty()), status("Rejected", Optional.empty())));

    assertThat(result.isNoStatusSelected()).isFalse();
    assertThat(result.getStatusOptions())
        .extracting(StatusOption::isSelected)
        .containsExactly(false, true);
  }

  @Test
  public void map_noNote_setsHasNoteFalseAndEmptyModalNote() {
    ProgramApplicationPageViewModel result = mapMinimal();

    assertThat(result.isHasNote()).isFalse();
    assertThat(result.getNoteModal().getNote()).isNull();
  }

  @Test
  public void map_emptyNote_setsHasNoteFalse() {
    ProgramApplicationPageViewModel result =
        map(
            ImmutableList.of(),
            ImmutableList.of(),
            statuses(),
            /* note= */ Optional.of(""),
            /* hasEligibilityEnabled= */ false,
            /* showDownloadButton= */ true,
            NO_FILTERS,
            /* successMessage= */ Optional.empty());

    assertThat(result.isHasNote()).isFalse();
    assertThat(result.getNoteModal().getNote()).isEmpty();
  }

  @Test
  public void map_withNote_setsHasNoteAndModalNote() {
    ProgramApplicationPageViewModel result =
        map(
            ImmutableList.of(),
            ImmutableList.of(),
            statuses(),
            /* note= */ Optional.of("Called applicant"),
            /* hasEligibilityEnabled= */ false,
            /* showDownloadButton= */ true,
            NO_FILTERS,
            /* successMessage= */ Optional.empty());

    assertThat(result.isHasNote()).isTrue();
    assertThat(result.getNoteModal().getNote()).isEqualTo("Called applicant");
  }

  @Test
  public void map_setsRandomNoteModalIds() {
    ProgramApplicationPageViewModel result = mapMinimal();

    assertThat(result.getNoteModal().getId()).startsWith("uuid-");
    assertThat(result.getNoteModal().getFormId()).startsWith("uuid-");
    assertThat(result.getNoteModal().getId()).isNotEqualTo(result.getNoteModal().getFormId());
    assertThat(result.getNoteModal().getFieldId()).matches("[A-Za-z]{8}");
  }

  @Test
  public void map_statusModal_noCurrentStatus_showsUnsetAndPostsEmpty() {
    ProgramApplicationPageViewModel result =
        mapWithStatuses(statuses(status("Approved", Optional.empty())));

    StatusModal modal = result.getStatusModals().get(0);
    assertThat(modal.getId()).startsWith("uuid-");
    assertThat(modal.getStatusText()).isEqualTo("Approved");
    assertThat(modal.getPreviousStatusDisplay()).isEqualTo("Unset");
    assertThat(modal.getPreviousStatusData()).isEmpty();
  }

  @Test
  public void map_statusModal_currentStatus_showsAndPostsCurrentStatus() {
    when(application.getLatestStatus()).thenReturn(Optional.of("Approved"));

    ProgramApplicationPageViewModel result =
        mapWithStatuses(statuses(status("Rejected", Optional.empty())));

    StatusModal modal = result.getStatusModals().get(0);
    assertThat(modal.getPreviousStatusDisplay()).isEqualTo("Approved");
    assertThat(modal.getPreviousStatusData()).isEqualTo("Approved");
  }

  @Test
  public void map_statusModal_noEmailBody_setsNoEmailContentMode() {
    when(account.getEmailAddress()).thenReturn("jane@example.com");

    ProgramApplicationPageViewModel result =
        mapWithStatuses(statuses(status("Approved", Optional.empty())));

    StatusModal modal = result.getStatusModals().get(0);
    assertThat(modal.getEmailMode()).isEqualTo(StatusModal.EmailMode.NO_EMAIL_CONTENT);
    assertThat(modal.getEmailAddresses()).isNull();
  }

  @Test
  public void map_statusModal_noApplicantEmail_setsNoApplicantEmailMode() {
    ProgramApplicationPageViewModel result =
        mapWithStatuses(statuses(status("Approved", Optional.of("You are approved"))));

    StatusModal modal = result.getStatusModals().get(0);
    assertThat(modal.getEmailMode()).isEqualTo(StatusModal.EmailMode.NO_APPLICANT_EMAIL);
    assertThat(modal.getEmailAddresses()).isNull();
  }

  @Test
  public void map_statusModal_accountEmail_setsNotifyMode() {
    when(account.getEmailAddress()).thenReturn("jane@example.com");

    ProgramApplicationPageViewModel result =
        mapWithStatuses(statuses(status("Approved", Optional.of("You are approved"))));

    StatusModal modal = result.getStatusModals().get(0);
    assertThat(modal.getEmailMode()).isEqualTo(StatusModal.EmailMode.NOTIFY);
    assertThat(modal.getEmailAddresses()).isEqualTo("jane@example.com");
  }

  @Test
  public void map_statusModal_distinctAccountAndApplicantEmails_joinsWithAnd() {
    when(account.getEmailAddress()).thenReturn("jane@example.com");
    when(applicant.getEmailAddress()).thenReturn(Optional.of("jane.doe@example.com"));

    ProgramApplicationPageViewModel result =
        mapWithStatuses(statuses(status("Approved", Optional.of("You are approved"))));

    assertThat(result.getStatusModals().get(0).getEmailAddresses())
        .isEqualTo("jane@example.com and jane.doe@example.com");
  }

  @Test
  public void map_statusModal_sameAccountAndApplicantEmail_listsItOnce() {
    when(account.getEmailAddress()).thenReturn("jane@example.com");
    when(applicant.getEmailAddress()).thenReturn(Optional.of("jane@example.com"));

    ProgramApplicationPageViewModel result =
        mapWithStatuses(statuses(status("Approved", Optional.of("You are approved"))));

    assertThat(result.getStatusModals().get(0).getEmailAddresses()).isEqualTo("jane@example.com");
  }

  @Test
  public void map_setsBlockCardsWithTheirAnswers() {
    Block screen1 = block("1", "Screen 1", "First screen");
    Block screen2 = block("2", "Screen 2", "Second screen");
    AnswerData nameAnswer = answer("1", 10L, "name-q");
    AnswerData emailAnswer = answer("2", 20L, "email-q");

    ProgramApplicationPageViewModel result =
        mapWithAnswers(
            ImmutableList.of(screen1, screen2), ImmutableList.of(nameAnswer, emailAnswer));

    assertThat(result.getBlocks())
        .extracting(BlockCard::getName, BlockCard::getDescription)
        .containsExactly(tuple("Screen 1", "First screen"), tuple("Screen 2", "Second screen"));
    assertThat(result.getBlocks().get(0).getAnswers())
        .extracting(Answer::getQuestionName)
        .containsExactly("name-q");
    assertThat(result.getBlocks().get(1).getAnswers())
        .extracting(Answer::getQuestionName)
        .containsExactly("email-q");
  }

  @Test
  public void map_textAnswer_collapsesNewlines() {
    Block screen = block("1", "Screen 1", "");
    AnswerData textAnswer =
        answerBuilder("1", 10L, "address-q").setAnswerText("123 Main St\nSeattle, WA").build();

    Answer result =
        mapWithAnswers(ImmutableList.of(screen), ImmutableList.of(textAnswer))
            .getBlocks()
            .get(0)
            .getAnswers()
            .get(0);

    assertThat(result.getContentType()).isEqualTo(Answer.ContentType.TEXT);
    assertThat(result.getAnswerText()).isEqualTo("123 Main St; Seattle, WA");
    assertThat(result.getFiles()).isEmpty();
    assertThat(result.getFileHref()).isNull();
    assertThat(result.getAnsweredOn()).isEqualTo("2024/01/01");
  }

  @Test
  public void map_singleFileAnswer_linksAnswerTextToFile() {
    Block screen = block("1", "Screen 1", "");
    AnswerData fileAnswer =
        answerBuilder("1", 10L, "file-q")
            .setAnswerText("resume.pdf")
            .setEncodedFileKey(Optional.of("encoded-key"))
            .build();

    Answer result =
        mapWithAnswers(ImmutableList.of(screen), ImmutableList.of(fileAnswer))
            .getBlocks()
            .get(0)
            .getAnswers()
            .get(0);

    assertThat(result.getContentType()).isEqualTo(Answer.ContentType.FILE_LINK);
    assertThat(result.getAnswerText()).isEqualTo("resume.pdf");
    assertThat(result.getFileHref()).isEqualTo("/admin/applicant-files/encoded-key");
  }

  @Test
  public void map_multipleFileAnswer_linksEachFile() {
    Block screen = block("1", "Screen 1", "");
    AnswerData filesAnswer =
        answerBuilder("1", 10L, "files-q")
            .setEncodedFileKeys(ImmutableList.of("key-1", "key-2"))
            .setFileNames(ImmutableList.of("a.pdf", "b.pdf"))
            .build();

    Answer result =
        mapWithAnswers(ImmutableList.of(screen), ImmutableList.of(filesAnswer))
            .getBlocks()
            .get(0)
            .getAnswers()
            .get(0);

    assertThat(result.getContentType()).isEqualTo(Answer.ContentType.FILE_LIST);
    assertThat(result.getFiles())
        .extracting(FileLink::getName, FileLink::getHref)
        .containsExactly(
            tuple("a.pdf", "/admin/applicant-files/key-1"),
            tuple("b.pdf", "/admin/applicant-files/key-2"));
    assertThat(result.getAnswerText()).isNull();
  }

  @Test
  public void map_eligibilityDisabled_hidesEligibilityText() {
    Block screen = block("1", "Screen 1", "");
    when(screen.getEligibilityDefinition()).thenReturn(Optional.of(eligibilityOnQuestion(10L)));
    AnswerData eligibleAnswer = answerBuilder("1", 10L, "income-q").setIsEligible(true).build();

    Answer result =
        map(
                ImmutableList.of(screen),
                ImmutableList.of(eligibleAnswer),
                statuses(),
                /* note= */ Optional.empty(),
                /* hasEligibilityEnabled= */ false,
                /* showDownloadButton= */ true,
                NO_FILTERS,
                /* successMessage= */ Optional.empty())
            .getBlocks()
            .get(0)
            .getAnswers()
            .get(0);

    assertThat(result.isShowEligibilityText()).isFalse();
  }

  @Test
  public void map_eligibilityEnabled_showsEligibilityTextForPredicateQuestionsOnly() {
    Block screen = block("1", "Screen 1", "");
    when(screen.getEligibilityDefinition()).thenReturn(Optional.of(eligibilityOnQuestion(10L)));
    AnswerData predicateAnswer = answerBuilder("1", 10L, "income-q").setIsEligible(false).build();
    AnswerData otherAnswer = answer("1", 20L, "name-q");

    ImmutableList<Answer> result =
        map(
                ImmutableList.of(screen),
                ImmutableList.of(predicateAnswer, otherAnswer),
                statuses(),
                /* note= */ Optional.empty(),
                /* hasEligibilityEnabled= */ true,
                /* showDownloadButton= */ true,
                NO_FILTERS,
                /* successMessage= */ Optional.empty())
            .getBlocks()
            .get(0)
            .getAnswers();

    assertThat(result)
        .extracting(Answer::isShowEligibilityText, Answer::isEligible)
        .containsExactly(tuple(true, false), tuple(false, false));
  }

  @Test
  public void map_noSuccessMessage_setsNullToast() {
    ProgramApplicationPageViewModel result = mapMinimal();

    assertThat(result.getSuccessToast()).isNull();
  }

  @Test
  public void map_successMessage_setsToastWithRandomId() {
    ProgramApplicationPageViewModel result =
        map(
            ImmutableList.of(),
            ImmutableList.of(),
            statuses(),
            /* note= */ Optional.empty(),
            /* hasEligibilityEnabled= */ false,
            /* showDownloadButton= */ true,
            NO_FILTERS,
            /* successMessage= */ Optional.of("Application status updated"));

    assertThat(result.getSuccessToast().getMessage()).isEqualTo("Application status updated");
    assertThat(result.getSuccessToast().getId()).isNotBlank();
  }

  private static EligibilityDefinition eligibilityOnQuestion(long questionId) {
    return EligibilityDefinition.builder()
        .setPredicate(
            PredicateDefinition.create(
                PredicateExpressionNode.create(
                    LeafAddressServiceAreaExpressionNode.create(
                        questionId, "seattle", Operator.IN_SERVICE_AREA)),
                PredicateAction.ELIGIBLE_BLOCK))
        .build();
  }
}
