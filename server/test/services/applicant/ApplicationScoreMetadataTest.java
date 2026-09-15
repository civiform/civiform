package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import services.Path;

public class ApplicationScoreMetadataTest {

  @Test
  public void isReservedScoreKey_matchesOnlyScoreKeyNames() {
    assertThat(ApplicationScoreMetadata.isReservedScoreKey("score")).isTrue();
    assertThat(ApplicationScoreMetadata.isReservedScoreKey("scores")).isTrue();
    assertThat(ApplicationScoreMetadata.isReservedScoreKey("total_score")).isTrue();
    assertThat(ApplicationScoreMetadata.isReservedScoreKey("selection")).isFalse();
    assertThat(ApplicationScoreMetadata.isReservedScoreKey("selections")).isFalse();
    assertThat(ApplicationScoreMetadata.isReservedScoreKey("text")).isFalse();
  }

  @Test
  public void totalScorePath_isTheRootLevelSiblingOfApplicant() {
    // Outside the applicant tree entirely, so it cannot collide with a question admin-named
    // "total_score" (which lives at applicant.total_score).
    assertThat(ApplicationScoreMetadata.totalScorePath().toString()).isEqualTo("total_score");
  }

  @Test
  public void scorePaths_areSiblingsOfTheAnswerScalar() {
    Path questionPath = Path.create("applicant.favorite_color");

    assertThat(ApplicationScoreMetadata.scorePath(questionPath).toString())
        .isEqualTo("applicant.favorite_color.score");
    assertThat(ApplicationScoreMetadata.scoresPath(questionPath).toString())
        .isEqualTo("applicant.favorite_color.scores");
  }

  @Test
  public void scorePaths_repeatedContextualizedPath() {
    Path questionPath = Path.create("applicant.household_members[2].favorite_color");

    assertThat(ApplicationScoreMetadata.scorePath(questionPath).toString())
        .isEqualTo("applicant.household_members[2].favorite_color.score");
  }

  @Test
  public void scorePaths_nestedRepeatedContextualizedPath() {
    Path questionPath =
        Path.create("applicant.household_members[1].household_members_jobs[0].days_worked");

    assertThat(ApplicationScoreMetadata.scoresPath(questionPath).toString())
        .isEqualTo("applicant.household_members[1].household_members_jobs[0].days_worked.scores");
  }

  @Test
  public void scorePaths_rejectPathsNotRootedAtApplicant() {
    assertThatThrownBy(
            () -> ApplicationScoreMetadata.scorePath(Path.create("other.favorite_color")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ApplicationScoreMetadata.scoresPath(Path.empty()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void isMetadataPath_matchesOnlyTheMetadataRoot() {
    assertThat(ApplicationScoreMetadata.isMetadataPath(Path.create("application_metadata")))
        .isTrue();
    assertThat(
            ApplicationScoreMetadata.isMetadataPath(
                Path.create("application_metadata.option_scoring.total_score")))
        .isTrue();
    assertThat(ApplicationScoreMetadata.isMetadataPath(Path.create("application_metadata[0].x")))
        .isTrue();
    assertThat(ApplicationScoreMetadata.isMetadataPath(Path.create("applicant.total_score")))
        .isFalse();
    assertThat(ApplicationScoreMetadata.isMetadataPath(Path.empty())).isFalse();
  }
}
