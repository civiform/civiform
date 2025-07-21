package views.admin.shared;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import lombok.Builder;

/** This record contains values used by layout templates */
@Builder
public record LayoutParams(
    String pageTemplate,
    String civiformImageTag,
    Boolean addNoIndexMetaTag,
    String favicon,
    Optional<String> measurementId,
    ImmutableList<String> stylesheets,
    ImmutableList<String> headScripts,
    ImmutableList<String> bodyScripts) {}
