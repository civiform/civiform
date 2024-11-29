package views.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.DivTag;
import java.util.Map;
import org.junit.Test;

public class ProgramFormBuilderTest {

  @Test
  public void buildApplicationStepDiv_buildsApplicationStepFormElement() {
    DivTag applicationStepsDiv = ProgramFormBuilder.buildApplicationStepDiv(0, ImmutableList.of());
    String renderedDiv = applicationStepsDiv.render();

    // field id
    assertThat(renderedDiv).contains("apply-step-1-title");
    assertThat(renderedDiv).contains("apply-step-1-description");
    // field name
    assertThat(renderedDiv).contains("applicationSteps[0][title]");
    assertThat(renderedDiv).contains("applicationSteps[0][description]");
    // field label
    assertThat(renderedDiv).contains("Step 1 title");
    assertThat(renderedDiv).contains("Step 1 description");

    DivTag optionalApplicationStepsDiv =
        ProgramFormBuilder.buildApplicationStepDiv(1, ImmutableList.of());
    String renderedOptionalDiv = optionalApplicationStepsDiv.render();

    // field label for divs other than the first one are labeled "optional"
    assertThat(renderedOptionalDiv).contains("Step 2 title (optional)");
    assertThat(renderedOptionalDiv).contains("Step 2 description (optional)");
  }

  @Test
  public void buildApplicationStepDiv_rendersExistingValues() {
    DivTag applicationStepDiv =
        ProgramFormBuilder.buildApplicationStepDiv(
            0,
            ImmutableList.of(
                Map.of("title", "Step one title", "description", "Step one description")));
    String renderedDiv = applicationStepDiv.render();

    assertThat(renderedDiv).contains("Step one title");
    assertThat(renderedDiv).contains("Step one description");
  }
}
