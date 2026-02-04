package views.admin.shared;

import lombok.Builder;

/** This record contains globally shared values that are available to any Thymeleaf template. */
@Builder
public record TemplateGlobals(String pageTitle, String cspNonce, String csrfToken) {}
