package dynamake;

public interface PrevaylerServiceBranchContinuation<T> {
	void doContinue(PropogationContext propCtx, PrevaylerServiceBranch<T> branch);
}
