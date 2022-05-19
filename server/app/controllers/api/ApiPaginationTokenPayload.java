package controllers.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * The semantic contents of a pagination token. This class holds the state needed to fetch the next
 * page of results from a paginated API.
 */
public class ApiPaginationTokenPayload {
  private final PageSpec pageSpec;
  private final ImmutableMap<String, String> requestSpec;

  public ApiPaginationTokenPayload(
      @JsonProperty("pageSpec") PageSpec pageSpec,
      @JsonProperty("requestSpec") Map<String, String> requestSpec) {
    this.pageSpec = pageSpec;
    this.requestSpec = ImmutableMap.copyOf(requestSpec);
  }

  public PageSpec getPageSpec() {
    return this.pageSpec;
  }

  /** A map of the query parameters for the paginated request. */
  public ImmutableMap<String, String> getRequestSpec() {
    return this.requestSpec;
  }

  /** Holds the generic state for defining a page of results. */
  public static class PageSpec {
    private final String offsetIdentifier;
    private final int pageSize;

    public PageSpec(
        @JsonProperty("offsetIdentifier") String offsetIdentifier,
        @JsonProperty("pageSize") int pageSize) {
      this.offsetIdentifier = offsetIdentifier;
      this.pageSize = pageSize;
    }

    /**
     * The offset identifier identifies the last item of the previous page of results using its sort
     * order attribute.
     */
    public String getOffsetIdentifier() {
      return offsetIdentifier;
    }

    public int getPageSize() {
      return pageSize;
    }
  }
}
