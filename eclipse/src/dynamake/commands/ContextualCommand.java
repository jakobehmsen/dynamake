package dynamake.commands;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;

import dynamake.models.Location;
import dynamake.transcription.SnapshottingTranscriber;

// Instances each represents a pairing of a transaction and the models, which were affected
// during the original forward execution of the transaction.
public class ContextualCommand<T> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public final ArrayList<SnapshottingTranscriber.Connection.LocationCommandsPair<T>> transactionsFromRoot;
	public final Hashtable<Location, ArrayList<CommandState<T>>> transactionsFromReferenceLocations;

	public ContextualCommand(ArrayList<SnapshottingTranscriber.Connection.LocationCommandsPair<T>> transactionsFromRoot, Hashtable<Location, ArrayList<CommandState<T>>> transactionsFromReferenceLocations) {
		this.transactionsFromRoot = transactionsFromRoot;
		this.transactionsFromReferenceLocations = transactionsFromReferenceLocations;
	}
}
