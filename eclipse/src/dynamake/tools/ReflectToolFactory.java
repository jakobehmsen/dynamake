package dynamake.tools;

public class ReflectToolFactory<T extends Tool> implements ToolFactory {
	private String name;
	private Class<T> c;

	public ReflectToolFactory(String name, Class<T> c) {
		this.name = name;
		this.c = c;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Tool createTool() {
		try {
//			System.out.println("Create tool " + name);
			return c.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
}
