package dynamake;

public interface PrevaylerServiceBranchCreation<T> {
	void create(DualCommand<T> transaction, PrevaylerServiceBranchContinuation<T> continuation);
}
