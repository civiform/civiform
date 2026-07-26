package forms.questions;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Http.Request;
import repository.ResetPostgres;
import services.CiviFormError;

/**
 * Proves Play's binding of the parallel score string lists on a real {@link FormFactory}, in
 * particular that a blank trailing score row still binds so the lists stay parallel to the option
 * lists.
 */
public class MultiOptionQuestionFormBindingTest extends ResetPostgres {

  private FormFactory formFactory;

  @Before
  public void setup() {
    formFactory = instanceOf(FormFactory.class);
  }

  @Test
  public void bindFromRequest_bindsParallelScoreLists_includingTrailingBlank() {
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.<String, String>builder()
                    .put("questionName", "binding test")
                    .put("questionDescription", "desc")
                    .put("questionText", "question text")
                    .put("questionHelpText", "help text")
                    .put("options[0]", "one")
                    .put("options[1]", "two")
                    .put("optionAdminNames[0]", "one_admin")
                    .put("optionAdminNames[1]", "two_admin")
                    .put("optionIds[0]", "1")
                    .put("optionIds[1]", "2")
                    .put("optionScores[0]", "10.5")
                    // A trailing blank score row must still bind an entry.
                    .put("optionScores[1]", "")
                    .put("newOptions[0]", "three")
                    .put("newOptionAdminNames[0]", "three_admin")
                    .put("newOptionScores[0]", "")
                    .put("nextAvailableId", "3")
                    .build())
            .build();

    CheckboxQuestionForm form =
        formFactory.form(CheckboxQuestionForm.class).bindFromRequest(request).get();

    assertThat(form.getOptions()).containsExactly("one", "two");
    assertThat(form.getOptionScores()).containsExactly("10.5", "");
    assertThat(form.getNewOptions()).containsExactly("three");
    assertThat(form.getNewOptionScores()).containsExactly("");
    assertThat(form.getOptionScoreErrors()).isEmpty();
  }

  @Test
  public void bindFromRequest_missingScoreFields_bindsEmptyLists() {
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.<String, String>builder()
                    .put("questionName", "binding test")
                    .put("questionDescription", "desc")
                    .put("questionText", "question text")
                    .put("questionHelpText", "help text")
                    .put("options[0]", "one")
                    .put("optionAdminNames[0]", "one_admin")
                    .put("optionIds[0]", "1")
                    .put("nextAvailableId", "2")
                    .build())
            .build();

    CheckboxQuestionForm form =
        formFactory.form(CheckboxQuestionForm.class).bindFromRequest(request).get();

    assertThat(form.getOptionScores()).isEmpty();
    assertThat(form.getNewOptionScores()).isEmpty();
    // Missing score fields bind as empty lists, which fail cardinality validation: the rendered
    // form always submits a score input per option, so this shape is a crafted post and must not
    // silently build unscored options.
    assertThat(form.getOptionScoreErrors())
        .extracting(CiviFormError::message)
        .containsExactly("The number of option scores does not match the number of options");
  }
}
