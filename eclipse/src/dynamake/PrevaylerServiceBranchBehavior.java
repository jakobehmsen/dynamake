package dynamake;

public interface PrevaylerServiceBranchBehavior<T> {
	void commit(PropogationContext propCtx, ContextualTransaction<T> ctxTransaction);
}
