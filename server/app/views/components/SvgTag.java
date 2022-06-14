package views.components;

import j2html.tags.ContainerTag;
import j2html.tags.attributes.IWidth;
import j2html.tags.attributes.IHeight;

public final class SvgTag extends ContainerTag<SvgTag> implements IWidth<SvgTag>, IHeight<SvgTag> {
  public SvgTag() {
    super("svg");
  }
}
