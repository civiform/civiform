package services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PathTest {

  @Test
  public void pathWithPrefix_removesOnlyPrefix() {
    String path = "favorite.color";
    assertThat(Path.create("$.applicant." + path).path()).isEqualTo("applicant.favorite.color");
    assertThat(Path.create(path).path()).isEqualTo(path);
  }

  @Test
  public void segments_emptyPath() {
    assertThat(Path.create("").segments()).isEmpty();
  }

  @Test
  public void segments_oneSegment() {
    assertThat(Path.create("applicant").segments()).containsExactly("applicant");
  }

  @Test
  public void segments() {
    Path path = Path.create("my.super.long.path");
    assertThat(path.segments()).containsExactly("my", "super", "long", "path");
  }

  @Test
  public void parentPath_emptyPath() {
    Path path = Path.create("");
    assertThat(path.parentPath()).isEqualTo(Path.create(""));
  }

  @Test
  public void parentPath_oneSegment() {
    Path path = Path.create("applicant");
    assertThat(path.parentPath()).isEqualTo(Path.create(""));
  }

  @Test
  public void parentPath() {
    Path path = Path.create("one.two.three.me");
    assertThat(path.parentPath()).isEqualTo(Path.create("one.two.three"));
  }

  @Test
  public void keyName_emptyPath() {
    Path path = Path.create("");
    assertThat(path.keyName()).isEqualTo("");
  }

  @Test
  public void keyName_oneSegment() {
    Path path = Path.create("applicant");
    assertThat(path.keyName()).isEqualTo("applicant");
  }

  @Test
  public void keyName() {
    Path path = Path.create("one.two.name");
    assertThat(path.keyName()).isEqualTo("name");
  }

  @Test
  public void parentPaths_emptyPath() {
    Path path = Path.create("");
    assertThat(path.parentPaths()).isEmpty();
  }

  @Test
  public void parentPaths_oneSegment() {
    Path path = Path.create("applicant");
    assertThat(path.parentPaths()).isEmpty();
  }

  @Test
  public void parentPaths() {
    Path path = Path.create("animals.favorites.dog");
    assertThat(path.parentPaths())
        .containsExactly(Path.create("animals"), Path.create("animals.favorites"));
  }

  @Test
  public void pathBuilder() {
    Path path = Path.builder().setPath("applicant.my.path").build();
    assertThat(path.path()).isEqualTo("applicant.my.path");

    path = path.toBuilder().append("another").build();
    assertThat(path.path()).isEqualTo("applicant.my.path.another");

    path = path.toBuilder().append("part").build();
    assertThat(path.path()).isEqualTo("applicant.my.path.another.part");

    path = path.toBuilder().setPath("something.new").build();
    assertThat(path.path()).isEqualTo("something.new");
  }
}
