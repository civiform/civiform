package controllers.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class ApiPaginationTokenPayload {
  private PageSpec pageSpec;
  private ImmutableMap<String, String> requestSpec;

  public ApiPaginationTokenPayload(
      @JsonProperty("pageSpec") PageSpec pageSpec,
      @JsonProperty("requestSpec") Map<String, String> requestSpec) {
    this.pageSpec = pageSpec;
    this.requestSpec = ImmutableMap.copyOf(requestSpec);
  }

  public PageSpec getPageSpec() {
    return this.pageSpec;
  }

  public ImmutableMap<String, String> getRequestSpec() {
    return this.requestSpec;
  }

  public void setPageSpec(PageSpec pageSpec) {
    this.pageSpec = pageSpec;
  }

  public void setRequestSpec(ImmutableMap<String, String> requestSpec) {
    this.requestSpec = requestSpec;
  }

  public static class PageSpec {
    private String offsetIdentifier;
    private int pageSize;

    public PageSpec(
        @JsonProperty("offsetIdentifier") String offsetIdentifier,
        @JsonProperty("pageSize") int pageSize) {
      this.offsetIdentifier = offsetIdentifier;
      this.pageSize = pageSize;
    }

    public String getOffsetIdentifier() {
      return offsetIdentifier;
    }

    public int getPageSize() {
      return pageSize;
    }

    public void setOffsetIdentifier(String offsetIdentifier) {
      this.offsetIdentifier = offsetIdentifier;
    }

    public void setPageSize(int pageSize) {
      this.pageSize = pageSize;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PageSpec)) return false;
      PageSpec pageSpec = (PageSpec) o;
      return pageSize == pageSpec.pageSize
          && Objects.equal(offsetIdentifier, pageSpec.offsetIdentifier);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(offsetIdentifier, pageSize);
    }
  }
}
