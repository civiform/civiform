package views.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.DivTag;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class ProgramFormBuilderTest {

  @Test
  @Parameters(method = "provideApplicationStepNumbers")
  public void buildApplicationSteps_buildsFiveApplicationSteps(
      String index, String indexPlusOne, String fieldType, boolean required) {
    DivTag applicationStepsDiv = ProgramFormBuilder.buildApplicationSteps(ImmutableList.of());
    String renderedDiv = applicationStepsDiv.render();

    // field id
    assertThat(renderedDiv).contains("apply-step-" + indexPlusOne + "-" + fieldType);
    // field name
    assertThat(renderedDiv).contains("applicationSteps[" + index + "][" + fieldType + "]");
    if (required) {
      // field label
      assertThat(renderedDiv).contains("Step " + indexPlusOne + " " + fieldType);
    } else {
      assertThat(renderedDiv).contains("Step " + indexPlusOne + " " + fieldType + " (optional)");
    }
  }

  private static ImmutableList<Object[]> provideApplicationStepNumbers() {
    return ImmutableList.<Object[]>of(
        new Object[] {"0", "1", "title", true},
        new Object[] {"0", "1", "description", true},
        new Object[] {"1", "2", "title", false},
        new Object[] {"1", "2", "description", false},
        new Object[] {"2", "3", "title", false},
        new Object[] {"2", "3", "description", false},
        new Object[] {"3", "4", "title", false},
        new Object[] {"3", "4", "description", false},
        new Object[] {"4", "5", "title", false},
        new Object[] {"4", "5", "description", false});
  }

  @Test
  public void buildApplicationSteps_rendersExistingValues() {
    DivTag applicationStepsDiv =
        ProgramFormBuilder.buildApplicationSteps(
            ImmutableList.of(
                Map.of("title", "Step one title", "description", "Step one description")));
    String renderedDiv = applicationStepsDiv.render();

    assertThat(renderedDiv).contains("Step one title");
    assertThat(renderedDiv).contains("Step one description");
  }
}
