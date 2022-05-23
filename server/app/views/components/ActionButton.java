package views.components;

import static j2html.TagCreator.button;
import static j2html.TagCreator.span;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import j2html.tags.Tag;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

@AutoValue
public abstract class ActionButton {

  private static final int SVG_PIXEL_SIZE = 18;

  private static final ImmutableSet<String> BASE_STYLES =
      ImmutableSet.of(
          Styles.FLEX,
          Styles.ITEMS_CENTER,
          Styles.MY_2,
          Styles.ROUNDED_FULL,
          Styles.FONT_MEDIUM,
          StyleUtils.focus(Styles.OUTLINE_NONE, Styles.RING_2));

  private static final ImmutableSet<String> PRIMARY_STYLES =
      ImmutableSet.copyOf(
          Sets.union(BASE_STYLES, ImmutableSet.of(BaseStyles.BG_SEATTLE_BLUE, Styles.TEXT_WHITE)));

  private static final ImmutableSet<String> SECONDARY_STYLES =
      ImmutableSet.copyOf(
          Sets.union(
              BASE_STYLES,
              ImmutableSet.of(
                  Styles.BORDER,
                  BaseStyles.BORDER_SEATTLE_BLUE,
                  Styles.BG_WHITE,
                  BaseStyles.TEXT_SEATTLE_BLUE)));

  private static final ImmutableSet<String> TERTIARY_STYLES =
      ImmutableSet.copyOf(
          Sets.union(
              BASE_STYLES,
              ImmutableSet.of(Styles.BORDER_NONE, Styles.BG_WHITE, Styles.TEXT_BLACK)));

  public enum ActionType {
    PRIMARY,
    SECONDARY,
    TERTIARY
  }

  public static String navigateToJS(String href) {
    return String.format("document.location.href = \"%s\"", href);
  }

  ActionButton() {}

  public static Builder builder() {
    return new AutoValue_ActionButton.Builder().setId("").setExtraStyles(ImmutableSet.of());
  }

  abstract String id();

  abstract String text();

  abstract String svgRef();

  abstract ActionType actionType();

  abstract ImmutableSet<String> extraStyles();

  abstract String onClickJS();

  private ImmutableSet<String> stylesForAction() {
    switch (actionType()) {
      case PRIMARY:
        return PRIMARY_STYLES;
      case SECONDARY:
        return SECONDARY_STYLES;
      case TERTIARY:
        return TERTIARY_STYLES;
      default:
        throw new RuntimeException(String.format("unrecognized action: %s", actionType()));
    }
  }

  public final Tag render() {
    return button()
        .withId(id())
        .withClasses(StyleUtils.joinStyles(Sets.union(stylesForAction(), extraStyles())))
        .with(
            Icons.svg(svgRef(), SVG_PIXEL_SIZE)
                .withClasses(Styles.ML_2, Styles.MR_1, Styles.INLINE_BLOCK),
            span(text()))
        .attr("onclick", onClickJS());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String id);

    public abstract Builder setText(String text);

    public abstract Builder setSvgRef(String svgRef);

    public abstract Builder setActionType(ActionType actionType);

    public abstract Builder setExtraStyles(ImmutableSet<String> extraStyles);

    public abstract Builder setOnClickJS(String onClickJS);

    abstract ActionButton autoBuild(); // not public

    public final ActionButton build() {
      ActionButton button = autoBuild();

      Preconditions.checkState(!button.text().isEmpty(), "text is required");
      Preconditions.checkState(!button.svgRef().isEmpty(), "SVG reference is required");
      Preconditions.checkState(!button.onClickJS().isEmpty(), "onClickJS is required");
      return button;
    }
  }
}
