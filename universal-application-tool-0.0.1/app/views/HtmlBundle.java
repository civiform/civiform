package views;

import static j2html.TagCreator.body;
import static j2html.TagCreator.each;
import static j2html.TagCreator.footer;
import static j2html.TagCreator.head;
import static j2html.TagCreator.header;
import static j2html.TagCreator.html;
import static j2html.TagCreator.main;
import static j2html.TagCreator.title;

import j2html.tags.ContainerTag;
import j2html.tags.EmptyTag;
import j2html.tags.Tag;
import java.util.ArrayList;
import java.util.Arrays;
import views.components.ToastMessage;

/** The HtmlBundle class stores all of the data necessary for rendering a page. */
public class HtmlBundle {
  String pageTitle;

  ArrayList<String> bodyStyles = new ArrayList<String>();
  ArrayList<Tag> footerContent = new ArrayList<Tag>();
  ArrayList<Tag> footerScripts = new ArrayList<Tag>();
  ArrayList<String> footerStyles = new ArrayList<String>();
  ArrayList<Tag> headScripts = new ArrayList<Tag>();
  ArrayList<Tag> headerContent = new ArrayList<Tag>();
  ArrayList<String> headerStyles = new ArrayList<String>();
  ArrayList<Tag> mainContent = new ArrayList<Tag>();
  ArrayList<String> mainStyles = new ArrayList<String>();
  ArrayList<EmptyTag> metadata = new ArrayList<EmptyTag>();
  ArrayList<Tag> stylesheets = new ArrayList<Tag>();
  ArrayList<ToastMessage> toastMessages = new ArrayList<ToastMessage>();

  public HtmlBundle addBodyStyles(String... styles) {
    bodyStyles.addAll(Arrays.asList(styles));
    return this;
  }

  public HtmlBundle addFooterContent(Tag... tags) {
    footerContent.addAll(Arrays.asList(tags));
    return this;
  }

  public HtmlBundle addFooterScripts(Tag... sources) {
    footerScripts.addAll(Arrays.asList(sources));
    return this;
  }

  public HtmlBundle addFooterStyles(String... styles) {
    footerStyles.addAll(Arrays.asList(styles));
    return this;
  }

  public HtmlBundle addHeadScripts(Tag... sources) {
    headScripts.addAll(Arrays.asList(sources));
    return this;
  }

  public HtmlBundle addHeaderContent(Tag... tags) {
    headerContent.addAll(Arrays.asList(tags));
    return this;
  }

  public HtmlBundle addHeaderStyles(String... styles) {
    headerStyles.addAll(Arrays.asList(styles));
    return this;
  }

  public HtmlBundle addMainContent(Tag... tags) {
    mainContent.addAll(Arrays.asList(tags));
    return this;
  }

  public HtmlBundle addMainStyles(String... styles) {
    mainStyles.addAll(Arrays.asList(styles));
    return this;
  }

  public HtmlBundle addStylesheets(Tag... sources) {
    stylesheets.addAll(Arrays.asList(sources));
    return this;
  }

  public HtmlBundle addToastMessages(ToastMessage... messages) {
    toastMessages.addAll(Arrays.asList(messages));
    return this;
  }

  public ContainerTag getContent() {
    return html(renderHead(), renderBody());
  }

  public HtmlBundle setTitle(String title) {
    pageTitle = title;
    return this;
  }

  /** The page body contains: - header - main - footer */
  private ContainerTag renderBody() {
    return body()
        .with(renderHeader())
        .with(renderMain())
        .with(renderFooter())
        .withClasses(bodyStyles.toArray(new String[0]));
  }

  private ContainerTag renderFooter() {
    return footer()
        .with(footerContent)
        .with(footerScripts)
        .withClasses(footerStyles.toArray(new String[0]));
  }

  /**
   * The page head contains: - page title - page metadata - CSS styles - javascript that needs to
   * run immediately
   */
  private ContainerTag renderHead() {
    // TODO: Throw exception if page title is not set.
    return head().with(title(pageTitle)).with(metadata).with(stylesheets).with(headScripts);
  }

  private ContainerTag renderHeader() {
    // TODO: Sort toastMessages by priority before displaying.
    return header()
        .with(each(toastMessages, toastMessage -> toastMessage.getContainerTag()))
        .with(headerContent)
        .withClasses(headerStyles.toArray(new String[0]));
  }

  private ContainerTag renderMain() {
    return main().with(mainContent).withClasses(mainStyles.toArray(new String[0]));
  }
}
