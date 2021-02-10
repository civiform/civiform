package services.program;

import com.google.common.collect.ImmutableList;

/** Operations you can perform on Programs. */
public interface ProgramService {

    /** List all programs. */
    public ImmutableList<ProgramDefinition> listProgramDefinitions();

    /** Get the definition for a given program. */
    public ProgramDefinition getProgramDefinition(String id);

    /** Create a new program. */
    public ProgramDefinition createProgram(String name, String description);

}
