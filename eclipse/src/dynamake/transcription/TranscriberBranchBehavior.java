package dynamake.transcription;

import dynamake.ContextualTransaction;
import dynamake.models.PropogationContext;

public interface TranscriberBranchBehavior<T> {
	void commit(PropogationContext propCtx, ContextualTransaction<T> ctxTransaction);
}
