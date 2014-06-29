package dynamake;

import dynamake.models.PropogationContext;

public interface TranscriberBranchBehavior<T> {
	void commit(PropogationContext propCtx, ContextualTransaction<T> ctxTransaction);
}
