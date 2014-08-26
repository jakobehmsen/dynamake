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
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dynamake.commands.CommandStateWithOutput;
import dynamake.commands.ContextualCommand;
import dynamake.commands.CommandState;
import dynamake.commands.ExecutionScope;
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
	private static <T> void replay(ContextualCommand<T> ctxTransaction, T prevalentSystem, PropogationContext propCtx, Collector<T> isolatedCollector) {
		TransactionHandler<T> transactionHandler = ctxTransaction.transactionHandlerFactory.createTransactionHandler();
		ExecutionScope scope = transactionHandler.getScope();

		Location locationFromReference = new ModelRootLocation();
		T reference = (T)ctxTransaction.locationFromRootToReference.getChild(prevalentSystem);
		
		transactionHandler.startLogFor(reference);
		
		for(Object transactionFromRoot: ctxTransaction.transactionsFromRoot) {
			if(transactionFromRoot instanceof SnapshottingTranscriber.Connection.LocationCommandsPair) {
				SnapshottingTranscriber.Connection.LocationCommandsPair<T> entry = (SnapshottingTranscriber.Connection.LocationCommandsPair<T>)transactionFromRoot;
				
				ArrayList<Execution<T>> pendingUndoablePairs = new ArrayList<Execution<T>>();
				for(CommandState<T> transaction: entry.pending) {
					CommandStateWithOutput<T> undoable = (CommandStateWithOutput<T>)transaction.executeOn(propCtx, reference, isolatedCollector, locationFromReference, scope);
					pendingUndoablePairs.add(new Execution<T>(transaction, undoable));
				}
				
				transactionHandler.logFor((T)reference, pendingUndoablePairs, propCtx, 0, isolatedCollector);
			} else if(transactionFromRoot instanceof ContextualCommand) {
				replay((ContextualCommand<T>)transactionFromRoot, prevalentSystem, propCtx, isolatedCollector);
			}
		}
		
		transactionHandler.commitLogFor(reference);
	}
	
	private static <T> void replay(ArrayList<ContextualCommand<T>> transactions, T prevalentSystem) {
		PropogationContext propCtx = new PropogationContext();
		
		Collector<T> isolatedCollector = new NullCollector<T>();
		
		for(ContextualCommand<T> ctxTransaction: transactions)
			replay(ctxTransaction, prevalentSystem, propCtx, isolatedCollector);
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
		public static final int OPCODE_START = 0;
		public static final int OPCODE_COMMIT = 1;
		public static final int OPCODE_REJECT = 2;
		public static final int OPCODE_FLUSH_NEXT_TRIGGER = 3;
		public static final int OPCODE_SEND_PROPOGATION_FINISHED = 4;
		
		public final int type;
		public final Object operand1;
		public final Object operand2;
		
		public Instruction(int type, Object operand1) {
			this.type = type;
			this.operand1 = operand1;
			this.operand2 = null;
		}
		
		public Instruction(int type, Object operand1, Object operand2) {
			this.type = type;
			this.operand1 = operand1;
			this.operand2 = operand2;
		}
	}
	
	public static class Connection<T> implements dynamake.transcription.Connection<T> {
		// Probably, many of the below fields are to be related to "transaction frame" of some sort
		private TriggerHandler<T> triggerHandler;
		private SnapshottingTranscriber<T> transcriber;
		private TransactionFrame<T> currentFrame;
		
		public Connection(SnapshottingTranscriber<T> transcriber, TriggerHandler<T> triggerHandler) {
			this.transcriber = transcriber;
			this.triggerHandler = triggerHandler;
		}
		
		private static class TransactionFrame<T> {
			public final TransactionFrame<T> parent;
			public final T reference;
			public final Location locationFromRootToReference;
			public final TransactionHandlerFactory<T> handlerFactory;
			public final TransactionHandler<T> handler;
			
			// Somehow, flushed transaction should both be able to consist of atomic commands and committed transactions
			// This is needed in order to keep the order of the flushed commands/transaction correct
			// - and this is important during replay and reject
			public ArrayList<Object> flushedTransactionsFromRoot = new ArrayList<Object>(); // List of either atomic commands or transactions
			public ArrayList<Object> flushedUndoableTransactionsFromReferences = new ArrayList<Object>();  // List of either atomic commands or transactions
			
			public TransactionFrame(TransactionFrame<T> parent, T reference, Location locationFromRootToReference, TransactionHandlerFactory<T> transactionHandlerFactory, TransactionHandler<T> handler) {
				this.parent = parent;
				this.reference = reference;
				this.locationFromRootToReference = locationFromRootToReference;
				this.handlerFactory = transactionHandlerFactory;
				this.handler = handler;
			}
		}
		
		public static class LocationCommandsPair<T> implements Serializable {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			public final Location location;
			public final ArrayList<CommandState<T>> pending;
			public final TransactionHandlerFactory<T> transactionHandlerFactory;
			
			public LocationCommandsPair(Location location, ArrayList<CommandState<T>> pending, TransactionHandlerFactory<T> transactionHandlerFactory) {
				this.location = location;
				this.pending = pending;
				this.transactionHandlerFactory = transactionHandlerFactory;
			}
		}
		
		private static class UndoableCommandFromReference<T> {
			@SuppressWarnings("unused")
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
					final PropogationContext propCtx = new PropogationContext();
					final LinkedList<Object> commands = new LinkedList<Object>();
					commands.add(trigger);
					
					Stack<List<Execution<T>>> propogationStack = new Stack<List<Execution<T>>>();
					final ArrayList<Runnable> onAfterNextTrigger = new ArrayList<Runnable>();
					
					while(!commands.isEmpty()) {
						Object command = commands.pop();
						
						final ArrayList<Object> collectedCommands = new ArrayList<Object>();
						Collector<T> collector = new Collector<T>() {
							@Override
							public void startTransaction(T reference, Object transactionHandlerClass) {
								TransactionHandlerFactory<T> transactionHandlerFactory;
								if(transactionHandlerClass instanceof Class)
									transactionHandlerFactory = new ReflectedTransactionHandlerFactory<T>((Class<? extends TransactionHandler<T>>)transactionHandlerClass);
								else
									transactionHandlerFactory = (TransactionHandlerFactory<T>)transactionHandlerClass;
								collectedCommands.add(new Instruction(Instruction.OPCODE_START, reference, transactionHandlerFactory));
							}
							
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
							public void commitTransaction() {
								collectedCommands.add(Instruction.OPCODE_COMMIT);
							}
							
							@Override
							public void rejectTransaction() {
								collectedCommands.add(Instruction.OPCODE_REJECT);
							}
							
							@Override
							public void afterNextTrigger(Runnable runnable) {
								onAfterNextTrigger.add(runnable);
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
							case Instruction.OPCODE_START:
								T reference = (T)instruction.operand1;
								TransactionHandlerFactory<T> transactionHandlerFactory = (TransactionHandlerFactory<T>)instruction.operand2;
								
								TransactionHandler<T> transactionHandler = transactionHandlerFactory.createTransactionHandler();
									
								transactionHandler.startLogFor(reference);
								
								Location locationFromRoot = ((Model)reference).getLocator().locate();
								
								currentFrame = new TransactionFrame<T>(currentFrame, reference, locationFromRoot, transactionHandlerFactory, transactionHandler);

								break;
							case Instruction.OPCODE_SEND_PROPOGATION_FINISHED:
								List<Execution<T>> pendingUndoablePairs = propogationStack.pop();
								if(pendingUndoablePairs.size() != 1) {
									new String();
								}
								Execution<T> execution = pendingUndoablePairs.get(0);
								((PendingCommandFactory<T>)instruction.operand1).afterPropogationFinished(execution, propCtx, 0, collector);
								break;
							}
						} else if(command instanceof PendingCommandFactory) {
							TransactionFrame<T> frame = currentFrame;
							
							PendingCommandFactory<T> transactionFactory = (PendingCommandFactory<T>)command;
							T reference = frame.reference;
							
							TransactionHandler<T> transactionHandler = frame.handler;
							
							Location locationFromReference = new ModelRootLocation();
							
							// If location was part of the executeOn invocation, location is probably no
							// necessary for creating dual commands. Further, then, it is probably not necessary
							// to create two sequences of pendingCommands.
							CommandState<T> pendingCommand = transactionFactory.createPendingCommand();
							
							// Assumed that exactly one command is created consistenly
							
							// Should be in pending state
							ArrayList<CommandState<T>> undoables = new ArrayList<CommandState<T>>();

							ExecutionScope scope = transactionHandler.getScope();
							// The command in pending state should return a command in undoable state
							CommandState<T> undoableCommand = pendingCommand.executeOn(propCtx, reference, collector, locationFromReference, scope);
							undoables.add(undoableCommand);

							frame.flushedUndoableTransactionsFromReferences.add(new UndoableCommandFromReference<T>(reference, undoables));
							
							ArrayList<Execution<T>> pendingUndoablePairs = new ArrayList<Execution<T>>();

							CommandStateWithOutput<T> undoable = (CommandStateWithOutput<T>)undoables.get(0);
							Execution<T> pendingUndoablePair = new Execution<T>(pendingCommand, undoable);
							pendingUndoablePairs.add(pendingUndoablePair);
							
							transactionHandler.logFor(reference, pendingUndoablePairs, propCtx, 0, collector);

							ArrayList<CommandState<T>> pendingCommands = new ArrayList<CommandState<T>>();
							pendingCommands.add(pendingCommand);
							frame.flushedTransactionsFromRoot.add(new LocationCommandsPair<T>(null, pendingCommands, null));
							
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
			if(currentFrame.flushedTransactionsFromRoot.size() > 0) {
				final ArrayList<Runnable> onAfterNextTrigger = new ArrayList<Runnable>();
				
				currentFrame.handler.commitLogFor(currentFrame.reference);
				
				ArrayList<Object> transactionsFromRoot = new ArrayList<Object>();
				
				buildTransactionToPersist(transactionsFromRoot, currentFrame);
				
				ContextualCommand<T> transactionToPersist = new ContextualCommand<T>(currentFrame.locationFromRootToReference, currentFrame.handlerFactory, transactionsFromRoot);
				
				if(currentFrame.parent == null) {
	//				System.out.println("Committed connection");
					transcriber.persistTransaction(transactionToPersist);
					
					if(onAfterNextTrigger.size() > 0) {
						triggerHandler.handleAfterTrigger(onAfterNextTrigger);
					}
				} else {
					currentFrame.parent.flushedTransactionsFromRoot.add(currentFrame);
					currentFrame.parent.flushedUndoableTransactionsFromReferences.add(currentFrame);
				}
			}
			
			currentFrame = currentFrame.parent;
		}
		
		@SuppressWarnings("unchecked")
		private void buildTransactionToPersist(ArrayList<Object> transactionsFromRoot, TransactionFrame<T> frame) {
			for(Object committedExecution: frame.flushedTransactionsFromRoot) {
				if(committedExecution instanceof LocationCommandsPair) {
					transactionsFromRoot.add((LocationCommandsPair<T>)committedExecution);
				} else if(committedExecution instanceof TransactionFrame) {
					TransactionFrame<T> innerFrame = (TransactionFrame<T>)committedExecution;
					ArrayList<Object> innerTransactionsFromRoot = new ArrayList<Object>();
					
					buildTransactionToPersist(innerTransactionsFromRoot, innerFrame);
					
					transactionsFromRoot.add(new ContextualCommand<T>(innerFrame.locationFromRootToReference, innerFrame.handlerFactory, innerTransactionsFromRoot));
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
			
			currentFrame.handler.rejectLogFor(currentFrame.reference);

			while(currentFrame != null) {
				rejectTransaction(propCtx, isolatedCollector, currentFrame);
				
				currentFrame = currentFrame.parent;
			}
			
			System.out.println("Rejected connection");

			if(onAfterNextTrigger.size() > 0) {
				triggerHandler.handleAfterTrigger(onAfterNextTrigger);
			}
		}
		
		private static <T> void rejectTransaction(PropogationContext propCtx, Collector<T> isolatedCollector, TransactionFrame<T> frame) {
			ExecutionScope scope = frame.handler.getScope();
			for(int i = frame.flushedUndoableTransactionsFromReferences.size() - 1; i >= 0; i--) {
				Object committedExecution = frame.flushedUndoableTransactionsFromReferences.get(i);
				
				if(committedExecution instanceof UndoableCommandFromReference) {
					@SuppressWarnings("unchecked")
					UndoableCommandFromReference<T> undoableTransaction = (UndoableCommandFromReference<T>)committedExecution;
					
					Location locationFromReference = new ModelRootLocation();
					for(CommandState<T> undoable: undoableTransaction.undoables) {
						@SuppressWarnings("unused")
						CommandState<T> redoable = undoable.executeOn(propCtx, frame.reference, isolatedCollector, locationFromReference, scope);
					}
				} else if(committedExecution instanceof TransactionFrame) {
					@SuppressWarnings("unchecked")
					TransactionFrame<T> innerFrame = (TransactionFrame<T>)committedExecution;
					
					rejectTransaction(propCtx, isolatedCollector, innerFrame);
				}
			}
		}
	}
	
	@Override
	public Connection<T> createConnection(TriggerHandler<T> flushHandler) {
		return new Connection<T>(this, flushHandler);
	}
}
