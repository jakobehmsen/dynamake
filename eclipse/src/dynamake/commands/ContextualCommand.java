package dynamake.commands;

import java.io.Serializable;
import java.util.List;

import dynamake.models.Location;
import dynamake.transcription.TransactionHandler;

// Instances each represents a pairing of a transaction and the models, which were affected
// during the original forward execution of the transaction.
public class ContextualCommand<T> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public final Location locationFromRootToReference;
	public final Class<? extends TransactionHandler<T>> transactionHandler;
	public final List<Object> transactionsFromRoot; // List of either atomic commands or transactions

	public ContextualCommand(Location locationFromRootToReference, Class<? extends TransactionHandler<T>> transactionHandler, List<Object> transactionsFromRoot) {
		this.locationFromRootToReference = locationFromRootToReference;
		this.transactionHandler = transactionHandler;
		this.transactionsFromRoot = transactionsFromRoot;
	}
}
