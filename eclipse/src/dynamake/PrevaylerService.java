package dynamake;

import java.io.IOException;

public interface PrevaylerService<T> {
	void close() throws IOException;
	T prevalentSystem();
	void undo(PropogationContext propCtx);
	void redo(PropogationContext propCtx);
	
	void executeTransient(Runnable runnable);
	
	PrevaylerServiceBranch<T> createBranch();
}
