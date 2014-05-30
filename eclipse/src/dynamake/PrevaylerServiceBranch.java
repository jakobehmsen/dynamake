package dynamake;

public interface PrevaylerServiceBranch<T> {
	void absorb();
	void reject();
//	PrevaylerServiceBranch<T> branch(PropogationContext propCtx, DualCommandFactory<T> transactionFactory, PrevaylerServiceBranchContinuation<T> continuation);
	PrevaylerServiceBranch<T> branch(PropogationContext propCtx, PrevaylerServiceBranchCreator<T> branchCreator);
	PrevaylerServiceBranch<T> branch();
	void execute(PropogationContext propCtx, DualCommandFactory<T> transactionFactory);
	void doContinue();
	void close();
//	void setVariable(String variableName, Object value);
//	Object getVariable(String variableName);
}