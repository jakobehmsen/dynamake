package dynamake;

public interface PrevaylerServiceBranch<T> {
	void absorb();
	void reject();
//	PrevaylerServiceBranch<T> branch(PropogationContext propCtx, DualCommandFactory<T> transactionFactory, PrevaylerServiceBranchContinuation<T> continuation);
	PrevaylerServiceBranch<T> branch(PropogationContext propCtx, PrevaylerServiceBranchCreator<T> branchCreator);
	void doContinue();
//	void setVariable(String variableName, Object value);
//	Object getVariable(String variableName);
}