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
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dynamake.commands.ContextualTransaction;
import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandSequence;
import dynamake.delegates.Func0;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelLocator;
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
			ArrayList<ContextualTransaction<T>> transactions = readJournal(prevalanceDirectory + "/" + journalFile);
			replay(transactions, new PropogationContext(), prevalentSystem);
			// Update the number of enlisted transactions which is used in the snapshotting logic
			transactionEnlistingCount += transactions.size();
		}
	}
	
	private static <T> Snapshot<T> loadAndReplay(PropogationContext propCtx, Func0<T> prevalantSystemFunc, String journalPath, String snapshotPath) throws ClassNotFoundException, IOException {
		Snapshot<T> snapshot;
		
		Path snapshotFilePath = Paths.get(snapshotPath);
		
		if(java.nio.file.Files.exists(snapshotFilePath))
			snapshot = loadSnapshot(snapshotPath);
		else {
			T prevalantSystem = prevalantSystemFunc.call();
			
			snapshot = new Snapshot<T>(prevalantSystem);
		}
		
		replay(propCtx, snapshot.prevalentSystem, journalPath);
		
		return snapshot;
	}
	
	private static <T> ArrayList<ContextualTransaction<T>> readJournal(String journalPath) throws ClassNotFoundException, IOException {
		ArrayList<ContextualTransaction<T>> transactions = new ArrayList<ContextualTransaction<T>>();
		
		FileInputStream fileOutput = new FileInputStream(journalPath);
		BufferedInputStream bufferedOutput = new BufferedInputStream(fileOutput);
		
		try {
			while(bufferedOutput.available() != 0) {
				// Should be read in chunks
				ObjectInputStream objectOutput = new ObjectInputStream(bufferedOutput);
				@SuppressWarnings("unchecked")
				ContextualTransaction<T> transaction = (ContextualTransaction<T>)objectOutput.readObject();
					
				transactions.add(transaction);
			}
		} finally {
			bufferedOutput.close();
		}
		
		return transactions;
	}
	
	@SuppressWarnings("unchecked")
	private static <T> void replay(ArrayList<ContextualTransaction<T>> transactions, PropogationContext propCtx, T prevalentSystem) {
		for(ContextualTransaction<T> ctxTransaction: transactions) {
			DualCommand<T> transaction = ctxTransaction.transaction;

			transaction.executeForwardOn(propCtx, prevalentSystem, null, new NullCollector<T>());
			
			for(Location affectedModelLocation: ctxTransaction.affectedModelLocations) {
				// TODO: Abstracted the following code to reduce coupling to models.
				// What kind of interface could be applicable here?
				Model affectedModel = (Model)affectedModelLocation.getChild(prevalentSystem);
				affectedModel.log((ContextualTransaction<Model>)ctxTransaction);
			}
		}
	}
	
	private static <T> void replay(PropogationContext propCtx, T prevalentSystem, String journalPath) throws ClassNotFoundException, IOException {
		ArrayList<ContextualTransaction<T>> transactions = readJournal(journalPath);
		replay(transactions, new PropogationContext(), prevalentSystem);
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
	
	private static <T> void saveSnapshot(PropogationContext propCtx, Func0<T> prevalantSystemFunc, String journalPath, String snapshotPath) throws ClassNotFoundException, IOException, ParseException {
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
		Snapshot<T> snapshot = loadAndReplay(propCtx, prevalantSystemFunc, closedJournalFilePath.toString(), closedSnapshotFilePath.toString());
		
		// Save modified snapshot
		FileOutputStream fileOutput = new FileOutputStream(snapshotPath, true);
		BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput);
		ObjectOutputStream objectOutput = new ObjectOutputStream(bufferedOutput);
		
		objectOutput.writeObject(snapshot);
		
		objectOutput.close();
	}
	
	private void saveSnapshot(PropogationContext propCtx) throws ClassNotFoundException, IOException, ParseException {
		saveSnapshot(propCtx, prevalentSystemFunc, prevalanceDirectory + "/" + journalFile, prevalanceDirectory + "/" + snapshotFile);
	}

	public void executeTransient(Runnable runnable) {
		transactionExecutor.execute(runnable);
	}
	
	public void persistTransaction(final PropogationContext propCtx, final ContextualTransaction<T> transaction) {
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
				System.out.println("Persisted transaction.");
				
				transactionEnlistingCount++;
				if(transactionEnlistingCount >= snapshotThreshold) {
					System.out.println("Enlisted snapshot on thread " + Thread.currentThread().getId());
					// TODO: Consider: Should an isolated propogation context created here? I.e., a snapshot propogation context?
					try {
						// Could be separated into the following:
						// Close latest journal and snapshot 
						// With other execution service: Save snapshot based on closed journal and snapshot
						saveSnapshot(propCtx);
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
	
	private static class Connection<T> implements TranscriberConnection<T> {
		private TriggerHandler<T> flushHandler;
		private SnapshottingTranscriber<T> transcriber;
		private ArrayList<Object> enlistings = new ArrayList<Object>();
		private ArrayList<DualCommand<T>> flushedTransactions = new ArrayList<DualCommand<T>>();
		private HashSet<T> affectedModels = new HashSet<T>();
		
		public Connection(SnapshottingTranscriber<T> transcriber, TriggerHandler<T> flushHandler) {
			this.transcriber = transcriber;
			this.flushHandler = flushHandler;
		}
		
		private PropogationContext propCtx = new PropogationContext();
		
		private ArrayList<Runnable> onFlush = new ArrayList<Runnable>();

		@Override
		public void trigger(final Trigger<T> trigger) {
			this.transcriber.transactionExecutor.execute(new Runnable() {
				@SuppressWarnings("unchecked")
				@Override
				public void run() {
					final LinkedList<Object> commands = new LinkedList<Object>();
					commands.add(trigger);
					
					while(!commands.isEmpty()) {
						Object command = commands.pop();
						
						TranscriberCollector<T> collector = new TranscriberCollector<T>() {
							@Override
							public void enlist(DualCommandFactory<T> transactionFactory) {
								enlistings.add(transactionFactory);
							}
							
							@Override
							public void execute( DualCommandFactory<T> transactionFactory) {
								commands.add(transactionFactory);
							}
							
							@Override
							public void afterNextTrigger(Runnable runnable) {
								onFlush.add(runnable);
							}

							@Override
							public void registerAffectedModel(T model) {
								affectedModels.add(model);
							}
							
							@Override
							public void enlistReject() {
								enlistings.add(0);
							}
							
							@Override
							public void enlistCommit() {
								enlistings.add(1);
							}
							
							@Override
							public void flush() {
								commands.add(2);
							}
						};
						
						if(command instanceof Integer) {
							int i = (int)command;
							
							switch(i) {
							case 0: // reject
								doReject();
								break;
							case 1: // commit
								doCommit();
								break;
							case 2: // flush
								commands.addAll(enlistings);
								enlistings.clear();
								break;
							}
						} else if(command instanceof DualCommandFactory) {
							DualCommandFactory<T> transactionFactory = (DualCommandFactory<T>)command;
							
							ArrayList<DualCommand<T>> dualCommands = new ArrayList<DualCommand<T>>();
							transactionFactory.createDualCommands(dualCommands);
	
							for(DualCommand<T> transaction: dualCommands) {
								transaction.executeForwardOn(propCtx, transcriber.prevalentSystem, null, collector);
								flushedTransactions.add(transaction);
							}
						} else if(command instanceof Trigger) {
							((Trigger<T>)command).run(collector);
						}
					}

					if(onFlush.size() > 0) {
						final ArrayList<Runnable> localOnFlush = new ArrayList<Runnable>(onFlush);
						flushHandler.handleAfterTrigger(localOnFlush);
						onFlush.clear();
					}
				}
			});
		}
		
		@SuppressWarnings("unchecked")
		private void doCommit() {
			if(flushedTransactions.size() > 0) {
				DualCommand<T>[] dualCommandArray = flushedTransactions.toArray(new DualCommand[flushedTransactions.size()]);
				flushedTransactions.clear();
				
				DualCommandSequence<T> transaction = new DualCommandSequence<T>(dualCommandArray);
				
				ArrayList<Location> affectedModelLocations = new ArrayList<Location>();
				for(T affectedModel: affectedModels) {
					// TODO: Decouple from Model further
					// E.g. by introducing an interface for the getLocator() method 
					Model affectedModelAsModel = (Model)affectedModel;
					ModelLocator locator = affectedModelAsModel.getLocator();
					if(locator != null) {
						// Only affected models which still "physically" exists are collected for
						// affected models to persist along with the transaction.
						affectedModelLocations.add(locator.locate());
					}
				}
				
				ContextualTransaction<T> ctxTransaction = new ContextualTransaction<T>(transaction, affectedModelLocations);
				
				for(T affectedModel: affectedModels) {
					// TODO: Decouple from Model further
					// E.g. by introducing an interface for the log(...) method 
					Model affectedModelAsModel = (Model)affectedModel;
					ModelLocator locator = affectedModelAsModel.getLocator();
					if(locator != null) {
						// Only affected models which still "physically" exists are collected for
						// affected models to persist along with the transaction.
						affectedModelAsModel.log((ContextualTransaction<Model>)ctxTransaction);
					}
				}

				System.out.println("Committed connection: " + Connection.this);
				transcriber.persistTransaction(propCtx, ctxTransaction);
				affectedModels.clear();
			}
		}
		
		@SuppressWarnings("unchecked")
		private void doReject() {
			final ArrayList<Object> currentEnlistings = Connection.this.enlistings;
			
			Connection.this.enlistings = new ArrayList<Object>();
			
			PropogationContext propCtx = new PropogationContext();
			
			TranscriberCollector<T> isolatedCollector = new TranscriberCollector<T>() {
				@Override
				public void enlist(DualCommandFactory<T> transactionFactory) { }
				
				@Override
				public void execute(DualCommandFactory<T> transactionFactory) { }
				
				@Override
				public void afterNextTrigger(Runnable runnable) {
					currentEnlistings.add(runnable);
				}

				@Override
				public void registerAffectedModel(T model) { }
				
				@Override
				public void enlistReject() { }
				
				@Override
				public void enlistCommit() { }
				
				@Override
				public void flush() { }
			};
			
			for(DualCommand<T> transaction: flushedTransactions)
				transaction.executeBackwardOn(propCtx, transcriber.prevalentSystem, null, isolatedCollector);
			
			flushedTransactions.clear();
			
			while(currentEnlistings.size() > 0) {
				ArrayList<Object> enlistingsClone = (ArrayList<Object>)currentEnlistings.clone();
				currentEnlistings.clear();
				
				for(Object enlisting: enlistingsClone) {
					if(enlisting instanceof Trigger) {
						Trigger<T> trigger = (Trigger<T>)enlisting;
						trigger.run(isolatedCollector);
					}
				}
			}
		}
	}
	
	@Override
	public TranscriberConnection<T> createConnection(TriggerHandler<T> flushHandler) {
		return new Connection<T>(this, flushHandler);
	}
}
