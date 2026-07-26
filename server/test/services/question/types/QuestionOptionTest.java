package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Test;
import services.LocalizedStrings;
import services.ObjectMapperSingleton;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;

public class QuestionOptionTest {

  private static final ObjectMapper mapper = ObjectMapperSingleton.instance();

  @Test
  public void localize_unsupportedLocale_throws() {
    QuestionOption questionOption =
        QuestionOption.create(1L, "opt1", LocalizedStrings.of(Locale.US, "option 1"));

    Throwable thrown = catchThrowable(() -> questionOption.localize(Locale.CANADA));

    assertThat(thrown).hasMessageContaining("not supported for question option");
  }

  @Test
  public void localizeOrDefault_returnsDefaultForUnsupportedLocale() {
    QuestionOption option =
        QuestionOption.create(1L, "default admin", LocalizedStrings.of(Locale.US, "default"));

    assertThat(option.localizeOrDefault(Locale.CHINESE))
        .isEqualTo(
            LocalizedQuestionOption.create(
                /* id= */ 1L,
                /* order= */ 1L,
                /* adminName= */ "default admin",
                /* optionText= */ "default",
                /* displayInAnswerOptions= */ Optional.empty(),
                /* locale= */ Locale.US));
  }

  @Test
  public void localize_localizes() {
    QuestionOption option =
        QuestionOption.builder()
            .setId(123L)
            .setAdminName("test admin")
            .setOptionText(LocalizedStrings.withDefaultValue("test"))
            .setDisplayOrder(OptionalLong.of(1L))
            .build();

    assertThat(option.localize(LocalizedStrings.DEFAULT_LOCALE))
        .isEqualTo(
            LocalizedQuestionOption.create(
                /* id= */ 123L,
                /* order= */ 1L,
                /* adminName= */ "test admin",
                /* optionText= */ "test",
                /* displayInAnswerOptions= */ Optional.empty(),
                /* locale= */ LocalizedStrings.DEFAULT_LOCALE));
  }

  @Test
  public void create_withDisplayOrder_preservesDisplayOrderWhenLocalized() {
    // This test verifies that Yes/No question options display in the same order
    // after create, edit and in UI
    QuestionOption yesOption =
        QuestionOption.create(
            /* id= */ 1L, // YES = 1 (true)
            /* displayOrder= */ 0L, // Shows first
            /* adminName= */ "yes",
            /* optionText= */ LocalizedStrings.of(Locale.US, "Yes"),
            /* displayInAnswerOptions= */ Optional.of(true));

    QuestionOption noOption =
        QuestionOption.create(
            /* id= */ 0L, // NO = 0 (false)
            /* displayOrder= */ 1L, // Shows second
            /* adminName= */ "no",
            /* optionText= */ LocalizedStrings.of(Locale.US, "No"),
            /* displayInAnswerOptions= */ Optional.of(true));

    QuestionOption notSureOption =
        QuestionOption.create(
            /* id= */ 2L, // NOT_SURE = 2
            /* displayOrder= */ 2L, // Shows third
            /* adminName= */ "not-sure",
            /* optionText= */ LocalizedStrings.of(Locale.US, "Not sure"),
            /* displayInAnswerOptions= */ Optional.of(true));

    QuestionOption maybeOption =
        QuestionOption.create(
            /* id= */ 3L, // MAYBE = 3
            /* displayOrder= */ 3L, // Shows fourth
            /* adminName= */ "maybe",
            /* optionText= */ LocalizedStrings.of(Locale.US, "Maybe"),
            /* displayInAnswerOptions= */ Optional.of(true));

    // Verify that displayOrder is preserved (not replaced with empty)
    assertThat(yesOption.displayOrder()).isEqualTo(OptionalLong.of(0L));
    assertThat(noOption.displayOrder()).isEqualTo(OptionalLong.of(1L));
    assertThat(notSureOption.displayOrder()).isEqualTo(OptionalLong.of(2L));
    assertThat(maybeOption.displayOrder()).isEqualTo(OptionalLong.of(3L));

    // Verify that when localized, the displayOrder is used (not the id)
    LocalizedQuestionOption localizedYes = yesOption.localize(Locale.US);
    LocalizedQuestionOption localizedNo = noOption.localize(Locale.US);
    LocalizedQuestionOption localizedNotSure = notSureOption.localize(Locale.US);
    LocalizedQuestionOption localizedMaybe = maybeOption.localize(Locale.US);

    // Critical assertion: displayOrder should be used for order, not id
    assertThat(localizedYes.order()).isEqualTo(0L); // displayOrder=0, not id=1
    assertThat(localizedNo.order()).isEqualTo(1L); // displayOrder=1, not id=0
    assertThat(localizedNotSure.order()).isEqualTo(2L); // displayOrder=2, same as id=2
    assertThat(localizedMaybe.order()).isEqualTo(3L); // displayOrder=3, same as id=3
  }

  @Test
  public void serde_roundTrip_preservesScore() throws Exception {
    QuestionOption option =
        QuestionOption.create(
            /* id= */ 1L,
            /* displayOrder= */ 0L,
            /* adminName= */ "scored",
            /* optionText= */ LocalizedStrings.of(Locale.US, "scored option"),
            /* displayInAnswerOptions= */ Optional.of(true),
            /* score= */ Optional.of(5));

    QuestionOption deserialized =
        mapper.readValue(mapper.writeValueAsString(option), QuestionOption.class);

    assertThat(deserialized).isEqualTo(option);
    assertThat(deserialized.score()).isEqualTo(Optional.of(5));
  }

  @Test
  public void serde_roundTrip_preservesNegativeScore() throws Exception {
    QuestionOption option =
        QuestionOption.create(
            /* id= */ 1L,
            /* displayOrder= */ 0L,
            /* adminName= */ "negative",
            /* optionText= */ LocalizedStrings.of(Locale.US, "negative option"),
            /* displayInAnswerOptions= */ Optional.of(true),
            /* score= */ Optional.of(-3));

    QuestionOption deserialized =
        mapper.readValue(mapper.writeValueAsString(option), QuestionOption.class);

    assertThat(deserialized.score()).isEqualTo(Optional.of(-3));
  }

  @Test
  public void serde_roundTrip_absentScore() throws Exception {
    QuestionOption option =
        QuestionOption.create(
            /* id= */ 1L,
            /* displayOrder= */ 0L,
            /* adminName= */ "unscored",
            /* optionText= */ LocalizedStrings.of(Locale.US, "unscored option"),
            /* displayInAnswerOptions= */ Optional.of(true));

    QuestionOption deserialized =
        mapper.readValue(mapper.writeValueAsString(option), QuestionOption.class);

    assertThat(deserialized).isEqualTo(option);
    assertThat(deserialized.score()).isEmpty();
  }

  @Test
  public void deserialize_storedJsonWithoutScoreField_readsEmptyScore() throws Exception {
    // Simulates a pre-feature stored row: no `score` property at all.
    QuestionOption option =
        QuestionOption.create(
            /* id= */ 1L,
            /* displayOrder= */ 0L,
            /* adminName= */ "pre-feature",
            /* optionText= */ LocalizedStrings.of(Locale.US, "pre-feature option"),
            /* displayInAnswerOptions= */ Optional.of(true));
    ObjectNode node = (ObjectNode) mapper.readTree(mapper.writeValueAsString(option));
    node.remove("score");

    QuestionOption deserialized = mapper.readValue(node.toString(), QuestionOption.class);

    assertThat(deserialized).isEqualTo(option);
    assertThat(deserialized.score()).isEmpty();
  }

  @Test
  public void deserialize_explicitNullScore_readsEmptyScore() throws Exception {
    QuestionOption option =
        QuestionOption.create(
            /* id= */ 1L,
            /* displayOrder= */ 0L,
            /* adminName= */ "null-score",
            /* optionText= */ LocalizedStrings.of(Locale.US, "null score option"),
            /* displayInAnswerOptions= */ Optional.of(true));
    ObjectNode node = (ObjectNode) mapper.readTree(mapper.writeValueAsString(option));
    node.putNull("score");

    QuestionOption deserialized = mapper.readValue(node.toString(), QuestionOption.class);

    assertThat(deserialized.score()).isEmpty();
  }

  @Test
  public void deserialize_legacyOptionTextJson_preservesScore() throws Exception {
    // Legacy rows (pre-May 2021) have `optionText` instead of `localizedOptionText`.
    String legacyJson =
        "{\"id\": 1, \"displayOrder\": 2, \"adminName\": \"legacy\","
            + " \"optionText\": {\"en_US\": \"legacy option\"}, \"score\": 7}";

    QuestionOption deserialized = mapper.readValue(legacyJson, QuestionOption.class);

    assertThat(deserialized.optionText().get(Locale.US)).isEqualTo("legacy option");
    assertThat(deserialized.score()).isEqualTo(Optional.of(7));
  }

  @Test
  public void localize_neverExposesScore() {
    // D6: the applicant-facing LocalizedQuestionOption must never carry the score. This guards
    // against a future accessor being added and picked up by applicant rendering.
    QuestionOption scored =
        QuestionOption.create(
            /* id= */ 1L,
            /* displayOrder= */ 0L,
            /* adminName= */ "invisible",
            /* optionText= */ LocalizedStrings.of(Locale.US, "option"),
            /* displayInAnswerOptions= */ Optional.of(true),
            /* score= */ Optional.of(987654));

    LocalizedQuestionOption localized = scored.localizeOrDefault(Locale.US);

    assertThat(
            java.util.Arrays.stream(LocalizedQuestionOption.class.getMethods())
                .map(java.lang.reflect.Method::getName))
        .noneMatch(name -> name.toLowerCase(Locale.ROOT).contains("score"));
    assertThat(localized.toString()).doesNotContain("987654");
  }

  @Test
  public void jsonCreator_legacyBranch_preservesScore() {
    QuestionOption option =
        QuestionOption.jsonCreator(
            /* id= */ 1L,
            /* displayOrder= */ 2L,
            /* adminName= */ "legacy",
            /* localizedOptionText= */ null,
            /* legacyOptionText= */ ImmutableMap.of(Locale.US, "legacy option"),
            /* displayInAnswerOptions= */ Optional.empty(),
            /* score= */ Optional.of(4));

    assertThat(option.score()).isEqualTo(Optional.of(4));
  }
}
