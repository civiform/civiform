package services;

public class PaginationSpec {

  private final int pageSize;
  private final int currentPage;

  public PaginationSpec(int pageSize, int currentPage) {
    this.pageSize = pageSize;
    this.currentPage = currentPage;
  }

  public int getPageSize() {
    return this.pageSize;
  }

  public int getCurrentPage() {
    return this.currentPage;
  }
}
