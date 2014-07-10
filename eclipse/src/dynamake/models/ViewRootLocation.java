package dynamake.models;

public class ViewRootLocation implements Location {
	@Override
	public Object getChild(Object holder) {
		return holder;
	}
}