package dynamake;

import java.util.ArrayList;

public interface PrevaylerServiceBranch<T> {
	void absorb();
	void reject();
	PrevaylerServiceBranch<T> branch();
	void execute(PropogationContext propCtx, DualCommandFactory<T> transactionFactory);
	void close();
	// TODO: Consider: 
	// It should probably be possible to supply a backward runnable in case of rejects?
	void onFinished(Runnable runnable);
	void setOnFinishedBuilder(RunBuilder finishedBuilder);

	void sendChangeToObservers(Model sender, ArrayList<Observer> observers,
			Object change, PropogationContext propCtx, int nextPropDistance,
			int nextChangeDistance);
	PrevaylerServiceBranch<T> isolatedBranch();
}