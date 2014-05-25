package dynamake;

import java.util.Date;

public class DualCommandSequence<T> implements DualCommand<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private DualCommand<T>[] transactions;
	
	public DualCommandSequence(DualCommand<T>[] transactions) {
		this.transactions = transactions;
	}

	@Override
	public void executeForwardOn(PropogationContext propCtx, T prevalentSystem,
			Date executionTime, PrevaylerServiceConnection<T> connection) {
		for(DualCommand<T> t: transactions)
			t.executeForwardOn(propCtx, prevalentSystem, executionTime, connection);
	}

	@Override
	public void executeBackwardOn(PropogationContext propCtx,
			T prevalentSystem, Date executionTime, PrevaylerServiceConnection<T> newParam) {
		// Reverse the sequence
		for(int i = transactions.length - 1; i >= 0; i--) {
			DualCommand<T> t = transactions[i];
			t.executeBackwardOn(propCtx, prevalentSystem, executionTime, null);
		}
	}
}
