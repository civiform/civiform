package views.components;

import j2html.tags.ContainerTag;

/**
 * The <use> element takes nodes from within the SVG document, and duplicates them somewhere else.
 * The effect is the same as if the nodes were deeply cloned into a non-exposed DOM, then pasted
 * where the use element is, much like cloned template elements.
 */
public final class UseTag extends ContainerTag<UseTag> {
  public UseTag() {
    super("use");
  }
}
