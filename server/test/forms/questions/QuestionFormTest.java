package forms.questions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.UUID;
import models.QuestionDisplayMode;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.PrimaryApplicantInfoTag;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.QuestionType;
import services.question.types.StaticContentQuestionDefinition;

public class QuestionFormTest {

  private static class TestQuestionForm extends QuestionForm {

    TestQuestionForm() {
      super();
    }

    TestQuestionForm(QuestionDefinition qd) {
      super(qd);
    }

    @Override
    public QuestionType getQuestionType() {
      return QuestionType.TEXT;
    }
  }

  @Test
  public void getRedirectUrl_relativeUrl() {
    TestQuestionForm form = new TestQuestionForm();
    form.setRedirectUrl("/some/relative/url?queryparam=a#hashparam=b");
    assertThat(form.getRedirectUrl()).isEqualTo("/some/relative/url?queryparam=a#hashparam=b");
  }

  @Test
  public void getRedirectUrl_null() {
    TestQuestionForm form = new TestQuestionForm();
    assertThat(form.getRedirectUrl()).isEqualTo("");
  }

  @Test
  public void getRedirectUrl_empty() {
    TestQuestionForm form = new TestQuestionForm();
    form.setRedirectUrl("");
    assertThat(form.getRedirectUrl()).isEqualTo("");
  }

  @Test
  public void getRedirectUrl_absoluteUrl_throws() {
    TestQuestionForm form = new TestQuestionForm();
    form.setRedirectUrl("https://www.example.com");
    assertThatThrownBy(form::getRedirectUrl).hasMessageContaining("Invalid absolute URL.");
  }

  @Test
  public void getRedirectUrl_otherSchemeUrl_throws() {
    TestQuestionForm form = new TestQuestionForm();
    form.setRedirectUrl("file://foo/bar");
    assertThatThrownBy(form::getRedirectUrl).hasMessageContaining("Invalid absolute URL.");
  }

  @Test
  public void getConcurrencyToken_generatesUUIDWhenUnset() {
    TestQuestionForm form = new TestQuestionForm();
    assertThat(form.getConcurrencyToken()).isNotNull();
  }

  @Test
  public void getDisplayMode_defaultsToVisible() {
    TestQuestionForm form = new TestQuestionForm();
    assertThat(form.getDisplayMode()).isEqualTo(QuestionDisplayMode.VISIBLE);
  }

  @Test
  public void getConcurrencyToken_returnsBuilderWithGeneratedUUIDWhenUnset()
      throws UnsupportedQuestionTypeException {
    TestQuestionForm form = new TestQuestionForm();
    assertThat(form.getBuilder().build().getConcurrencyToken()).isNotEmpty();
  }

  @Test
  public void getConcurrencyToken_returnsSameUUIDWhenSet() {
    UUID initialToken = UUID.randomUUID();
    TestQuestionForm form = new TestQuestionForm();
    form.setConcurrencyToken(initialToken);
    assertThat(form.getConcurrencyToken()).isEqualTo(initialToken.toString());
  }

  @Test
  public void getConcurrencyToken_returnsBuilderWithSameUUIDWhenSet()
      throws UnsupportedQuestionTypeException {
    UUID initialToken = UUID.randomUUID();
    TestQuestionForm form = new TestQuestionForm();
    form.setConcurrencyToken(initialToken);
    assertThat(form.getBuilder().build().getConcurrencyToken()).hasValue(initialToken);
  }

  @Test
  public void addsAndRemovesPrimaryApplicantInfoTags() throws UnsupportedQuestionTypeException {
    TestQuestionForm form = new TestQuestionForm();
    assertThat(form.primaryApplicantInfoTags()).isEqualTo(ImmutableSet.of());

    form.setPrimaryApplicantName(true);
    form.setPrimaryApplicantEmail(true);
    // Make sure adding it again doesn't screw anything up
    form.setPrimaryApplicantName(true);
    assertThat(form.primaryApplicantInfoTags())
        .isEqualTo(
            ImmutableSet.of(
                PrimaryApplicantInfoTag.APPLICANT_NAME, PrimaryApplicantInfoTag.APPLICANT_EMAIL));

    form.setPrimaryApplicantName(false);
    // Make sure removing it again doesn't screw anything up
    form.setPrimaryApplicantName(false);
    assertThat(form.primaryApplicantInfoTags())
        .isEqualTo(ImmutableSet.of(PrimaryApplicantInfoTag.APPLICANT_EMAIL));
  }

  @Test
  public void getQuestionImageDescription_defaultsToEmpty() {
    TestQuestionForm form = new TestQuestionForm();
    assertThat(form.getQuestionImageDescription()).isEmpty();
  }

  @Test
  public void setQuestionImageDescription_setsValue() {
    TestQuestionForm form = new TestQuestionForm();
    form.setQuestionImageDescription("Alt text for image");
    assertThat(form.getQuestionImageDescription()).isEqualTo("Alt text for image");
  }

  @Test
  public void constructorWithQuestionDefinition_populatesQuestionImageDescription() {
    QuestionDefinition qd =
        new StaticContentQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("static-q")
                .setDescription("desc")
                .setQuestionText(LocalizedStrings.withDefaultValue("text"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .setLocalizedImageDescription(
                    Optional.of(LocalizedStrings.withDefaultValue("Alt text description")))
                .build());
    TestQuestionForm form = new TestQuestionForm(qd);
    assertThat(form.getQuestionImageDescription()).isEqualTo("Alt text description");
  }

  @Test
  public void constructorWithQuestionDefinition_noImageDescription_defaultsToEmpty() {
    QuestionDefinition qd =
        new StaticContentQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("static-q")
                .setDescription("desc")
                .setQuestionText(LocalizedStrings.withDefaultValue("text"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());
    TestQuestionForm form = new TestQuestionForm(qd);
    assertThat(form.getQuestionImageDescription()).isEmpty();
  }
}
