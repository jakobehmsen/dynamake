package dynamake;

public interface PrevaylerServiceBranch<T> {
	void absorb();
	void reject();
	PrevaylerServiceBranch<T> branch(PropogationContext propCtx, DualCommandFactory<T> transactionFactory);
}
