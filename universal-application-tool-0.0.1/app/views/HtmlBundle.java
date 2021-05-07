package views;


public HtmlBundle {    
    String pageTitle;
    
    List<EmptyTag> metadata;
    List<String> stylesheets;
    List<String> toastMessages;

    List<String> bodyStyles;

    List<Tag> headerContent;
    List<String> headerStyles;

    List<Tag> mainContent;
    List<String> mainStyles;

    List<Tag> footerContent;
    List<String> footerStyles;

    List<String> headScripts;
    List<String> footerScripts;

    /**
     * The page head contains: 
     *  - page title
     *  - page metadata
     *  - CSS styles
     *  - javascript that needs to run immediately
     */
    private ContainerTag renderHead() {
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
        return header(each(headerContent)).withClasses(headerStyles);
    }

    private ContainerTag renderMain() {
        return main(each(mainContent)).withClasses(mainStyles);
    }

    private ContainerTag renderFooter() {
        return footer(footerContent,
            each(footerScripts, source -> viewUtils.makeLocalJsTag(source))
        ).withClasses(footerStyles);
    }

    public Content getContent() {
        return html(renderHead(), renderBody());
    }
}