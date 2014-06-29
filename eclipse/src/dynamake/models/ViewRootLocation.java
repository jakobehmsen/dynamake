package dynamake.models;


class ViewRootLocation implements Location {
	@Override
	public Object getChild(Object holder) {
		return holder;
	}
}