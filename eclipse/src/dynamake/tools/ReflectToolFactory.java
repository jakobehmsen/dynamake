package dynamake.tools;

public class ReflectToolFactory<T extends Tool> implements ToolFactory {
	private String name;
	private Class<T> c;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Tool createTool() {
		try {
			return c.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
}
