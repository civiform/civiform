package mapping.admin.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.Test;
import views.admin.shared.LegacyPagination;
import views.admin.shared.LegacyPagination.Item;

public final class LegacyPaginationMapperTest {

  private final LegacyPaginationMapper mapper = new LegacyPaginationMapper();

  private static String urlForPage(int page) {
    return "/list?page=" + page;
  }

  @Test
  public void map_firstPage_hasNoPreviousLink() {
    LegacyPagination pagination =
        mapper.map(/* page= */ 1, /* pageCount= */ 3, LegacyPaginationMapperTest::urlForPage);

    assertThat(pagination.getPreviousPageUrl()).isNull();
    assertThat(pagination.getNextPageUrl()).isEqualTo("/list?page=2");
  }

  @Test
  public void map_lastPage_hasNoNextLink() {
    LegacyPagination pagination =
        mapper.map(/* page= */ 3, /* pageCount= */ 3, LegacyPaginationMapperTest::urlForPage);

    assertThat(pagination.getPreviousPageUrl()).isEqualTo("/list?page=2");
    assertThat(pagination.getNextPageUrl()).isNull();
  }

  @Test
  public void map_singlePage_hasNoArrows() {
    LegacyPagination pagination =
        mapper.map(/* page= */ 1, /* pageCount= */ 1, LegacyPaginationMapperTest::urlForPage);

    assertThat(pagination.getPreviousPageUrl()).isNull();
    assertThat(pagination.getNextPageUrl()).isNull();
    assertThat(pagination.getItems()).containsExactly(Item.page(1, "/list?page=1", true));
  }

  @Test
  public void map_marksCurrentPage() {
    LegacyPagination pagination =
        mapper.map(/* page= */ 2, /* pageCount= */ 3, LegacyPaginationMapperTest::urlForPage);

    assertThat(pagination.getItems())
        .extracting(Item::getPageNumber, Item::isCurrent)
        .containsExactly(tuple(1, false), tuple(2, true), tuple(3, false));
  }

  @Test
  public void map_sevenPagesOrFewer_showsEveryPage() {
    LegacyPagination pagination =
        mapper.map(/* page= */ 4, /* pageCount= */ 7, LegacyPaginationMapperTest::urlForPage);

    assertThat(pagination.getItems())
        .extracting(Item::getPageNumber)
        .containsExactly(1, 2, 3, 4, 5, 6, 7);
    assertThat(pagination.getItems()).noneMatch(Item::isEllipsis);
  }

  @Test
  public void map_currentPageInFirstFour_showsEllipsisOnRight() {
    LegacyPagination pagination =
        mapper.map(/* page= */ 2, /* pageCount= */ 8, LegacyPaginationMapperTest::urlForPage);

    assertThat(pagination.getItems())
        .containsExactly(
            Item.page(1, "/list?page=1", false),
            Item.page(2, "/list?page=2", true),
            Item.page(3, "/list?page=3", false),
            Item.page(4, "/list?page=4", false),
            Item.page(5, "/list?page=5", false),
            Item.ellipsis(),
            Item.page(8, "/list?page=8", false));
  }

  @Test
  public void map_currentPageInMiddle_showsEllipsisOnBothSides() {
    LegacyPagination pagination =
        mapper.map(/* page= */ 5, /* pageCount= */ 10, LegacyPaginationMapperTest::urlForPage);

    assertThat(pagination.getItems())
        .containsExactly(
            Item.page(1, "/list?page=1", false),
            Item.ellipsis(),
            Item.page(4, "/list?page=4", false),
            Item.page(5, "/list?page=5", true),
            Item.page(6, "/list?page=6", false),
            Item.ellipsis(),
            Item.page(10, "/list?page=10", false));
  }

  @Test
  public void map_currentPageInLastFour_showsEllipsisOnLeft() {
    LegacyPagination pagination =
        mapper.map(/* page= */ 8, /* pageCount= */ 10, LegacyPaginationMapperTest::urlForPage);

    assertThat(pagination.getItems())
        .containsExactly(
            Item.page(1, "/list?page=1", false),
            Item.ellipsis(),
            Item.page(6, "/list?page=6", false),
            Item.page(7, "/list?page=7", false),
            Item.page(8, "/list?page=8", true),
            Item.page(9, "/list?page=9", false),
            Item.page(10, "/list?page=10", false));
  }

  @Test
  public void map_pagePastLastPage_rendersLikeLegacyWithoutCurrentMarker() {
    // The legacy view never clamped the requested page; a stale link must not 500.
    LegacyPagination pagination =
        mapper.map(/* page= */ 4, /* pageCount= */ 3, LegacyPaginationMapperTest::urlForPage);

    assertThat(pagination.getPreviousPageUrl()).isEqualTo("/list?page=3");
    assertThat(pagination.getNextPageUrl()).isEqualTo("/list?page=5");
    assertThat(pagination.getItems())
        .extracting(Item::getPageNumber, Item::isCurrent)
        .containsExactly(tuple(1, false), tuple(2, false), tuple(3, false));
  }

  @Test
  public void map_pageBelowOne_throws() {
    assertThatThrownBy(
            () ->
                mapper.map(
                    /* page= */ 0, /* pageCount= */ 3, LegacyPaginationMapperTest::urlForPage))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
