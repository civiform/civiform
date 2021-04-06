package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;
import java.util.EnumSet;
import java.util.Locale;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.Path;
import services.applicant.ApplicantData;
import services.question.AddressQuestionDefinition;
import services.question.DropdownQuestionDefinition;
import services.question.NameQuestionDefinition;
import services.question.NumberQuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;
import services.question.TextQuestionDefinition;
import services.question.UnsupportedQuestionTypeException;

@RunWith(JUnitParamsRunner.class)
public class ApplicantQuestionTest {

  private static final DropdownQuestionDefinition dropdownQuestionDefinition =
      new DropdownQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          ImmutableListMultimap.of(
              Locale.US,
              "option 1",
              Locale.US,
              "option 2",
              Locale.FRANCE,
              "un",
              Locale.FRANCE,
              "deux"));
  private static final TextQuestionDefinition textQuestionDefinition =
      new TextQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));
  private static final NameQuestionDefinition nameQuestionDefinition =
      new NameQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));
  private static final NumberQuestionDefinition numberQuestionDefinition =
      new NumberQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));
  private static final AddressQuestionDefinition addressQuestionDefinition =
      new AddressQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));

  // TODO(https://github.com/seattle-uat/civiform/issues/405): Change this to just use
  // @Parameters(source = QuestionType.class) once RepeatedQuestionDefinition exists.
  @Test
  @Parameters(method = "types")
  public void errorsPresenterExtendedForAllTypes(QuestionType type)
      throws UnsupportedQuestionTypeException {
    QuestionDefinitionBuilder builder = QuestionDefinitionBuilder.sample(type);
    ApplicantQuestion question = new ApplicantQuestion(builder.build(), new ApplicantData());

    assertThat(question.errorsPresenter().hasTypeSpecificErrors()).isFalse();
  }

  private EnumSet<QuestionType> types() {
    return EnumSet.complementOf(EnumSet.of(QuestionType.REPEATER));
  }

  @Test
  public void getsExpectedQuestionType() {
    ApplicantQuestion addressApplicantQuestion =
        new ApplicantQuestion(addressQuestionDefinition, new ApplicantData());
    assertThat(addressApplicantQuestion.getAddressQuestion()).isInstanceOf(AddressQuestion.class);

    ApplicantQuestion nameApplicantQuestion =
        new ApplicantQuestion(nameQuestionDefinition, new ApplicantData());
    assertThat(nameApplicantQuestion.getNameQuestion()).isInstanceOf(NameQuestion.class);

    ApplicantQuestion numberApplicantQuestion =
        new ApplicantQuestion(numberQuestionDefinition, new ApplicantData());
    assertThat(numberApplicantQuestion.getNumberQuestion()).isInstanceOf(NumberQuestion.class);

    ApplicantQuestion singleSelectApplicantQuestion =
        new ApplicantQuestion(dropdownQuestionDefinition, new ApplicantData());
    assertThat(singleSelectApplicantQuestion.getSingleSelectQuestion())
        .isInstanceOf(SingleSelectQuestion.class);

    ApplicantQuestion textApplicantQuestion =
        new ApplicantQuestion(textQuestionDefinition, new ApplicantData());
    assertThat(textApplicantQuestion.getTextQuestion()).isInstanceOf(TextQuestion.class);
  }

  @Test
  public void equals() {
    ApplicantData dataWithAnswers = new ApplicantData();
    dataWithAnswers.putString(Path.create("applicant.color"), "blue");

    new EqualsTester()
        .addEqualityGroup(
            new ApplicantQuestion(addressQuestionDefinition, new ApplicantData()),
            new ApplicantQuestion(addressQuestionDefinition, new ApplicantData()))
        .addEqualityGroup(
            new ApplicantQuestion(dropdownQuestionDefinition, new ApplicantData()),
            new ApplicantQuestion(dropdownQuestionDefinition, new ApplicantData()))
        .addEqualityGroup(
            new ApplicantQuestion(nameQuestionDefinition, new ApplicantData()),
            new ApplicantQuestion(nameQuestionDefinition, new ApplicantData()))
        .addEqualityGroup(
            new ApplicantQuestion(numberQuestionDefinition, new ApplicantData()),
            new ApplicantQuestion(numberQuestionDefinition, new ApplicantData()))
        .addEqualityGroup(
            new ApplicantQuestion(textQuestionDefinition, new ApplicantData()),
            new ApplicantQuestion(textQuestionDefinition, new ApplicantData()))
        .addEqualityGroup(
            new ApplicantQuestion(textQuestionDefinition, dataWithAnswers),
            new ApplicantQuestion(textQuestionDefinition, dataWithAnswers))
        .testEquals();
  }
}
