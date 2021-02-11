package services.program;

import com.google.common.collect.ImmutableList;

/** Operations you can perform on ProgramDefinitions. */
public interface ProgramService {

    /** List all programs. */
    ImmutableList<ProgramDefinition> listProgramDefinitions();

    /** Get the definition for a given program. */
    ProgramDefinition getProgramDefinition(long id);

    /** Create a new program. */
    ProgramDefinition createProgram(String name, String description);
}
