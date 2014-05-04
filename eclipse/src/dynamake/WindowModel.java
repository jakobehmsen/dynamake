package dynamake;

public class WindowModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Model modelCloneIsolated() {
		return new WindowModel();
	}
	
	@Override
	public Binding<ModelComponent> createView(ViewManager viewManager,
			TransactionFactory transactionFactory) {
		// TODO Auto-generated method stub
		return null;
	}
}
