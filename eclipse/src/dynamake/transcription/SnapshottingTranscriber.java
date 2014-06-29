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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dynamake.DualCommandFactory;
import dynamake.RunBuilder;
import dynamake.commands.ContextualTransaction;
import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandSequence;
import dynamake.delegates.Func0;
import dynamake.models.Location;
import dynamake.models.Model;
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

			IsolatedBranch<T> branch = new IsolatedBranch<T>();
			transaction.executeForwardOn(propCtx, prevalentSystem, null, branch);
			
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
	
	private static interface BranchParent {
		void doOnFinished(Runnable runnable);
	}
	
	private static class IsolatedBranch<T> implements TranscriberBranch<T>, BranchParent {
		private BranchParent parent;
		private RunBuilder finishedBuilder;
		
		public IsolatedBranch() { }
		
		public IsolatedBranch(BranchParent parent) {
			this.parent = parent;
		}
		
		@Override
		public void reject() { }
		
		private ArrayList<IsolatedBranch<T>> branches = new ArrayList<IsolatedBranch<T>>();

		@Override
		public TranscriberBranch<T> branch() {
			IsolatedBranch<T> branch = new IsolatedBranch<T>(this);
			branches.add(branch);
			return branch;
		}

		@Override
		public void execute(PropogationContext propCtx, DualCommandFactory<T> transactionFactory) { }

		@Override
		public void close() { }
		
		@Override
		public TranscriberBranch<T> isolatedBranch() {
			return branch();
		}
		
		@Override
		public void onFinished(Runnable runnable) {
			if(finishedBuilder != null)
				finishedBuilder.addRunnable(runnable);
			else {
				if(parent != null)
					parent.doOnFinished(runnable);
			}
		}
		
		@Override
		public void setOnFinishedBuilder(RunBuilder finishedBuilder) { 
			this.finishedBuilder = finishedBuilder;
		}
		
		@Override
		public boolean isIsolated() {
			return true;
		}
		
		@Override
		public void doOnFinished(Runnable runnable) {
			onFinished(runnable);
		}
		
		private HashSet<T> affectedModels = new HashSet<T>();

		@Override
		public void registerAffectedModel(T model) {
			affectedModels.add(model);
		}
		
		@Override
		public void addRegisteredAffectedModels(HashSet<T> allAffectedModels) {
			allAffectedModels.addAll(this.affectedModels);
			for(IsolatedBranch<T> branch: branches)
				branch.addRegisteredAffectedModels(allAffectedModels);
		}
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
	
	private static class Branch<T> implements TranscriberBranch<T>, BranchParent {
		private Branch<T> parent;
		private SnapshottingTranscriber<T> prevaylerService;
		private DualCommand<T> transaction;
		private ArrayList<SnapshottingTranscriber.Branch<T>> branches = new ArrayList<SnapshottingTranscriber.Branch<T>>();
		private ArrayList<SnapshottingTranscriber.Branch<T>> absorbedBranches = new ArrayList<SnapshottingTranscriber.Branch<T>>();
		private PropogationContext propCtx;
		private boolean rejected;
		
		private TranscriberBranchBehavior<T> behavior;
		
		private Branch(TranscriberBranchBehavior<T> behavior, SnapshottingTranscriber<T> prevaylerService, final PropogationContext propCtx) {
			this.behavior = behavior;
			this.prevaylerService = prevaylerService;
			this.propCtx = propCtx;
			
			if(parent == null)
				System.out.println("Started branch: " + this);
		}
		
		private Branch(Branch<T> parent, SnapshottingTranscriber<T> prevaylerService, final PropogationContext propCtx) {
			this.parent = parent;
			this.prevaylerService = prevaylerService;
			this.propCtx = propCtx;
			
			if(parent == null)
				System.out.println("Started branch: " + this);
		}
		
		private void commit(final PropogationContext propCtx) {
			prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					commit(propCtx, SnapshottingTranscriber.Branch.this);
				}
			});
		}
		
		@SuppressWarnings("unchecked")
		private static <T> void commit(final PropogationContext propCtx, SnapshottingTranscriber.Branch<T> branch) {
			DualCommand<T> reduction = branch.reduce();
			
			// Reduction is null if no transactions were performed that were to be persisted.
			// For instance, in the case of executing a transaction on an isolated branch.
			if(reduction != null) {
				
				HashSet<T> allAffectedModels = new HashSet<T>();
				
				branch.addRegisteredAffectedModels(allAffectedModels);
				
				ArrayList<Location> affectedModelLocations = new ArrayList<Location>();
				for(T affectedModel: allAffectedModels) {
					// TODO: Decouple from Model further
					// E.g. by introducing an interface for the getLocator() method 
					Model affectedModelAsModel = (Model)affectedModel;
					affectedModelLocations.add(affectedModelAsModel.getLocator().locate());
				}
				
				ContextualTransaction<T> ctxTransaction = new ContextualTransaction<T>(reduction, affectedModelLocations);
				
				for(T affectedModel: allAffectedModels) {
					// TODO: Decouple from Model further
					// E.g. by introducing an interface for the log(...) method 
					Model affectedModelAsModel = (Model)affectedModel;
					affectedModelAsModel.log((ContextualTransaction<Model>)ctxTransaction);
				}

				System.out.println("Committed branch: " + branch);
				branch.behavior.commit(propCtx, ctxTransaction);
			}
		}
		
		private DualCommand<T> reduce() {
			ArrayList<DualCommand<T>> transactionList = new ArrayList<DualCommand<T>>();
			
			if(transaction != null)
				transactionList.add(transaction);
			
			for(SnapshottingTranscriber.Branch<T> branch: absorbedBranches) {
				DualCommand<T> reduction = branch.reduce();
				
				if(reduction != null)
					transactionList.add(reduction);
			}
			
			if(transactionList.size() > 1) {
				@SuppressWarnings("unchecked")
				DualCommand<T>[] transactionArray = (DualCommand<T>[])new DualCommand<?>[transactionList.size()];
				transactionList.toArray(transactionArray);
				
				return new DualCommandSequence<T>(transactionArray);
			} else if(transactionList.size() == 1)
				return transactionList.get(0);
			return null;
		}
		
		private boolean isAbsorbed;
		
		private boolean hasSentFinished;
		
		private void sendFinished() {
			if(finishBuilder != null) {
				if(!hasSentFinished) {
					finishBuilder.execute();
					hasSentFinished = true;
				}
			}
		}
		
		private void doAbsorb() {
			if(!isAbsorbed) {
				if(parent != null) {
					parent.absorbBranch(Branch.this);
				} else {
					// Every effect has been absorbed
					commit(null);
				}
			
				isAbsorbed = true;
			} else {
				System.out.println("Attempted to absorb absorbed branch");
			}
		}
		
		private void absorbBranch(final Branch<T> branch) {
			absorbedBranches.add(branch);

			if(isClosed)
				checkAbsorbed();
		}
		
		private void checkAbsorbed() {
			if(absorbedBranches.size() == branches.size()) {
				doAbsorb();
				
				sendFinished();
			}
		}

		@Override
		public void reject() {
			this.prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					rejectSync();
				}
			});
		}
		
		private void rejectSync() {
			if(parent != null)
				parent.rejectSync();
			else {
				rejectDownwards();
			}
		}
		
		private void rejectDownwards() {
			rejected = true;
			
			if(finishBuilder != null) {
				if(!hasSentFinished) {
//					System.out.println("Sending finished before reject");
					finishBuilder.execute();
					hasSentFinished = false;
				}
				
				finishBuilder.clear();
			}
			
			// Reject in reverse order, i.e. start with the last branch first
			for(Branch<T> branch: branches)
				branch.rejectDownwards();
			
			if(transaction != null) {
				// In some cases, there are onFinished registrations made during rejects
				IsolatedBranch<T> backwardsBranch = new IsolatedBranch<T>(new BranchParent() {
					@Override
					public void doOnFinished(Runnable runnable) {
						Branch.this.doOnFinishedDirect(runnable);
					}
				});
				transaction.executeBackwardOn(propCtx, prevaylerService.prevalentSystem(), null, backwardsBranch);
			}
			
			// TODO: Consider:
			// What if finished has already been sent?
			// Send a "rejected after finished" message?
			// What finished has not been sent?
			// Send a "rejected before finished" message?
			if(finishBuilder != null) {
//				System.out.println("Sending finished after reject");
				finishBuilder.execute();
				hasSentFinished = true;
			}
		}
		
		@Override
		public TranscriberBranch<T> branch() {
			final SnapshottingTranscriber.Branch<T> branch = new Branch<T>(this, prevaylerService, propCtx);
			
			this.prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					if(!isClosed) {
						if(!rejected) {
							Branch.this.branches.add(branch);
						} else {
							branch.rejected = true;
						}
					}
				}
			});
			
			return branch;
		}
		
		private boolean isClosed;
		private DualCommandFactory<T> transactionFactory;
		
		@Override
		public void execute(final PropogationContext propCtx, final DualCommandFactory<T> transactionFactory) {
			final SnapshottingTranscriber.Branch<T> branch = new Branch<T>(this, prevaylerService, propCtx);
			branch.transactionFactory = transactionFactory;
			
			this.prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					if(!isClosed) {
						branches.add(branch);
					}
				}
			});
		}
		
		@Override
		public void close() {
			this.prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					if(!rejected) {
						if(!isClosed) {
							flushBranches();
							
							isClosed = true;
							
							if(branches.size() == 0) {
								// Branch is leaf
								doAbsorb();
//								System.out.println("implicit absorb was called on " + this);
								sendFinished();
							} else {
								checkAbsorbed();
							}
						} else {
							System.out.println("Attempted to close closed branch");
						}
					} else {
						System.out.println("Attempted to close rejected branch");
					}
				}
			});
		}
		
		private void flushBranches() {
			for(final Branch<T> branch: branches) {
				if(branch.transactionFactory != null) {
					ArrayList<DualCommand<T>> dualCommands = new ArrayList<DualCommand<T>>();
					branch.transactionFactory.createDualCommands(dualCommands);
					@SuppressWarnings("unchecked")
					DualCommand<T>[] dualCommandArray = dualCommands.toArray(new DualCommand[dualCommands.size()]);
					
					DualCommandSequence<T> transaction = new DualCommandSequence<T>(dualCommandArray);
					branch.transaction = transaction;
					
					for(DualCommand<T> t: dualCommands) {
						final Branch<T> b = (Branch<T>)branch.branch();
						// Initialize affectedModels to support registration of affected models on b
						b.affectedModels = affectedModels;
						t.executeForwardOn(branch.propCtx, branch.prevaylerService.prevalentSystem(), null, b);
						
						b.prevaylerService.transactionExecutor.execute(new Runnable() {
							@Override
							public void run() {
								b.flushBranches();
								
								b.isClosed = true;
								
								if(b.branches.size() > 0) {
									b.checkAbsorbed();
								} else {
									// If b is leaf
									b.doAbsorb();
									
									b.sendFinished();
//									System.out.println("b is leaf and was implicitly absorbed");
								}
							}
						});
					}
					
					branch.prevaylerService.transactionExecutor.execute(new Runnable() {
						@Override
						public void run() {
							branch.flushBranches();
							branch.isClosed = true;
							
							// Is there a scenario where branch is absorbed before this point?
							// Shouldn't be possible, since the branch isn't closed
							branch.checkAbsorbed();
						}
					});
				}
			}
		}
		
		@Override
		public TranscriberBranch<T> isolatedBranch() {
			return new IsolatedBranch<T>(this);
		}
		
		@Override
		public void onFinished(final Runnable runnable) {
			this.prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					doOnFinishedDirect(runnable);
				}
			});
		}
		
		private void doOnFinishedDirect(final Runnable runnable) {
			if(finishBuilder != null) {
				finishBuilder.addRunnable(runnable);
//				System.out.println("Registered onFinished: " + runnable);
			} else {
				if(parent != null)
					parent.doOnFinishedDirect(runnable);
			}
		}
		
		private RunBuilder finishBuilder;

		@Override
		public void setOnFinishedBuilder(final RunBuilder finishedBuilder) {
			this.prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					Branch.this.finishBuilder = finishedBuilder;
				}
			});
		}

		@Override
		public boolean isIsolated() {
			return false;
		}
		
		@Override
		public void doOnFinished(final Runnable runnable) {
			onFinished(runnable);
		}
		
		private HashSet<T> affectedModels = new HashSet<T>();

		@Override
		public void registerAffectedModel(final T model) {
			this.prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					affectedModels.add(model);
				}
			});
		}

		@Override
		public void addRegisteredAffectedModels(HashSet<T> allAffectedModels) {
			allAffectedModels.addAll(this.affectedModels);
			
			for(SnapshottingTranscriber.Branch<T> branch: absorbedBranches) {
				if(!branch.isIsolated())
					branch.addRegisteredAffectedModels(allAffectedModels);
				else {
					// This shouldn't be possible since isolated branches aren't added to branches
					System.out.println("addRegisteredAffectedModels wasn't called on isolated branch.");
				}
			}
		}
	}
	
	@Override
	public TranscriberBranch<T> createBranch() {
		TranscriberBranchBehavior<T> branchBehavior = new TranscriberBranchBehavior<T>() {
			@Override
			public void commit(PropogationContext propCtx, ContextualTransaction<T> ctxTransaction) {
//				registerTransaction(ctxTransaction);
				persistTransaction(propCtx, ctxTransaction);
			}
		};
		return new SnapshottingTranscriber.Branch<T>(branchBehavior, this, null);
	}
	
	@Override
	public TranscriberBranch<T> createBranch(TranscriberBranchBehavior<T> branchBehavior) {
		return new SnapshottingTranscriber.Branch<T>(branchBehavior, this, null);
	}
}
