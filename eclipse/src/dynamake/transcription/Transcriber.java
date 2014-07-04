package dynamake.transcription;

import java.io.IOException;

public interface Transcriber<T> {
	void close() throws IOException;
	T prevalentSystem();
	
	void executeTransient(Runnable runnable);
	
	TranscriberBranch<T> createBranch();
	TranscriberBranch<T> createBranch(TranscriberBranchBehavior<T> branchBehavior);
	
	TranscriberConnection<T> createConnection();
}
