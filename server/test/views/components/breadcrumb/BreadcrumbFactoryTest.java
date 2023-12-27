package views.components.breadcrumb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.NavTag;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import views.components.Icons;

public class BreadcrumbFactoryTest {

  private final BreadcrumbFactory breadcrumbFactory = new BreadcrumbFactory();

  @Test
  public void buildBreadcrumb_emptyList_throws() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> breadcrumbFactory.buildBreadcrumb(ImmutableList.of()));
  }

  @Test
  public void buildBreadcrumb_hasNavWithNestedOl() {
    NavTag breadcrumb =
        breadcrumbFactory.buildBreadcrumb(
            ImmutableList.of(
                BreadcrumbItem.create("Fake text", /* link= */ null, /* icon= */ null)));

    assertThat(breadcrumb.render()).containsPattern("<nav.*><ol.*>.*</ol></nav>");
  }

  @Test
  public void buildBreadcrumb_singleItem_singleLi() {
    NavTag breadcrumb =
        breadcrumbFactory.buildBreadcrumb(
            ImmutableList.of(
                BreadcrumbItem.create("Fake text", /* link= */ null, /* icon= */ null)));

    int numLiTags = StringUtils.countMatches(breadcrumb.render(), "<li");
    assertThat(numLiTags).isEqualTo(1);
  }

  @Test
  public void buildBreadcrumb_hasTextInLi() {
    NavTag breadcrumb =
        breadcrumbFactory.buildBreadcrumb(
            ImmutableList.of(
                BreadcrumbItem.create("Fake text", /* link= */ null, /* icon= */ null)));

    assertThat(breadcrumb.render()).containsPattern("<li.*>Fake text.*</li>");
  }

  @Test
  public void buildBreadcrumb_multipleItems_multipleLiInOrder() {
    NavTag breadcrumb =
        breadcrumbFactory.buildBreadcrumb(
            ImmutableList.of(
                BreadcrumbItem.create("Fake text 1", /* link= */ null, /* icon= */ null),
                BreadcrumbItem.create(
                    "Fake text 2", /* link= */ "fake.link", /* icon= */ Icons.ADD),
                BreadcrumbItem.create("Fake text 3", /* link= */ null, /* icon= */ Icons.DELETE)));

    int numLiTags = StringUtils.countMatches(breadcrumb.render(), "<li");
    assertThat(numLiTags).isEqualTo(3);
    assertThat(breadcrumb.render())
        .containsPattern(
            "<li.*>Fake text 1<.*</li><li.*>Fake text 2<.*</li><li.*>Fake text 3<.*</li>");
  }

  @Test
  public void buildBreadcrumb_noLink_noATag() {
    NavTag breadcrumb =
        breadcrumbFactory.buildBreadcrumb(
            ImmutableList.of(
                BreadcrumbItem.create("Fake text", /* link= */ null, /* icon= */ null)));

    assertThat(breadcrumb.render()).doesNotContain("<a");
  }

  @Test
  public void buildBreadcrumb_link_hasATagWithHref() {
    NavTag breadcrumb =
        breadcrumbFactory.buildBreadcrumb(
            ImmutableList.of(
                BreadcrumbItem.create("Fake text", /* link= */ "fake.link", /* icon= */ null)));

    assertThat(breadcrumb.render()).containsPattern("<a.*href=\"fake.link");
  }

  @Test
  public void buildBreadcrumb_noIcon_noSvgTag() {
    NavTag breadcrumb =
        breadcrumbFactory.buildBreadcrumb(
            ImmutableList.of(
                BreadcrumbItem.create("Fake text", /* link= */ null, /* icon= */ null)));

    assertThat(breadcrumb.render()).doesNotContain("<svg");
  }

  @Test
  public void buildBreadcrumb_icon_hasSvgTagWithPath() {
    NavTag breadcrumb =
        breadcrumbFactory.buildBreadcrumb(
            ImmutableList.of(
                BreadcrumbItem.create("Fake text", /* link= */ null, /* icon= */ Icons.ADD)));

    assertThat(breadcrumb.render()).contains("<svg");
    assertThat(breadcrumb.render()).contains(Icons.ADD.path);
  }

  @Test
  public void buildBreadcrumb_linkAndIcon_hasATagWithNestedSvgTag() {
    NavTag breadcrumb =
        breadcrumbFactory.buildBreadcrumb(
            ImmutableList.of(
                BreadcrumbItem.create(
                    "Fake text", /* link= */ "fake.link", /* icon= */ Icons.ADD)));

    assertThat(breadcrumb.render()).containsPattern("<a.*><svg.*>.*</svg>.*</a>");
  }
}
