package dynamake.models;

public class RootLocation implements Location {
	@Override
	public Object getChild(Object holder) {
		return holder;
	}
}