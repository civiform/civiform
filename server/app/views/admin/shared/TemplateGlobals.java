package views.admin.shared;

import java.util.Optional;
import lombok.Builder;

/**
 * This record contains globally shared values that are available to any Thymeleaf template.
 *
 * @param pageTitle Page title shown in browser window/tab
 * @param pageHeading Page heading shown on the page's H1 element
 * @param pageIntro Page intro shown below the {@link #pageHeading }
 * @param cspNonce CSP Nonce value used for scripts and styles elements
 * @param csrfToken CSRF Token used when submitting forms
 */
@Builder
public record TemplateGlobals(
    String pageTitle,
    String pageHeading,
    Optional<String> pageIntro,
    String cspNonce,
    String csrfToken) {}
