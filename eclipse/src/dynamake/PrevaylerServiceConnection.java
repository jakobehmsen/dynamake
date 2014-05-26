package dynamake;

/*

Possible alternative names:
Session, Recorder, Mediator

*/
public interface PrevaylerServiceConnection<T> {
	/**
	 * Take something which has two methods:
	 * - One method, which creates zero or more forward transactions
	 * - Another method, which creates zero or more backward transactions (if requested - e.g., this won't be requested during undo/redo) 
	 * 
	 * 
	 * @param propCtx
	 * @param transactionFactory
	 */
	void execute2(PropogationContext propCtx, DualCommandFactory2<T> transactionFactory);
	void execute(PropogationContext propCtx, DualCommandFactory<T> transactionFactory);
	void commit(PropogationContext propCtx);
	void rollback(PropogationContext propCtx);
}
