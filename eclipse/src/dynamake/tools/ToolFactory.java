package dynamake.tools;

public interface ToolFactory {
	String getName();
	Tool createTool();
}
