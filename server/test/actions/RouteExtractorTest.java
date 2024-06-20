package actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public final class RouteExtractorTest {
  @Test
  public void successfully_extracts_route_parameters() {
    String routePattern = "/programs/$programId<[^/]+>/blocks/$blockId<[^/]+>/edit";
    String path = "/programs/1/blocks/2/edit";
    RouteExtractor routeExtractor = new RouteExtractor(routePattern, path);

    assertThat(routeExtractor.size()).isEqualTo(2);
    assertThat(routeExtractor.getLongValue("programId")).isEqualTo(1);
    assertThat(routeExtractor.getLongValue("blockId")).isEqualTo(2);
  }

  @Test
  public void has_no_route_parameters_configured() {
    String routePattern = "/programs/path/edit";
    String path = "/programs/path/edit";
    RouteExtractor routeExtractor = new RouteExtractor(routePattern, path);

    assertThat(routeExtractor.size()).isEqualTo(0);
  }

  @Test
  public void asking_for_route_parameter_that_does_not_exist() {
    String routePattern = "/programs/$programId<[^/]+>/blocks/$blockId<[^/]+>/edit";
    String path = "/programs/1/blocks/2/edit";
    RouteExtractor routeExtractor = new RouteExtractor(routePattern, path);

    assertThatThrownBy(() -> routeExtractor.getLongValue("unexpectedId"));
  }
}
