package views;

import static j2html.TagCreator.div;
import static j2html.TagCreator.document;
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
import play.twirl.api.Content;
import views.components.Modal;
import views.components.ToastMessage;
import views.style.BaseStyles;

/** The HtmlBundle class stores all of the data necessary for rendering a page. */
public class HtmlBundle {
  private String pageTitle;
  private String language = "en";

  private ArrayList<String> bodyStyles = new ArrayList<>();
  private ArrayList<Tag> footerContent = new ArrayList<>();
  private ArrayList<Tag> footerScripts = new ArrayList<>();
  private ArrayList<String> footerStyles = new ArrayList<>();
  private ArrayList<Tag> headScripts = new ArrayList<>();
  private ArrayList<Tag> headerContent = new ArrayList<>();
  private ArrayList<String> headerStyles = new ArrayList<>();
  private ArrayList<Tag> mainContent = new ArrayList<>();
  private ArrayList<String> mainStyles = new ArrayList<>();
  private ArrayList<EmptyTag> metadata = new ArrayList<>();
  private ArrayList<Modal> modals = new ArrayList<>();
  private ArrayList<Tag> stylesheets = new ArrayList<>();
  private ArrayList<ToastMessage> toastMessages = new ArrayList<>();

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

  public HtmlBundle addMetadata(EmptyTag... tags) {
    metadata.addAll(Arrays.asList(tags));
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

  public HtmlBundle addModals(Modal... modalTags) {
    modals.addAll(Arrays.asList(modalTags));
    return this;
  }

  private ContainerTag getContent() {
    return html(renderHead(), renderBody()).attr("lang", language);
  }

  public String getTitle() {
    return pageTitle;
  }

  public HtmlBundle setLanguage(String lang) {
    language = lang;
    return this;
  }

  public HtmlBundle setTitle(String title) {
    pageTitle = title;
    return this;
  }

  /** The page body contains: - header - main - footer */
  private ContainerTag renderBody() {
    ContainerTag bodyTag = j2html.TagCreator.body().with(renderHeader()).with(renderMain());
    bodyTag.with(renderModals());
    bodyTag.with(renderFooter());

    if (bodyStyles.size() > 0) {
      bodyTag.withClasses(bodyStyles.toArray(new String[0]));
    }

    return bodyTag;
  }

  private ContainerTag renderFooter() {
    ContainerTag footerTag = footer().with(footerContent).with(footerScripts);

    if (footerStyles.size() > 0) {
      footerTag.withClasses(footerStyles.toArray(new String[0]));
    }

    return footerTag;
  }

  /**
   * The page head contains:
   *
   * <ul>
   *   <li>page title
   *   <li>page metadata
   *   <li>CSS styles
   *   <li>javascript that needs to run immediately
   * </ul>
   */
  private ContainerTag renderHead() {
    // TODO: Throw exception if page title is not set.
    return head().with(title(pageTitle)).with(metadata).with(stylesheets).with(headScripts);
  }

  private ContainerTag renderHeader() {
    // TODO: Sort toastMessages by priority before displaying.
    ContainerTag headerTag =
        header()
            .with(each(toastMessages, toastMessage -> toastMessage.getContainerTag()))
            .with(headerContent);

    if (headerStyles.size() > 0) {
      headerTag.withClasses(headerStyles.toArray(new String[0]));
    }

    return headerTag;
  }

  private ContainerTag renderMain() {
    ContainerTag mainTag = main().with(mainContent);

    if (mainStyles.size() > 0) {
      mainTag.withClasses(mainStyles.toArray(new String[0]));
    }

    return mainTag;
  }

  private ContainerTag renderModals() {
    ContainerTag modalContainer =
        div()
            .withId("modal-container")
            .withClasses(BaseStyles.MODAL_CONTAINER)
            .with(div().withId("modal-glass-pane").withClasses(BaseStyles.MODAL_GLASS_PANE));
    modals.forEach(modal -> modalContainer.with(modal.getContainerTag()));
    return modalContainer;
  }

  public Content render() {
    return new HtmlBundleContent(getContent());
  }

  private static class HtmlBundleContent implements Content {
    ContainerTag bundleContent;

    public HtmlBundleContent(ContainerTag bundleContent) {
      this.bundleContent = bundleContent;
    }

    @Override
    public String body() {
      return document(bundleContent);
    }

    @Override
    public String contentType() {
      return "text/html";
    }
  }
}
