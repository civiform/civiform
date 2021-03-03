package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PathTest {

  @Test
  public void pathWithPrefix_removesOnlyPrefixes() {
    String expectedPath = "favorite.color";
    assertThat(Path.create("$.applicant." + expectedPath).path()).isEqualTo(expectedPath);
    assertThat(Path.create("$.metadata." + expectedPath).path()).isEqualTo(expectedPath);
    assertThat(Path.create("applicant." + expectedPath).path()).isEqualTo(expectedPath);
    assertThat(Path.create("metadata." + expectedPath).path()).isEqualTo(expectedPath);
    assertThat(Path.create(expectedPath).path()).isEqualTo(expectedPath);
  }

  @Test
  public void withApplicantPrefix() {
    assertThat(Path.create("favorite.color").withApplicantPrefix())
        .isEqualTo("applicant.favorite.color");
  }

  @Test
  public void withMetadataPrefix() {
    assertThat(Path.create("first.name").withMetadataPrefix()).isEqualTo("metadata.first.name");
  }
}
