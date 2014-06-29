package dynamake.transcription;

import dynamake.commands.ContextualTransaction;
import dynamake.models.PropogationContext;

public interface TranscriberBranchBehavior<T> {
	void commit(PropogationContext propCtx, ContextualTransaction<T> ctxTransaction);
}
