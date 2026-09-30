package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import services.question.types.QuestionType;

public class ScalarTest {

  @Test
  public void getScoreScalarKeys_areTheStoredJsonKeys() {
    assertThat(Scalar.getScoreScalarKeys()).containsExactly("score", "scores", "total_score");
  }

  @Test
  public void getScalars_neverIncludesScoreOrMetadataScalars() throws Exception {
    ImmutableSet<Scalar> excluded =
        ImmutableSet.<Scalar>builder()
            .addAll(Scalar.getScoreScalars())
            .addAll(Scalar.getMetadataScalars())
            .build();

    for (QuestionType type : QuestionType.values()) {
      if (type == QuestionType.ENUMERATOR || type == QuestionType.NULL_QUESTION) {
        continue;
      }
      assertThat(Scalar.getScalars(type)).doesNotContainAnyElementsOf(excluded);
    }
  }
}
