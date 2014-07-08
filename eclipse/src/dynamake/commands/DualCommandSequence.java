package dynamake.commands;

import java.util.ArrayList;
import java.util.Date;

import dynamake.models.PropogationContext;
import dynamake.transcription.TranscriberCollector;

public class DualCommandSequence<T> implements DualCommand<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private DualCommand<T>[] transactions;
	
	@SuppressWarnings("unchecked")
	public DualCommandSequence(ArrayList<DualCommand<T>> transactionList) {
		this.transactions = transactionList.toArray((DualCommand<T>[])new DualCommand[transactionList.size()]);
	}
	
	public DualCommandSequence(DualCommand<T>[] transactions) {
		this.transactions = transactions;
	}

	@Override
	public void executeForwardOn(PropogationContext propCtx, T prevalentSystem,
			Date executionTime, TranscriberCollector<T> collector) {
		for(DualCommand<T> t: transactions)
			t.executeForwardOn(propCtx, prevalentSystem, executionTime, collector);
	}

	@Override
	public void executeBackwardOn(PropogationContext propCtx,
			T prevalentSystem, Date executionTime, TranscriberCollector<T> collector) {
		// Reverse the sequence
		for(int i = transactions.length - 1; i >= 0; i--) {
			DualCommand<T> t = transactions[i];
			t.executeBackwardOn(propCtx, prevalentSystem, executionTime, collector);
		}
	}
}
