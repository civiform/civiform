package services.program;

import services.Path;

/** PathNotInBlockException is thrown when a path does not belong to any question in the block. */
public class PathNotInBlockException extends Exception {

  public PathNotInBlockException(String blockId, Path path) {
    super(String.format("Block (ID %s) does not contain path %s", blockId, path));
  }
}
