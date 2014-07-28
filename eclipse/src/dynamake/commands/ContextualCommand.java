package dynamake.commands;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

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
	public final HashSet<Location> affectedReferenceLocations;

	public ContextualCommand(ArrayList<SnapshottingTranscriber.Connection.LocationCommandsPair<T>> transactionsFromRoot, HashSet<Location> affectedReferenceLocations) {
		this.transactionsFromRoot = transactionsFromRoot;
		this.affectedReferenceLocations = affectedReferenceLocations;
	}
}
