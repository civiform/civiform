package views.admin.shared;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import lombok.Builder;
import views.admin.ScriptElementSettings;

/** This record contains values used by layout templates */
@Builder
public record LayoutParams(
    String pageTemplate,
    Boolean isWidescreen,
    String civiformImageTag,
    Boolean addNoIndexMetaTag,
    String favicon,
    Optional<String> measurementId,
    ImmutableList<String> stylesheets,
    ImmutableList<ScriptElementSettings> headScripts,
    ImmutableList<ScriptElementSettings> bodyScripts) {}
