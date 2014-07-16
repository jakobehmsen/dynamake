package dynamake.commands;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;

import dynamake.models.Location;
import dynamake.transcription.SnapshottingTranscriber;
import dynamake.transcription.SnapshottingTranscriber.Connection.LocationCommandsPair;

// Instances each represents a pairing of a transaction and the models, which were affected
// during the original forward execution of the transaction.
public class ContextualCommand<T> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

//	public final DualCommand<T> transaction;
//	public final ArrayList<Location> affectedModelLocations;
//	
//	public ContextualTransaction(DualCommand<T> transaction, ArrayList<Location> affectedModels) {
//		this.transaction = transaction;
//		this.affectedModelLocations = affectedModels;
//	}
	
//	public final ArrayList<DualCommand<T>> transactionsFromRoot;
//	public final Hashtable<Location, ArrayList<DualCommand<T>>> transactionsFromReferenceLocations;
//
//	public ContextualCommand(ArrayList<DualCommand<T>> transactionsFromRoot, Hashtable<Location, ArrayList<DualCommand<T>>> transactionsFromReferenceLocations) {
//		this.transactionsFromRoot = transactionsFromRoot;
//		this.transactionsFromReferenceLocations = transactionsFromReferenceLocations;
//	}
	
//	public static final class LocationCommandPair
	
//	public final ArrayList<CommandState<T>> transactionsFromRoot;
//	public final LinkedHashMap<Location, CommandState<T>> transactionsFromRoot;
	public final ArrayList<SnapshottingTranscriber.Connection.LocationCommandsPair<T>> transactionsFromRoot;
	public final Hashtable<Location, ArrayList<CommandState<T>>> transactionsFromReferenceLocations;

	public ContextualCommand(ArrayList<SnapshottingTranscriber.Connection.LocationCommandsPair<T>> transactionsFromRoot, Hashtable<Location, ArrayList<CommandState<T>>> transactionsFromReferenceLocations) {
		this.transactionsFromRoot = transactionsFromRoot;
		this.transactionsFromReferenceLocations = transactionsFromReferenceLocations;
	}
}
