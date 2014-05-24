package dynamake;

public interface PrevaylerServiceConnection<T> {
	void execute(PropogationContext propCtx, DualCommandFactory<T> transactionFactory);
	void commit(PropogationContext propCtx);
	void rollback(PropogationContext propCtx);
}
