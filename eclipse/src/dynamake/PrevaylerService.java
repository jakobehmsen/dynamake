package dynamake;

import java.io.IOException;

public interface PrevaylerService<T> {
	void close() throws IOException;
	T prevalentSystem();
	void undo(PropogationContext propCtx, Location location, RunBuilder runBuilder);
	void redo(PropogationContext propCtx, Location location, RunBuilder finishedBuilder);
	
	void executeTransient(Runnable runnable);
	
	PrevaylerServiceBranch<T> createBranch();
}
