package dynamake.transcription;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dynamake.collections.Categorizer;
import dynamake.commands.CommandStateWithOutput;
import dynamake.commands.ContextualCommand;
import dynamake.commands.CommandState;
import dynamake.delegates.Func0;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelRootLocation;
import dynamake.models.PropogationContext;

public class SnapshottingTranscriber<T> implements Transcriber<T> {
	private Func0<T> prevalentSystemFunc;
	private T prevalentSystem;
	private int transactionEnlistingCount = 0;
	private int snapshotThreshold = 100;
	private ExecutorService transactionExecutor;
	
	private String prevalanceDirectory;
	private static String journalFileName = "log";
	private static String journalFile = journalFileName + ".jnl";
	private static String snapshotFile = "snap.shot";
	private ExecutorService journalLogger;
	
	public SnapshottingTranscriber(Func0<T> prevalentSystemFunc) throws Exception {
		this.prevalentSystemFunc = prevalentSystemFunc; 
		transactionExecutor = Executors.newSingleThreadExecutor();
		
		journalLogger = Executors.newSingleThreadExecutor();
		prevalanceDirectory = "jnl";
		
		// One current journal with a predictive name and multiple old journals with predictive naming
		// One latest snapshot with a predictive name and multiple old snapshot with predictive naming
		// Each snapshot has a reference to its relative journal
		// When a snapshot is enqueue, then the current journal is closed and referenced to from the snapshot made followingly
		// such that it is known which journal to start from after the snapshot has been read.
		
		boolean journalExisted = true;
		boolean snapshotExisted = true;
		
		Path prevalanceDirectoryPath = Paths.get(prevalanceDirectory);
		if(!java.nio.file.Files.exists(prevalanceDirectoryPath)) {
			java.nio.file.Files.createDirectory(prevalanceDirectoryPath);
			journalExisted = false;
		}
		
		Path journalFilePath = Paths.get(prevalanceDirectory + "/" + journalFile);
		if(!java.nio.file.Files.exists(journalFilePath)) {
			java.nio.file.Files.createFile(journalFilePath);
			journalExisted = false;
		}
		
		Path snapshotFilePath = Paths.get(prevalanceDirectory + "/" + snapshotFile);
		if(!java.nio.file.Files.exists(snapshotFilePath)) {
			snapshotExisted = false;
		}
		
		if(snapshotExisted) {
			Snapshot<T> snapshot = loadSnapshot(prevalanceDirectory + "/" + snapshotFile);
			prevalentSystem = snapshot.prevalentSystem;
		} else
			prevalentSystem = prevalentSystemFunc.call();
		
		if(journalExisted) {
			ArrayList<ContextualCommand<T>> transactions = readJournal(prevalanceDirectory + "/" + journalFile);
			replay(transactions, prevalentSystem);
			// Update the number of enlisted transactions which is used in the snapshotting logic
			transactionEnlistingCount += transactions.size();
		}
	}
	
	private static <T> Snapshot<T> loadAndReplay(Func0<T> prevalantSystemFunc, String journalPath, String snapshotPath) throws ClassNotFoundException, IOException {
		Snapshot<T> snapshot;
		
		Path snapshotFilePath = Paths.get(snapshotPath);
		
		if(java.nio.file.Files.exists(snapshotFilePath))
			snapshot = loadSnapshot(snapshotPath);
		else {
			T prevalantSystem = prevalantSystemFunc.call();
			
			snapshot = new Snapshot<T>(prevalantSystem);
		}
		
		replay(snapshot.prevalentSystem, journalPath);
		
		return snapshot;
	}
	
	private static <T> ArrayList<ContextualCommand<T>> readJournal(String journalPath) throws ClassNotFoundException, IOException {
		ArrayList<ContextualCommand<T>> transactions = new ArrayList<ContextualCommand<T>>();
		
		FileInputStream fileOutput = new FileInputStream(journalPath);
		BufferedInputStream bufferedOutput = new BufferedInputStream(fileOutput);
		
		try {
			while(bufferedOutput.available() != 0) {
				// Should be read in chunks
				ObjectInputStream objectOutput = new ObjectInputStream(bufferedOutput);
				@SuppressWarnings("unchecked")
				ContextualCommand<T> transaction = (ContextualCommand<T>)objectOutput.readObject();
					
				transactions.add(transaction);
			}
		} finally {
			bufferedOutput.close();
		}
		
		return transactions;
	}
	
	@SuppressWarnings("unchecked")
	private static <T> void replay(ArrayList<ContextualCommand<T>> transactions, T prevalentSystem) {
		PropogationContext propCtx = new PropogationContext();
		
		Collector<T> isolatedCollector = new NullCollector<T>();
		
		for(ContextualCommand<T> ctxTransaction: transactions) {
			Categorizer<T, Class<? extends HistoryHandler<T>>> referencesToAppliedHistoryHandlers = new Categorizer<T, Class<? extends HistoryHandler<T>>>();
			Hashtable<Location, T> locationToReferenceMap = new Hashtable<Location, T>();
			Hashtable<Class<? extends HistoryHandler<T>>, HistoryHandler<T>> historyHandlerClassToInstanceMap = new Hashtable<Class<? extends HistoryHandler<T>>, HistoryHandler<T>>();
			
			for(SnapshottingTranscriber.Connection.LocationCommandsPair<T> entry: ctxTransaction.transactionsFromRoot) {
				Location location = entry.location;
				
				HistoryHandler<T> historyHandler = null;
				Model reference = (Model)location.getChild(prevalentSystem);
				if(!referencesToAppliedHistoryHandlers.containsItem((T)reference, entry.historyHandlerClass)) {
					try {
						historyHandler = entry.historyHandlerClass.newInstance();
					} catch (InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
					}
					historyHandler.startLogFor((T)reference, propCtx, 0, isolatedCollector);
					historyHandlerClassToInstanceMap.put(entry.historyHandlerClass, historyHandler);
				} else
					historyHandler = historyHandlerClassToInstanceMap.get(entry.historyHandlerClass);
				
				ArrayList<Execution<T>> pendingUndoablePairs = new ArrayList<Execution<T>>();
				for(CommandState<T> transaction: entry.pending) {
					CommandStateWithOutput<T> undoable = (CommandStateWithOutput<T>)transaction.executeOn(propCtx, prevalentSystem, isolatedCollector, location);
					pendingUndoablePairs.add(new Execution<T>(transaction, undoable));
				}
				
				historyHandler.logFor((T)reference, pendingUndoablePairs, propCtx, 0, isolatedCollector);
				referencesToAppliedHistoryHandlers.add((T)reference, entry.historyHandlerClass);
				
				locationToReferenceMap.put(location, (T)reference);
			}
			
			for(Location affectedReferenceLocation: ctxTransaction.affectedReferenceLocations) {
				T reference = (T)locationToReferenceMap.get(affectedReferenceLocation);
				// Update the log of each affected reference isolately; no transaction is cross-reference
				
				List<Class<? extends HistoryHandler<T>>> historyHandlerClasses = referencesToAppliedHistoryHandlers.getItems((T)reference);
				for(Class<? extends HistoryHandler<T>> historyHandlerClass: historyHandlerClasses) {
					HistoryHandler<T> historyHandler = historyHandlerClassToInstanceMap.get(historyHandlerClass);
					historyHandler.commitLogFor((T)reference, propCtx, 0, isolatedCollector);
				}
			}
		}
	}
	
	private static <T> void replay(T prevalentSystem, String journalPath) throws ClassNotFoundException, IOException {
		ArrayList<ContextualCommand<T>> transactions = readJournal(journalPath);
		replay(transactions, prevalentSystem);
	}
	
	private static class Snapshot<T> implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final T prevalentSystem;
		
		public Snapshot(T prevalentSystem) {
			this.prevalentSystem = prevalentSystem;
		}
	}
	
	private static <T> Snapshot<T> loadSnapshot(String snapshotPath) throws IOException, ClassNotFoundException {
		FileInputStream fileOutput = new FileInputStream(snapshotPath);
		BufferedInputStream bufferedOutput = new BufferedInputStream(fileOutput);

		ObjectInputStream objectOutput = new ObjectInputStream(bufferedOutput);
		@SuppressWarnings("unchecked")
		Snapshot<T> snapshot = (Snapshot<T>)objectOutput.readObject();
		
		bufferedOutput.close();
		
		return snapshot;
	}
	
	private static <T> void saveSnapshot(Func0<T> prevalantSystemFunc, String journalPath, String snapshotPath) throws ClassNotFoundException, IOException, ParseException {
		// Close journal
		Path currentJournalFilePath = Paths.get(journalPath);

		String nowFormatted = "" + System.nanoTime();
		Path closedJournalFilePath = Paths.get(currentJournalFilePath.getParent() + "/" + nowFormatted + currentJournalFilePath.getFileName());
		
		java.nio.file.Files.move(currentJournalFilePath, closedJournalFilePath);
		
		// Start new journal
		java.nio.file.Files.createFile(currentJournalFilePath);
		
		// Close snapshot
		Path currentSnapshotFilePath = Paths.get(snapshotPath);
		Path closedSnapshotFilePath = Paths.get(currentSnapshotFilePath.getParent() + "/" + nowFormatted + currentSnapshotFilePath.getFileName());
		if(java.nio.file.Files.exists(currentSnapshotFilePath))
			java.nio.file.Files.move(currentSnapshotFilePath, closedSnapshotFilePath);
		
		// Load copy of last snapshot (if any) and replay missing transactions;
		Snapshot<T> snapshot = loadAndReplay(prevalantSystemFunc, closedJournalFilePath.toString(), closedSnapshotFilePath.toString());
		
		// Save modified snapshot
		FileOutputStream fileOutput = new FileOutputStream(snapshotPath, true);
		BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput);
		ObjectOutputStream objectOutput = new ObjectOutputStream(bufferedOutput);
		
		objectOutput.writeObject(snapshot);
		
		objectOutput.close();
	}
	
	private void saveSnapshot() throws ClassNotFoundException, IOException, ParseException {
		saveSnapshot(prevalentSystemFunc, prevalanceDirectory + "/" + journalFile, prevalanceDirectory + "/" + snapshotFile);
	}

	public void executeTransient(Runnable runnable) {
		transactionExecutor.execute(runnable);
	}
	
	public void persistTransaction(final ContextualCommand<T> transaction) {
		journalLogger.execute(new Runnable() {
			@Override
			public void run() {
				try {
					// Should be written in chunks
					FileOutputStream fileOutput = new FileOutputStream(prevalanceDirectory + "/" + "log.jnl", true);
					BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput);
					ObjectOutputStream objectOutput = new ObjectOutputStream(bufferedOutput);
					
					objectOutput.writeObject(transaction);
					
					objectOutput.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
//				System.out.println("Persisted transaction.");
				
				transactionEnlistingCount++;
				if(transactionEnlistingCount >= snapshotThreshold) {
					System.out.println("Enlisted snapshot on thread " + Thread.currentThread().getId());
					// TODO: Consider: Should an isolated propogation context created here? I.e., a snapshot propogation context?
					try {
						// Could be separated into the following:
						// Close latest journal and snapshot 
						// With other execution service: Save snapshot based on closed journal and snapshot
						saveSnapshot();
					} catch (ClassNotFoundException | IOException
							| ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					transactionEnlistingCount = 0;
				}
			}
		});
	}
	
	@Override
	public void close() {
		try {
			transactionExecutor.shutdown();
			journalLogger.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public T prevalentSystem() {
		return prevalentSystem;
	}
	
	private static class Instruction {
		public static final int OPCODE_COMMIT = 0;
		public static final int OPCODE_REJECT = 1;
		public static final int OPCODE_FLUSH_NEXT_TRIGGER = 2;
		public static final int OPCODE_SEND_PROPOGATION_FINISHED = 3;
		
		public final int type;
		public final Object operand;
		
		public Instruction(int type, Object operand) {
			this.type = type;
			this.operand = operand;
		}
	}
	
	public static class Connection<T> implements dynamake.transcription.Connection<T> {
		// Probably, many of the below fields are to be related to "transaction frame" of some sort
		private TriggerHandler<T> triggerHandler;
		private SnapshottingTranscriber<T> transcriber;
		private ArrayList<LocationCommandsPair<T>> flushedTransactionsFromRoot = new ArrayList<LocationCommandsPair<T>>();
		private HashSet<ReferenceAndLocation<T>> affectedReferences = new HashSet<ReferenceAndLocation<T>>();
		private ArrayList<UndoableCommandFromReference<T>> flushedUndoableTransactionsFromReferences = new ArrayList<UndoableCommandFromReference<T>>();
		private Categorizer<ReferenceAndLocation<T>, Class<? extends HistoryHandler<T>>> referencesToAppliedHistoryHandlers = new Categorizer<ReferenceAndLocation<T>, Class<? extends HistoryHandler<T>>>();
		private Hashtable<Class<? extends HistoryHandler<T>>, HistoryHandler<T>> historyHandlerClassToInstanceMap = new Hashtable<Class<? extends HistoryHandler<T>>, HistoryHandler<T>>();
		
		public Connection(SnapshottingTranscriber<T> transcriber, TriggerHandler<T> triggerHandler) {
			this.transcriber = transcriber;
			this.triggerHandler = triggerHandler;
		}
		
		private static class ReferenceAndLocation<T> implements Serializable {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public final T reference;
			public final Location location;
			
			public ReferenceAndLocation(T reference, Location location) {
				this.reference = reference;
				this.location = location;
			}
			
			@Override
			public boolean equals(Object obj) {
				if(obj instanceof ReferenceAndLocation) {
					@SuppressWarnings("unchecked")
					ReferenceAndLocation<T> otherReferenceAndLocation = (ReferenceAndLocation<T>)obj;
					
					return this.reference.equals(otherReferenceAndLocation.reference) &&
							this.location.equals(otherReferenceAndLocation.location);
				}
				
				return false;
			}
			
			@Override
			public int hashCode() {
				return reference.hashCode() * location.hashCode();
			}
		}
		
		public static class LocationCommandsPair<T> implements Serializable {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public final Location location;
			public final ArrayList<CommandState<T>> pending;
			public final Class<? extends HistoryHandler<T>> historyHandlerClass;
			
			public LocationCommandsPair(Location location, ArrayList<CommandState<T>> pending, Class<? extends HistoryHandler<T>> historyHandlerClass) {
				this.location = location;
				this.pending = pending;
				this.historyHandlerClass = historyHandlerClass;
			}
		}
		
		private static class UndoableCommandFromReference<T> {
			public final T reference;
			public final ArrayList<CommandState<T>> undoables;
			
			public UndoableCommandFromReference(T reference, ArrayList<CommandState<T>> undoables) {
				this.reference = reference;
				this.undoables = undoables;
			}
		}

		@Override
		public void trigger(final Trigger<T> trigger) {
			this.transcriber.transactionExecutor.execute(new Runnable() {
				@SuppressWarnings("unchecked")
				@Override
				public void run() {
					PropogationContext propCtx = new PropogationContext();
					final LinkedList<Object> commands = new LinkedList<Object>();
					commands.add(trigger);
					
					Stack<List<Execution<T>>> propogationStack = new Stack<List<Execution<T>>>();
					final ArrayList<Runnable> onAfterNextTrigger = new ArrayList<Runnable>();
					
					while(!commands.isEmpty()) {
						Object command = commands.pop();
						
						final ArrayList<Object> collectedCommands = new ArrayList<Object>();
						Collector<T> collector = new Collector<T>() {
							@Override
							public void execute(Object command) {
								if(command instanceof PendingCommandFactory) {
									collectedCommands.add(command);
									collectedCommands.add(new Instruction(Instruction.OPCODE_SEND_PROPOGATION_FINISHED, command));
								} else {
									collectedCommands.add(command);
								}
							}
							
							@Override
							public void afterNextTrigger(Runnable runnable) {
								onAfterNextTrigger.add(runnable);
							}
							
							@Override
							public void reject() {
								collectedCommands.add(Instruction.OPCODE_REJECT);
							}
							
							@Override
							public void commit() {
								collectedCommands.add(Instruction.OPCODE_COMMIT);
							}
							
							@Override
							public void flushNextTrigger() {
								collectedCommands.add(Instruction.OPCODE_FLUSH_NEXT_TRIGGER);
							}
						};
						
						if(command instanceof Integer) {
							int i = (int)command;
							
							switch(i) {
							case Instruction.OPCODE_COMMIT:
								doCommit();
								break;
							case Instruction.OPCODE_REJECT:
								doReject();
								break;
							case Instruction.OPCODE_FLUSH_NEXT_TRIGGER:
								if(onAfterNextTrigger.size() > 0) {
									triggerHandler.handleAfterTrigger(new ArrayList<Runnable>(onAfterNextTrigger));
									onAfterNextTrigger.clear();
								}
								break;
							}
						} else if(command instanceof Instruction) {
							Instruction instruction = (Instruction)command;
							
							switch(instruction.type) {
							case Instruction.OPCODE_SEND_PROPOGATION_FINISHED:
								List<Execution<T>> pendingUndoablePairs = propogationStack.pop();
								if(pendingUndoablePairs.size() != 1) {
									new String();
								}
								Execution<T> execution = pendingUndoablePairs.get(0);
								((PendingCommandFactory<T>)instruction.operand).afterPropogationFinished(execution, propCtx, 0, collector);
								break;
							}
						} else if(command instanceof PendingCommandFactory) {
							PendingCommandFactory<T> transactionFactory = (PendingCommandFactory<T>)command;
							T reference = transactionFactory.getReference();
							
							if(reference == null || ((Model)reference).getLocator() == null) {
								reference = transactionFactory.getReference();
							}
							Location locationFromRoot = ((Model)reference).getLocator().locate();
							Class<? extends HistoryHandler<T>> historyHandlerClass = null;

							ReferenceAndLocation<T> referenceAndLocation = new ReferenceAndLocation<T>(reference, locationFromRoot);
							historyHandlerClass = transactionFactory.getHistoryHandlerClass();
							
							HistoryHandler<T> historyHandler = null;
							if(!referencesToAppliedHistoryHandlers.containsItem(referenceAndLocation, historyHandlerClass)) {
								try {
//									System.out.println("Start log for " + reference + " at " + referenceAndLocation.location);
									historyHandler = historyHandlerClass.newInstance();
									historyHandler.startLogFor(reference, propCtx, 0, collector);
									historyHandlerClassToInstanceMap.put(historyHandlerClass, historyHandler);
									referencesToAppliedHistoryHandlers.add(referenceAndLocation, historyHandlerClass);
								} catch (InstantiationException | IllegalAccessException e) {
									e.printStackTrace();
								}
							} else
								historyHandler = historyHandlerClassToInstanceMap.get(historyHandlerClass);
							
							Location locationFromReference = new ModelRootLocation();
							
							// If location was part of the executeOn invocation, location is probably no
							// necessary for creating dual commands. Further, then, it is probably not necessary
							// to create two sequences of pendingCommands.
							CommandState<T> pendingCommand =  transactionFactory.createPendingCommand();
							
							// Assumed that exactly one command is created consistenly
							
							// Should be in pending state
							ArrayList<CommandState<T>> undoables = new ArrayList<CommandState<T>>();

							// The command in pending state should return a command in undoable state
							CommandState<T> undoableCommand = pendingCommand.executeOn(propCtx, reference, collector, locationFromReference);
							undoables.add(undoableCommand);

							flushedUndoableTransactionsFromReferences.add(new UndoableCommandFromReference<T>(reference, undoables));
							
							ArrayList<Execution<T>> pendingUndoablePairs = new ArrayList<Execution<T>>();

							CommandStateWithOutput<T> undoable = (CommandStateWithOutput<T>)undoables.get(0);
							Execution<T> pendingUndoablePair = new Execution<T>(pendingCommand, undoable);
							pendingUndoablePairs.add(pendingUndoablePair);
							
							affectedReferences.add(referenceAndLocation);
							
							historyHandler.logFor(reference, pendingUndoablePairs, propCtx, 0, collector);

							ArrayList<CommandState<T>> pendingCommands = new ArrayList<CommandState<T>>();
							pendingCommands.add(pendingCommand);
							flushedTransactionsFromRoot.add(new LocationCommandsPair<T>(locationFromRoot, pendingCommands, historyHandlerClass));
							
							propogationStack.push(pendingUndoablePairs);
								
						} else if(command instanceof Trigger) {
							// TODO: Consider: Should it be possible to hint which model to base the trigger on?
							((Trigger<T>)command).run(collector);
						}
						
						if(collectedCommands.size() > 0) {
							commands.addAll(0, collectedCommands);
						}
					}

					if(onAfterNextTrigger.size() > 0) {
						triggerHandler.handleAfterTrigger(onAfterNextTrigger);
					}
					
//					System.out.println("Finished trigger");
				}
			});
		}
		
		private void doCommit() {
			if(flushedTransactionsFromRoot.size() > 0) {
				PropogationContext propCtx = new PropogationContext();
				
				final ArrayList<Runnable> onAfterNextTrigger = new ArrayList<Runnable>();
				
				Collector<T> isolatedCollector = new NullCollector<T>() {
					@Override
					public void afterNextTrigger(Runnable runnable) {
						onAfterNextTrigger.add(runnable);
					}
				};
				
				ArrayList<LocationCommandsPair<T>> transactionsFromRoot = new ArrayList<LocationCommandsPair<T>>();

				transactionsFromRoot.addAll(flushedTransactionsFromRoot);
					
				flushedTransactionsFromRoot.clear();
				
				HashSet<Location> affectedReferenceLocations = new HashSet<Location>();
				
				for(ReferenceAndLocation<T> referenceAndLocation: affectedReferences) {
					List<Class<? extends HistoryHandler<T>>> historyHandlerClasses = referencesToAppliedHistoryHandlers.getItems(referenceAndLocation);
					for(Class<? extends HistoryHandler<T>> historyHandlerClass: historyHandlerClasses) {
						HistoryHandler<T> historyHandler = historyHandlerClassToInstanceMap.get(historyHandlerClass);
						historyHandler.commitLogFor(referenceAndLocation.reference, propCtx, 0, isolatedCollector);
					}
					
					Location referenceLocation = referenceAndLocation.location;
					affectedReferenceLocations.add(referenceLocation);
				}
				
				affectedReferences.clear();
				flushedUndoableTransactionsFromReferences.clear();
				
				ContextualCommand<T> transactionToPersist = new ContextualCommand<T>(transactionsFromRoot, affectedReferenceLocations);

//				System.out.println("Committed connection");
				transcriber.persistTransaction(transactionToPersist);
				referencesToAppliedHistoryHandlers.clear();
				historyHandlerClassToInstanceMap.clear();
				
				if(onAfterNextTrigger.size() > 0) {
					triggerHandler.handleAfterTrigger(onAfterNextTrigger);
				}
			}
		}
		
		private void doReject() {
			PropogationContext propCtx = new PropogationContext();
			
			final ArrayList<Runnable> onAfterNextTrigger = new ArrayList<Runnable>();
			
			Collector<T> isolatedCollector = new NullCollector<T>() {
				@Override
				public void afterNextTrigger(Runnable runnable) {
					onAfterNextTrigger.add(runnable);
				}
			};

			for(UndoableCommandFromReference<T> transaction: flushedUndoableTransactionsFromReferences) {
				Location locationFromReference = new ModelRootLocation();
				for(CommandState<T> undoable: transaction.undoables) {
					@SuppressWarnings("unused")
					CommandState<T> redoable = undoable.executeOn(propCtx, transaction.reference, isolatedCollector, locationFromReference);
				}
			}
			
			System.out.println("Rejected connection");
			
			flushedTransactionsFromRoot.clear();
			
			for(ReferenceAndLocation<T> referenceAndLocation: affectedReferences) {
				List<Class<? extends HistoryHandler<T>>> historyHandlerClasses = referencesToAppliedHistoryHandlers.getItems(referenceAndLocation);
				for(Class<? extends HistoryHandler<T>> historyHandlerClass: historyHandlerClasses) {
					HistoryHandler<T> historyHandler = historyHandlerClassToInstanceMap.get(historyHandlerClass);
					historyHandler.rejectLogFor(referenceAndLocation.reference, propCtx, 0, isolatedCollector);
				}
			}
			
			affectedReferences.clear();
			flushedUndoableTransactionsFromReferences.clear();
			referencesToAppliedHistoryHandlers.clear();
			historyHandlerClassToInstanceMap.clear();

			if(onAfterNextTrigger.size() > 0) {
				triggerHandler.handleAfterTrigger(onAfterNextTrigger);
			}
		}
	}
	
	@Override
	public Connection<T> createConnection(TriggerHandler<T> flushHandler) {
		return new Connection<T>(this, flushHandler);
	}
}
