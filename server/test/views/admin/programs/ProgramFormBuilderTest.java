package views.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.DivTag;
import java.util.Map;
import org.junit.Test;

public class ProgramFormBuilderTest {

  @Test
  public void buildApplicationStepDiv_buildsApplicationStepFormElement() {
    // Add application step #1 as a required field
    DivTag applicationStepOneRequiredDiv =
        ProgramFormBuilder.buildApplicationStepDiv(0, ImmutableList.of(), /* isDisabled= */ false);
    String renderedDiv = applicationStepOneRequiredDiv.render();

    // field id
    assertThat(renderedDiv).contains("apply-step-1-title");
    assertThat(renderedDiv).contains("apply-step-1-description");
    // field name
    assertThat(renderedDiv).contains("applicationSteps[0][title]");
    assertThat(renderedDiv).contains("applicationSteps[0][description]");
    // field label, which has the required indicator
    assertThat(renderedDiv).contains("Step 1 title").contains("*");
    assertThat(renderedDiv).contains("Step 1 description").contains("*");

    // Add application step #1 as an optional field
    DivTag applicationStepOneOptionalDiv =
        ProgramFormBuilder.buildApplicationStepDiv(0, ImmutableList.of(), /* isDisabled= */ true);
    renderedDiv = applicationStepOneOptionalDiv.render();

    assertThat(renderedDiv).contains("Step 1 title").doesNotContain("*");
    assertThat(renderedDiv).contains("Step 1 description").doesNotContain("*");

    // Add application step #2, which should always be an optional field
    DivTag applicationStepTwoDiv =
        ProgramFormBuilder.buildApplicationStepDiv(1, ImmutableList.of(), /* isDisabled= */ false);
    String renderedOptionalDiv = applicationStepTwoDiv.render();

    assertThat(renderedOptionalDiv).contains("Step 2 title").doesNotContain("*");
    assertThat(renderedOptionalDiv).contains("Step 2 description").doesNotContain("*");
  }

  @Test
  public void buildApplicationStepDiv_rendersExistingValues() {
    DivTag applicationStepDiv =
        ProgramFormBuilder.buildApplicationStepDiv(
            0,
            ImmutableList.of(
                Map.of("title", "Step one title", "description", "Step one description")),
            /* isDisabled= */ false);
    String renderedDiv = applicationStepDiv.render();

    assertThat(renderedDiv).contains("Step one title");
    assertThat(renderedDiv).contains("Step one description");
  }
}
