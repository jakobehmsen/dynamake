package dynamake;

import dynamake.models.PropogationContext;

public interface PrevaylerServiceBranchBehavior<T> {
	void commit(PropogationContext propCtx, ContextualTransaction<T> ctxTransaction);
}
