package dynamake;

public interface TranscriberBranchCreation<T> {
	void create(DualCommand<T> transaction, TranscriberBranchContinuation<T> continuation);
}
