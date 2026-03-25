package views.admin.shared;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import lombok.Builder;
import views.admin.LayoutType;
import views.admin.ScriptElementSettings;

/**
 * This record contains values used by layout templates
 *
 * @param pageTemplate Primary page thymeleaf template file to render. This is a relative path from
 *     the {@link views} directory.
 * @param isWidescreen Determines if the TransitionLayout.html uses a widescreen or narrow layout
 * @param layoutType Determines if the layout has an aside
 * @param civiformImageTag Docker image tag for the running container
 * @param addNoIndexMetaTag Include robots meta tag
 * @param favicon Link to the favicon
 * @param measurementId Measurement ID used by tracking service, i.e. Google Analytics
 * @param stylesheets List of stylesheets
 * @param headScripts List of javascript scripts included in the <HEAD>
 * @param bodyScripts List of javascript scripts included at the end of the <BODY>
 */
@Builder
public record LayoutParams(
    String pageTemplate,
    Boolean isWidescreen,
    LayoutType layoutType,
    String civiformImageTag,
    Boolean addNoIndexMetaTag,
    String favicon,
    Optional<String> measurementId,
    ImmutableList<String> stylesheets,
    ImmutableList<ScriptElementSettings> headScripts,
    ImmutableList<ScriptElementSettings> bodyScripts) {}
