package views.questiontypes;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.common.collect.ImmutableSet;
import j2html.tags.Tag;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import repository.WithPostgresContainer;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.types.IDQuestionDefinition;
import services.question.types.IDQuestionDefinition.IDValidationPredicates;
import support.QuestionAnswerer;

public class IDQuestionRendererTest extends WithPostgresContainer {
    private static final IDQuestionDefinition ID_QUESTION_DEFINITION =
            new IDQuestionDefinition(
                    OptionalLong.of(1),
                    "question name",
                    Optional.empty(),
                    "description",
                    LocalizedStrings.of(Locale.US, "question?"),
                    LocalizedStrings.of(Locale.US, "help text"),
                    IDValidationPredicates.create(2, 3));

    private final ApplicantData applicantData = new ApplicantData();

    private ApplicantQuestion question;
    private Messages messages;
    private ApplicantQuestionRendererParams params;
    private IDQuestionRenderer renderer;

    @Before
    public void setUp() {
        question = new ApplicantQuestion(ID_QUESTION_DEFINITION, applicantData, Optional.empty());
        messages = instanceOf(MessagesApi.class).preferred(ImmutableSet.of(Lang.defaultLang()));
        params = ApplicantQuestionRendererParams.sample(messages);
        renderer = new IDQuestionRenderer(question);
    }

    @Test
    public void render_withoutQuestionErrors() {
        Tag result = renderer.render(params);

        assertThat(result.render()).doesNotContain("Must contain at");
    }

    @Test
    public void render_withMinLengthError() {
        QuestionAnswerer.answerIDQuestion(applicantData, question.getContextualizedPath(), "1");

        Tag result = renderer.render(params);

        assertThat(result.render()).contains("Must contain at least 2 characters.");
    }

    @Test
    public void render_withMaxLengthError() {
        QuestionAnswerer.answerIDQuestion(applicantData, question.getContextualizedPath(), "1234");

        Tag result = renderer.render(params);

        assertThat(result.render()).contains("Must contain at most 3 characters.");
    }

    @Test
    public void render_withInvalidCharactersError() {
        QuestionAnswerer.answerIDQuestion(applicantData, question.getContextualizedPath(), "ab");

        Tag result = renderer.render(params);

        assertThat(result.render()).contains("Must contain only numbers.");
    }
}
