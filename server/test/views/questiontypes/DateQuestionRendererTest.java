package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableSet;
import j2html.attributes.Attr;
import j2html.tags.specialized.DivTag;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Messages;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.types.DateQuestionDefinition;
import services.question.types.DateQuestionDefinition.DateValidationOption;
import services.question.types.DateQuestionDefinition.DateValidationOption.DateType;
import services.question.types.DateQuestionDefinition.DateValidationPredicates;
import services.question.types.QuestionDefinitionConfig;
import views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode;

public class DateQuestionRendererTest {
  private static final QuestionDefinitionConfig CONFIG =
      QuestionDefinitionConfig.builder()
          .setName("question name")
          .setDescription("description")
          .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
          .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
          .setId(OptionalLong.of(1))
          .setLastModifiedTime(Optional.empty())
          .build();
  private static final DateQuestionDefinition QUESTION = new DateQuestionDefinition(CONFIG);

  private final Messages messages =
      stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));
  private ApplicantData applicantData;
  private ApplicantQuestion question;
  private DateQuestionRenderer renderer;
  private ApplicantQuestionRendererParams params;

  @Before
  public void setup() {
    applicantData = new ApplicantData();
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ErrorDisplayMode.HIDE_ERRORS)
            .build();
    question =
        new ApplicantQuestion(QUESTION, new ApplicantModel(), applicantData, Optional.empty());
    renderer = new DateQuestionRenderer(question);
  }

  @Test
  public void applicantSelectedQuestionNameMatch_hasAutoFocus() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setAutofocus(ApplicantQuestionRendererParams.AutoFocusTarget.FIRST_FIELD)
            .setErrorDisplayMode(ErrorDisplayMode.DISPLAY_ERRORS)
            .build();

    DivTag result = renderer.render(params);

    assertThat(result.render()).contains(Attr.AUTOFOCUS);
  }

  @Test
  public void applicantSelectedQuestionNameMismatch_hasNoAutoFocus() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setAutofocus(ApplicantQuestionRendererParams.AutoFocusTarget.NONE)
            .setErrorDisplayMode(ErrorDisplayMode.DISPLAY_ERRORS)
            .build();

    DivTag result = renderer.render(params);

    assertThat(result.render()).doesNotContain(Attr.AUTOFOCUS);
  }

  @Test
  public void autofillsCurrentDate_whenBothMinAndMaxDateAreApplicationDate() {
    // Create a date question with both min and max date set to APPLICATION_DATE
    DateValidationOption minDate = DateValidationOption.builder()
        .setDateType(DateType.APPLICATION_DATE)
        .build();
    DateValidationOption maxDate = DateValidationOption.builder()
        .setDateType(DateType.APPLICATION_DATE)
        .build();
    
    DateQuestionDefinition questionWithValidation = new DateQuestionDefinition(
        QuestionDefinitionConfig.builder()
            .setName("question name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setId(OptionalLong.of(1))
            .setLastModifiedTime(Optional.empty())
            .setValidationPredicates(
                DateValidationPredicates.create(Optional.of(minDate), Optional.of(maxDate)))
            .build());
    
    ApplicantQuestion questionWithValidationWrapper = 
        new ApplicantQuestion(questionWithValidation, new ApplicantModel(), new ApplicantData(), Optional.empty());
    DateQuestionRenderer rendererWithValidation = new DateQuestionRenderer(questionWithValidationWrapper);
    
    DivTag result = rendererWithValidation.render(params);
    String todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
  
    assertThat(result.render()).contains("value=\"" + todayDate + "\"");
  }

  @Test
  public void doesNotAutofill_whenApplicantAlreadyHasDateValue() {
    DateValidationOption minDate = DateValidationOption.builder()
        .setDateType(DateType.APPLICATION_DATE)
        .build();
    DateValidationOption maxDate = DateValidationOption.builder()
        .setDateType(DateType.APPLICATION_DATE)
        .build();
    
    DateQuestionDefinition questionWithValidation = new DateQuestionDefinition(
        QuestionDefinitionConfig.builder()
            .setName("question name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setId(OptionalLong.of(1))
            .setLastModifiedTime(Optional.empty())
            .setValidationPredicates(
                DateValidationPredicates.create(Optional.of(minDate), Optional.of(maxDate)))
            .build());
    
    ApplicantData applicantDataWithDate = new ApplicantData();
    applicantDataWithDate.putDate(
        ApplicantData.APPLICANT_PATH.join("question_name").join("date"), 
        LocalDate.of(2023, 12, 25));
    
    ApplicantQuestion questionWithValidationWrapper = 
        new ApplicantQuestion(questionWithValidation, new ApplicantModel(), applicantDataWithDate, Optional.empty());
    DateQuestionRenderer rendererWithValidation = new DateQuestionRenderer(questionWithValidationWrapper);
    
    DivTag result = rendererWithValidation.render(params);
    
    assertThat(result.render()).contains("value=\"2023-12-25\"");
    String todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    assertThat(result.render()).doesNotContain("value=\"" + todayDate + "\"");
  }
}
