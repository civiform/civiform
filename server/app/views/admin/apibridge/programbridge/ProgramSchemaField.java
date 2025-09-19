package views.admin.apibridge.programbridge;

import lombok.Builder;

/**
 * Holds info on a single program api bridge request/response schema used to map all schema options.
 * If currently bound contains the selected values.
 */
@Builder
public record ProgramSchemaField(
    String externalName,
    String title,
    String description,
    String type,
    String questionName,
    String questionScalar) {}
