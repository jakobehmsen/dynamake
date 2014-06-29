package dynamake;

import java.io.IOException;

public interface PrevaylerService<T> {
	void close() throws IOException;
	T prevalentSystem();
	
	void executeTransient(Runnable runnable);
	
	PrevaylerServiceBranch<T> createBranch();
	PrevaylerServiceBranch<T> createBranch(PrevaylerServiceBranchBehavior<T> branchBehavior);
}
