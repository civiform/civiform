package views.admin.programs;

/** Enum representing the context of a block within a program. */
public enum BlockType {
  SINGLE, /* A block that stands alone in a program. */
  ENUMERATOR, /* A block where applicants will list the repeated entities. */
  REPEATED /* A block that is nested under an enumerator block.  It will be repeated once per repeated entity. */;
}
