package services.program;

import services.Path;

public class PathNotInBlockException extends Exception {

  // TODO: use block definition's reference to its program definition for ProgramDefinition.id when
  // it is available.
  public PathNotInBlockException(BlockDefinition block, Path path) {
    super(
        String.format(
            "Block (ID %d) in program (ID ?) does not contain path %s", block.id(), path.toString()));
  }
}
