package dynamake.commands;

import java.io.Serializable;
import java.util.List;

import dynamake.models.Location;
import dynamake.transcription.TransactionHandlerFactory;

// Instances each represents a pairing of a transaction and the models, which were affected
// during the original forward execution of the transaction.
public class ContextualCommand<T> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public final Location locationFromRootToReference;
	public final TransactionHandlerFactory<T> transactionHandlerFactory;
	public final List<Object> transactionsFromRoot; // List of either atomic commands or transactions

	public ContextualCommand(Location locationFromRootToReference, TransactionHandlerFactory<T> transactionHandler, List<Object> transactionsFromRoot) {
		this.locationFromRootToReference = locationFromRootToReference;
		this.transactionHandlerFactory = transactionHandler;
		this.transactionsFromRoot = transactionsFromRoot;
	}
}
