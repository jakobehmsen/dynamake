package dynamake;

import java.io.Serializable;
import java.util.ArrayList;

// Instances each represents a pairing of a transaction and the models, which were affected
// during the original forward execution of the transaction.
public class ContextualTransaction<T> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public final DualCommand<T> transaction;
	public final ArrayList<Location> affectedModels;
	
	public ContextualTransaction(DualCommand<T> transaction, ArrayList<Location> affectedModels) {
		this.transaction = transaction;
		this.affectedModels = affectedModels;
	}
}
