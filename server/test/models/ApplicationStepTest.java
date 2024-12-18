package models;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
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

  @Test
  public void setNewTitleAndDescriptionTranslations() {
    String title = "Title";
    String description = "Description";
    String frenchTitle = "French title";
    String frenchDescription = "French description";

    ApplicationStep applicationStep = new ApplicationStep(title, description);
    ApplicationStep updatedApplicationStep =
        applicationStep
            .setNewTitleTranslation(Locale.FRENCH, frenchTitle)
            .setNewDescriptionTranslation(Locale.FRENCH, frenchDescription);

    assertThat(updatedApplicationStep.getTitle().getOrDefault(Locale.FRENCH))
        .isEqualTo(frenchTitle);
    assertThat(updatedApplicationStep.getDescription().getOrDefault(Locale.FRENCH))
        .isEqualTo(frenchDescription);
    // default (en-US) translations remain unchanged
    assertThat(applicationStep.getTitle().getDefault()).isEqualTo(title);
    assertThat(applicationStep.getDescription().getDefault()).isEqualTo(description);
  }
}
