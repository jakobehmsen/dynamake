package dynamake;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SnapshottingPrevaylerService<T> implements PrevaylerService<T> {
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
	
	public SnapshottingPrevaylerService(Func0<T> prevalentSystemFunc) throws Exception {
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
		
//		if(journalExisted) {
//			FileInputStream fileOutput = new FileInputStream(journalDirectory + "/" + "log.jnl");
//			BufferedInputStream bufferedOutput = new BufferedInputStream(fileOutput);
//			
//			while(bufferedOutput.available() != 0) {
//				// Should be read in chunks
//				ObjectInputStream objectOutput = new ObjectInputStream(bufferedOutput);
//				Transaction<T> transaction = (Transaction<T>)objectOutput.readObject();
//				transaction.executeOn(prevalentSystem, null);
//			}
//			
//			bufferedOutput.close();
//		}
		
		if(snapshotExisted) {
			prevalentSystem = loadSnapshot(prevalanceDirectory + "/" + snapshotFile);
		} else
			prevalentSystem = prevalentSystemFunc.call();
		
		if(journalExisted)
			replay(new PropogationContext(), prevalentSystem, prevalanceDirectory + "/" + journalFile);
	}
	
	private static <T> T loadAndReplay(PropogationContext propCtx, Func0<T> prevalantSystemFunc, String journalPath, String snapshotPath) throws ClassNotFoundException, IOException {
		T prevalantSystem;
		
		Path snapshotFilePath = Paths.get(snapshotPath);
		
		if(java.nio.file.Files.exists(snapshotFilePath))
			prevalantSystem = loadSnapshot(snapshotPath);
		else
			prevalantSystem = prevalantSystemFunc.call();
		
		replay(propCtx, prevalantSystem, journalPath);
		
		return prevalantSystem;
	}
	
	private static <T> void replay(PropogationContext propCtx, T prevalentSystem, String journalPath) throws ClassNotFoundException, IOException {
		FileInputStream fileOutput = new FileInputStream(journalPath);
		BufferedInputStream bufferedOutput = new BufferedInputStream(fileOutput);
		
		while(bufferedOutput.available() != 0) {
			// Should be read in chunks
			ObjectInputStream objectOutput = new ObjectInputStream(bufferedOutput);
			DualCommand<T> transaction = (DualCommand<T>)objectOutput.readObject();
			transaction.executeForwardOn(propCtx, prevalentSystem, null, null);
		}
		
		bufferedOutput.close();
	}
	
	private static <T> T loadSnapshot(String snapshotPath) throws IOException, ClassNotFoundException {
		FileInputStream fileOutput = new FileInputStream(snapshotPath);
		BufferedInputStream bufferedOutput = new BufferedInputStream(fileOutput);

		ObjectInputStream objectOutput = new ObjectInputStream(bufferedOutput);
		T snapshot = (T)objectOutput.readObject();
		
		bufferedOutput.close();
		
		return snapshot;
	}
	
	private static <T> void saveSnapshot(PropogationContext propCtx, Func0<T> prevalantSystemFunc, String journalPath, String snapshotPath) throws ClassNotFoundException, IOException, ParseException {
		// Close journal
		Path currentJournalFilePath = Paths.get(journalPath);
		
//		Date now = new Date();
//		String nowFormatted = new DateFormatter().valueToString(now);
//		String nowFormatted = now.toString().replace(":", "_").replace(" ", "_");
		String nowFormatted = "" + System.nanoTime();
		Path closedJournalFilePath = Paths.get(currentJournalFilePath.getParent() + "/" + nowFormatted + currentJournalFilePath.getFileName());
		
		java.nio.file.Files.move(currentJournalFilePath, closedJournalFilePath);
		
		// Start new journal
		java.nio.file.Files.createFile(currentJournalFilePath);
		
		// Close snapshot and load copy of last snapshot (if any) and replay missing transactions;
		Path currentSnapshotFilePath = Paths.get(snapshotPath);
		Path closedSnapshotFilePath = Paths.get(currentSnapshotFilePath.getParent() + "/" + nowFormatted + currentSnapshotFilePath.getFileName());
		if(java.nio.file.Files.exists(currentSnapshotFilePath))
			java.nio.file.Files.move(currentSnapshotFilePath, closedSnapshotFilePath);
		
		// Load copy of last snapshot (if any) and replay missing transactions;
		T prevalantSystem = loadAndReplay(propCtx, prevalantSystemFunc, closedJournalFilePath.toString(), closedSnapshotFilePath.toString());
		
		
		// Save modified snapshot
		FileOutputStream fileOutput = new FileOutputStream(snapshotPath, true);
		BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput);
		ObjectOutputStream objectOutput = new ObjectOutputStream(bufferedOutput);
		
		objectOutput.writeObject(prevalantSystem);
		
		objectOutput.close();
	}
	
	private void saveSnapshot(PropogationContext propCtx) throws ClassNotFoundException, IOException, ParseException {
		saveSnapshot(propCtx, prevalentSystemFunc, prevalanceDirectory + "/" + journalFile, prevalanceDirectory + "/" + snapshotFile);
	}
	
	private int transactionIndex;
	private ArrayList<DualCommand<T>> transactions = new ArrayList<DualCommand<T>>();
	
	@Override
	public void undo(final PropogationContext propCtx) {
		transactionExecutor.execute(new Runnable() {
			@Override
			public void run() {
				if(transactionIndex > 0) {
					transactionIndex--;
					DualCommand<T> transaction = transactions.get(transactionIndex);
					transaction.executeBackwardOn(propCtx, prevalentSystem, null, null);
				}
			}
		});
	}

	@Override
	public void redo(final PropogationContext propCtx) {
		transactionExecutor.execute(new Runnable() {
			@Override
			public void run() {
				if(transactionIndex < transactions.size()) {
					DualCommand<T> transaction = transactions.get(transactionIndex);
					transaction.executeForwardOn(propCtx, prevalentSystem, null, null);
					transactionIndex++;
				}
			}
		});
	}

//	private static void startJournal(String journalPath) throws IOException {
//		Path journalFilePath = Paths.get(journalPath);
//		java.nio.file.Files.createFile(journalFilePath);
//	}
//
//	private static String closeJournal(String prevalanceDirectory) throws ParseException, IOException {
//		Path sourceJournalFilePath = Paths.get(prevalanceDirectory + "/" + journalFile);
//		Date now = new Date();
//		String nowFormatted = new DateFormatter().valueToString(now);
//		Path targetJournalFilePath = Paths.get(prevalanceDirectory + "/" + nowFormatted + journalFile);
//		
//		java.nio.file.Files.move(sourceJournalFilePath, targetJournalFilePath);
//	}

	public void executeTransient(Runnable runnable) {
		transactionExecutor.execute(runnable);
	}
	
	private void registerTransaction(final DualCommand<T> transaction) {
		if(transactionIndex == transactions.size())
			transactions.add(transaction);
		else
			transactions.set(transactionIndex, transaction);
		transactionIndex++;
	}
	
	public void persistTransaction(final PropogationContext propCtx, final DualCommand<T> transaction) {
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
	
	private static class Connection<T> implements PrevaylerServiceConnection<T> {
		private SnapshottingPrevaylerService<T> prevaylerService;
		private ArrayList<DualCommand<T>> transactionSequence = new ArrayList<DualCommand<T>>();

		public Connection(SnapshottingPrevaylerService<T> prevaylerService) {
			this.prevaylerService = prevaylerService;
		}

		// TODO: Replace usage of execute with execute2
		
		@Override
		public void execute(final PropogationContext propCtx, final DualCommandFactory<T> transactionFactory) {
			prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					ArrayList<DualCommand<T>> createdTransactions = new ArrayList<DualCommand<T>>();
					transactionFactory.createDualCommands(createdTransactions);
					
					for(DualCommand<T> transaction: createdTransactions) {
						transaction.executeForwardOn(propCtx, prevaylerService.prevalentSystem(), null, SnapshottingPrevaylerService.Connection.this);
						transactionSequence.add(transaction);
					}
				}
			});
		}
		
		@Override
		public void execute2(final PropogationContext propCtx, final DualCommandFactory2<T> transactionFactory) {
			prevaylerService.transactionExecutor.execute(new Runnable() {
				@SuppressWarnings("unchecked")
				@Override
				public void run() {
					ArrayList<Command<T>> createdForwardTransactions = new ArrayList<Command<T>>();
					ArrayList<Command<T>> createdBackwardTransactions = new ArrayList<Command<T>>();
					
					transactionFactory.createForwardTransactions(createdForwardTransactions);
					transactionFactory.createBackwardTransactions(createdBackwardTransactions);
					
					for(Command<T> forward: createdForwardTransactions)
						forward.executeOn(propCtx, prevaylerService.prevalentSystem(), null, SnapshottingPrevaylerService.Connection.this);
					
					transactionSequence.add(new DualCommandPair2<T>(
						createdForwardTransactions.toArray((Command<T>[])new Command<?>[createdForwardTransactions.size()]),
						createdBackwardTransactions.toArray((Command<T>[])new Command<?>[createdBackwardTransactions.size()])
					));
				}
			});
		}

		@Override
		public void commit(final PropogationContext propCtx) {
			prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
//					DualCommand<T>[] transactions = (DualCommand<T>[])new DualCommand<?>[transactionSequence.size()];
//					transactionSequence.toArray(transactions);
//					DualCommandSequence<T> compositeTransaction = new DualCommandSequence<T>(transactions);
//					prevaylerService.registerTransaction(compositeTransaction);
//					prevaylerService.persistTransaction(propCtx, compositeTransaction);
//					transactionSequence = null;
					commit(propCtx, SnapshottingPrevaylerService.Connection.this);
				}
			});
		}
		
		private static <T> void commit(final PropogationContext propCtx, SnapshottingPrevaylerService.Connection<T> connection) {
			DualCommand<T>[] transactions = (DualCommand<T>[])new DualCommand<?>[connection.transactionSequence.size()];
			connection.transactionSequence.toArray(transactions);
			DualCommandSequence<T> compositeTransaction = new DualCommandSequence<T>(transactions);
			connection.prevaylerService.registerTransaction(compositeTransaction);
			connection.prevaylerService.persistTransaction(propCtx, compositeTransaction);
			connection.transactionSequence = null;
			
			if(connection.branches != null) {
				for(SnapshottingPrevaylerService.Connection<T> branch: connection.branches)
					commit(propCtx, branch);
			}
		}

		@Override
		public void rollback(final PropogationContext propCtx) {
			prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					// Execute in reverse order
					for(int i = transactionSequence.size() - 1; i >= 0; i--) {
						DualCommand<T> part = transactionSequence.get(i);
						part.executeBackwardOn(propCtx, prevaylerService.prevalentSystem(), null, null);
					}
					transactionSequence = null;
				}
			});
		}
		
		private PrevaylerServiceConnection<T> parent;
		private int branchAbsorbCount;
		private SnapshottingPrevaylerService.Connection<T>[] branches;
		
		@Override
		public void absorb() {
			if(branches != null) {
				int newBranchAbsorbCount;
				synchronized (this) {
					branchAbsorbCount++;
					newBranchAbsorbCount = branchAbsorbCount;
				}
				
				if(newBranchAbsorbCount == branches.length) {
					if(parent != null)
						parent.absorb();
					else {
						// Every effect has been absorbed
						commit(null);
					}
				}
			} else {
				if(parent != null)
					parent.absorb();
				else {
					// Every effect has been absorbed
					commit(null);
				}
			}
		}
		
		@Override
		public PrevaylerServiceConnection<T>[] branch(int branchCount) {
			branches = (SnapshottingPrevaylerService.Connection<T>[])new SnapshottingPrevaylerService.Connection<?>[branchCount];
			
			for(int i = 0; i < branchCount; i++) {
				branches[i] = new SnapshottingPrevaylerService.Connection<T>(prevaylerService);
				branches[i].parent = this;
			}

			return branches;
		}
	}
	
	@Override
	public PrevaylerServiceConnection<T> createConnection() {
		return new SnapshottingPrevaylerService.Connection<T>(this);
	}
	
	private static class Branch<T> implements PrevaylerServiceBranch<T> {
		private Branch<T> parent;
		private SnapshottingPrevaylerService<T> prevaylerService;
		private ArrayList<DualCommand<T>> transactionSequence;
		private int branchAbsorbCount;
		private ArrayList<SnapshottingPrevaylerService.Branch<T>> branches = new ArrayList<SnapshottingPrevaylerService.Branch<T>>();
		
		private Branch(Branch<T> parent, SnapshottingPrevaylerService<T> prevaylerService, final DualCommandFactory<T> transactionFactory, final PropogationContext propCtx) {
			this.parent = parent;
			this.prevaylerService = prevaylerService;
			
			this.prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					ArrayList<DualCommand<T>> createdTransactions = new ArrayList<DualCommand<T>>();
					transactionFactory.createDualCommands(createdTransactions);
					
					for(DualCommand<T> transaction: createdTransactions) {
						transaction.executeForwardOn(propCtx, Branch.this.prevaylerService.prevalentSystem(), null, null);
						transactionSequence.add(transaction);
					}
				}
			});
		}
		
		public void commit(final PropogationContext propCtx) {
			prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					commit(propCtx, SnapshottingPrevaylerService.Branch.this);
				}
			});
		}
		
		private static <T> void commit(final PropogationContext propCtx, SnapshottingPrevaylerService.Branch<T> connection) {
			DualCommand<T>[] transactions = (DualCommand<T>[])new DualCommand<?>[connection.transactionSequence.size()];
			connection.transactionSequence.toArray(transactions);
			DualCommandSequence<T> compositeTransaction = new DualCommandSequence<T>(transactions);
			connection.prevaylerService.registerTransaction(compositeTransaction);
			connection.prevaylerService.persistTransaction(propCtx, compositeTransaction);
			connection.transactionSequence = null;
			
			if(connection.branches.size() > 0) {
				for(SnapshottingPrevaylerService.Branch<T> branch: connection.branches)
					commit(propCtx, branch);
			}
		}

		@Override
		public void absorb() {
			this.prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					if(branches.size() > 0) {
						branchAbsorbCount++;
						
						if(branchAbsorbCount == branches.size()) {
							if(parent != null)
								parent.absorb();
							else {
								// Every effect has been absorbed
								commit(null);
							}
						}
					} else {
						if(parent != null)
							parent.absorb();
						else {
							// Every effect has been absorbed
							commit(null);
						}
					}
				}
			});
		}

		@Override
		public void reject() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public PrevaylerServiceBranch<T> branch(PropogationContext propCtx,
				DualCommandFactory<T> transactionFactory) {
			final SnapshottingPrevaylerService.Branch<T> branch = new Branch<T>(this, prevaylerService, transactionFactory, propCtx);
			
			this.prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					Branch.this.branches.add(branch);
				}
			});
			
			return branch;
		}
	}
	
	@Override
	public PrevaylerServiceBranch<T> createBranch(PropogationContext propCtx, DualCommandFactory<T> transactionFactory) {
		return new SnapshottingPrevaylerService.Branch<T>(null, this, transactionFactory, propCtx);
	}
}
