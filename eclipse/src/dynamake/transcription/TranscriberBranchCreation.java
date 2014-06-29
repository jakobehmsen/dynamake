package dynamake.transcription;

import dynamake.commands.DualCommand;

public interface TranscriberBranchCreation<T> {
	void create(DualCommand<T> transaction, TranscriberBranchContinuation<T> continuation);
}
