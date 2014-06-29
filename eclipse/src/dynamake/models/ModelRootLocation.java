package dynamake.models;


class ModelRootLocation implements ModelLocation {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public Object getChild(Object holder) {
		return holder;
	}

	@Override
	public Location getModelComponentLocation() {
		return new ViewRootLocation();
	}
}