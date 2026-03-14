package services.program;

import models.DisplayMode;
import services.LocalizedStrings;

/**
 * A lightweight container for a program that references a question in the draft version, holding
 * only the fields needed for display rather than a full {@link ProgramDefinition}.
 */
public record DraftProgramReference(
    String adminName, DisplayMode displayMode, LocalizedStrings localizedName) {}
