package dynamake.tools;

/**
 * Instances of implementors are supposed to be able to create tools and describes the kind of tool.
 */
public interface ToolFactory {
	String getName();
	Tool createTool();
}
