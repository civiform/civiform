package views;

/** The HtmlBundle class stores all of the data necessary for rendering a page. */
public HtmlBundle {
    String pageTitle;
    
    List<EmptyTag> metadata;
    List<String> stylesheets;
    List<ToastMessage> toastMessages;

    List<String> headScripts;
    List<String> footerScripts;

    List<String> bodyStyles;

    List<Tag> footerContent;
    List<String> footerStyles;

    List<Tag> headerContent;
    List<String> headerStyles;

    List<Tag> mainContent;
    List<String> mainStyles;

    public HtmlBundle addBodyStyles(String... styles) {
        bodyStyles.addAll(styles);
        return this;
    }

    public HtmlBundle addFooterContent(Tag... tags) {
        footerContent.addAll(tags);
        return this;
    }

    public HtmlBundle addFooterScripts(String... sources) {
        footerScripts.addAll(sources);
        return this;
    }

    public HtmlBundle addFooterStyles(String... styles) {
        footerStyles.addAll(styles);
        return this;
    }

    public HtmlBundle addHeaderContent(Tag... tags) {
        headerContent.addAll(tags);
        return this;
    }

    public HtmlBundle addHeaderStyles(String... styles) {
        headerStyles.addAll(styles);
        return this;
    }

    public HtmlBundle addMainContent(Tag... tags) {
        mainContent.addAll(tags);
        return this;
    }

    public HtmlBundle addHeadScripts(String... sources) {
        headScripts.addAll(sources);
        return this;
    }
    
    public HtmlBundle addMainStyles(String... styles) {
        mainStyles.addAll(styles);
        return this;
    }

    public HtmlBundle addStylesheets(String... sources) {
        stylesheets.addAll(sources);
        return this;
    }

    public HtmlBundle addToastMessages(ToastMessage... messages) {
        toastMessages.addAll(messages);        
        return this;
    }

    public HtmlBundle setTitle(String title) {
        pageTitle = title;
        return this;
    }

    /**
     * The page head contains: 
     *  - page title
     *  - page metadata
     *  - CSS styles
     *  - javascript that needs to run immediately
     */
    private ContainerTag renderHead() {
        // TODO: Throw exception if page title is not set.
        return head().with(
            title(pageTitle),
            metadata,
            each(stylesheets, source -> viewUtils.makeLocalCssTag(source)),
            each(headScripts, source -> viewUtils.makeLocalJsTag(source))
        );
    }

    private ContainerTag renderBody() {
        return body(renderHeader(), renderMain(), renderFooter()).withClasses(bodyStyles);
    }

    private ContainerTag renderHeader() {
        // TODO: Sort toastMessages by priority before displaying.
        return header(
            each(toastMessages, toastMessage -> toastMessage.render()),
            each(headerContent)).withClasses(headerStyles);
    }

    private ContainerTag renderMain() {
        return main(each(mainContent)).withClasses(mainStyles);
    }

    private ContainerTag renderFooter() {
        return footer(
            each(footerContent),
            each(footerScripts, source -> viewUtils.makeLocalJsTag(source))
        ).withClasses(footerStyles);
    }

    public Content getContent() {
        return html(renderHead(), renderBody());
    }
}