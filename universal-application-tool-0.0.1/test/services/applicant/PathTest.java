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

  @Test
  public void segments() {
    Path path = Path.create("my.super.long.path");
    assertThat(path.segments()).containsExactly("my", "super", "long", "path");
  }

  @Test
  public void parentPath() {
    Path path = Path.create("one.two.three.me");
    assertThat(path.parentPath()).isEqualTo("one.two.three");
  }

  @Test
  public void keyName() {
    Path path = Path.create("one.two.name");
    assertThat(path.keyName()).isEqualTo("name");
  }

  @Test
  public void fullPathSegments() {
    Path path = Path.create("personality.favorites.color");
    assertThat(path.fullPathSegments()).containsExactly(Path.create("personality"), Path.create("personality.favorites"), Path.create("personality.favorites.color"));
  }

  @Test
  public void parentSegments() {
    Path path = Path.create("animals.favorites.dog");
    assertThat(path.parentSegments()).containsExactly(Path.create("animals"), Path.create("animals.favorites"));
  }
}
