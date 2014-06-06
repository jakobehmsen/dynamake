package dynamake;

import java.util.ArrayList;

public interface PrevaylerServiceBranch<T> {
	void absorb();
	void reject();
//	PrevaylerServiceBranch<T> branch(PropogationContext propCtx, DualCommandFactory<T> transactionFactory, PrevaylerServiceBranchContinuation<T> continuation);
//	PrevaylerServiceBranch<T> branch(PropogationContext propCtx, PrevaylerServiceBranchCreator<T> branchCreator);
	PrevaylerServiceBranch<T> branch();
	void execute(PropogationContext propCtx, DualCommandFactory<T> transactionFactory);
	void onAbsorbed(PrevaylerServiceBranchContinuation<T> continuation);
	void close();
//	void setVariable(String variableName, Object value);
//	Object getVariable(String variableName);
//	void flush();
	
	
	void sendChangeToObservers(Model sender, ArrayList<Observer> observers,
			Object change, PropogationContext propCtx, int nextPropDistance,
			int nextChangeDistance);
	PrevaylerServiceBranch<T> isolatedBranch();
}