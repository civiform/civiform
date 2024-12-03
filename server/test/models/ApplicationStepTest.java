package models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ApplicationStepTest {

  @Test
  public void alternativeConstructor_setsDefaultLocaleValues() {
    String title = "Title";
    String description = "Description";
    ApplicationStep applicationStep = new ApplicationStep(title, description);

    assertThat(applicationStep.getTitle().getDefault()).isEqualTo(title);
    assertThat(applicationStep.getDescription().getDefault()).isEqualTo(description);
  }
}
