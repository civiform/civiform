package services;

import com.google.common.collect.ImmutableList;

// TODO(#8841): Move this class into the pagination subpackage.

/** PaginationInfo is a generic class for pagination. */
public class PaginationInfo<T> {
  public ImmutableList<T> getAllItems() {
    return allItems;
  }

  public ImmutableList<T> getPageItems() {
    return pageItems;
  }

  public int getPage() {
    return page;
  }

  public int getPageCount() {
    return pageCount;
  }

  private final ImmutableList<T> allItems;
  private final ImmutableList<T> pageItems;
  private final int page;
  private final int pageCount;

  private PaginationInfo(
      ImmutableList<T> allItems, ImmutableList<T> pageItems, int page, int pageCount) {
    this.allItems = allItems;
    this.pageItems = pageItems;
    this.page = page;
    this.pageCount = pageCount;
  }

  public static <V> PaginationInfo<V> paginate(ImmutableList<V> allItems, int pageSize, int page) {
    int endOfListIndex = page * pageSize;
    int totalPageCount = (int) Math.ceil((double) allItems.size() / pageSize);

    if (allItems.size() <= endOfListIndex) {
      endOfListIndex = allItems.size();
    }

    ImmutableList<V> pageItems;

    if (allItems.size() <= (page - 1) * pageSize) {
      pageItems = ImmutableList.of();

      if (allItems.size() == 0) {
        // Display 1 page (which is empty)
        totalPageCount = 1;
      } else {
        // If for some reason we're way past the end of the list, make sure the "previous"
        // button goes to the end of the list.
        page = Math.floorDiv(allItems.size(), pageSize) + 2;
      }
    } else {
      pageItems = allItems.subList((page - 1) * pageSize, endOfListIndex);
    }

    return new PaginationInfo<V>(allItems, pageItems, page, totalPageCount);
  }
}
