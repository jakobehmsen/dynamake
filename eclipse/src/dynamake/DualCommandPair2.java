package dynamake;

import java.util.Date;

public class DualCommandPair2<T> implements DualCommand<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Command<T>[] forwardTransactions;
	private Command<T>[] backwardTransactions;
	
	public DualCommandPair2(Command<T>[] forwardTransactions, Command<T>[] backwardTransactions) {
		this.forwardTransactions = forwardTransactions;
		this.backwardTransactions = backwardTransactions;
	}

	@Override
	public void executeForwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, PrevaylerServiceConnection<T> connection) {
		for(Command<T> forward: forwardTransactions)
			forward.executeOn(propCtx, prevalentSystem, executionTime, connection);
	}

	@Override
	public void executeBackwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, PrevaylerServiceConnection<T> connection) {
		for(Command<T> backward: backwardTransactions)
			backward.executeOn(propCtx, prevalentSystem, executionTime, connection);
	}
}
