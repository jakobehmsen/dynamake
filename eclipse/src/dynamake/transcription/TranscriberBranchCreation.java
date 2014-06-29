package dynamake.transcription;

import dynamake.DualCommand;

public interface TranscriberBranchCreation<T> {
	void create(DualCommand<T> transaction, TranscriberBranchContinuation<T> continuation);
}
