package views.admin;

import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.QuestionDefinitionConfig;

public class QuestionCardTest {

  private static QuestionOption option(long id, String adminName, Optional<Double> score) {
    return QuestionOption.create(
        id,
        /* displayOrder= */ id,
        adminName,
        LocalizedStrings.of(Locale.US, "Option " + adminName),
        /* displayInAnswerOptions= */ Optional.of(true),
        score);
  }

  private static MultiOptionQuestionDefinition dropdown(QuestionOption... options) {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("scored-dropdown")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "Pick one"))
            .setQuestionHelpText(LocalizedStrings.empty())
            .setId(1L)
            .build();
    return new MultiOptionQuestionDefinition(
        config, ImmutableList.copyOf(options), MultiOptionQuestionType.DROPDOWN);
  }

  private static String renderForImport(MultiOptionQuestionDefinition question) {
    return QuestionCard.renderForImport(
            question, div("New Question"), /* maybeDuplicateHandlingForImport= */ Optional.empty())
        .render();
  }

  @Test
  public void renderForImport_scoredOptions_showsFormattedScores() {
    String html =
        renderForImport(
            dropdown(option(1L, "a", Optional.of(10.5)), option(2L, "b", Optional.of(0.0))));

    assertThat(html).contains("<span>Option a</span>");
    assertThat(html).contains("<span class=\"ml-1 text-sm text-gray-600\">(Score: 10.5)</span>");
    assertThat(html).contains("<span>Option b</span>");
    assertThat(html).contains("<span class=\"ml-1 text-sm text-gray-600\">(Score: 0)</span>");
  }

  @Test
  public void renderForImport_unscoredOptions_showsNoScores() {
    String html =
        renderForImport(
            dropdown(option(1L, "a", Optional.empty()), option(2L, "b", Optional.empty())));

    assertThat(html).contains("<span>Option a</span>");
    assertThat(html).contains("<span>Option b</span>");
    assertThat(html).doesNotContain("Score");
  }
}
