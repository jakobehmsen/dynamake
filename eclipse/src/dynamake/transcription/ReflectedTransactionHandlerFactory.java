package dynamake.transcription;

public class ReflectedTransactionHandlerFactory<T> implements TransactionHandlerFactory<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Class<? extends TransactionHandler<T>> handlerClass;

	public ReflectedTransactionHandlerFactory(Class<? extends TransactionHandler<T>> handlerClass) {
		this.handlerClass = handlerClass;
	}

	@Override
	public TransactionHandler<T> createTransactionHandler(T reference) {
		try {
			return handlerClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
}
