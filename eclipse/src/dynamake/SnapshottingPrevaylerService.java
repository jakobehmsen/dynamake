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
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.xml.internal.messaging.saaj.packaging.mime.util.BEncoderStream;

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
			transaction.executeForwardOn(propCtx, prevalentSystem, null, new IsolatedBranch<T>());
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
	
	private static class IsolatedBranch<T> implements PrevaylerServiceBranch<T> {
		@Override
		public void absorb() { }

		@Override
		public void reject() { }

		@Override
		public PrevaylerServiceBranch<T> branch() {
			return new IsolatedBranch<T>();
		}

		@Override
		public void execute(PropogationContext propCtx, DualCommandFactory<T> transactionFactory) { }

		@Override
		public void onAbsorbed(PrevaylerServiceBranchContinuation<T> continuation) { }

		@Override
		public void doContinue() { }

		@Override
		public void close() { }

//		@Override
//		public void flush() { }

		@Override
		public void sendChangeToObservers(Model sender,
				ArrayList<Observer> observers, Object change,
				PropogationContext propCtx, int nextPropDistance,
				int nextChangeDistance) {
			for(Observer observer: observers) {
				if(!(observer instanceof Model)) {
					PropogationContext propCtxBranch = propCtx.branch();
					observer.changed(sender, change, propCtxBranch, nextPropDistance, nextChangeDistance, (PrevaylerServiceBranch<Model>)this);
				}
			}
		}
		
		@Override
		public PrevaylerServiceBranch<T> isolatedBranch() {
			return branch();
		}
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
					transaction.executeBackwardOn(propCtx, prevalentSystem, null, new IsolatedBranch<T>());
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
					transaction.executeForwardOn(propCtx, prevalentSystem, null, new IsolatedBranch<T>());
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
	
	private static class Branch<T> implements PrevaylerServiceBranch<T> {
		private Branch<T> parent;
		private SnapshottingPrevaylerService<T> prevaylerService;
		private DualCommand<T> transaction;
		private ArrayList<SnapshottingPrevaylerService.Branch<T>> branches = new ArrayList<SnapshottingPrevaylerService.Branch<T>>();
		private ArrayList<SnapshottingPrevaylerService.Branch<T>> absorbedBranches = new ArrayList<SnapshottingPrevaylerService.Branch<T>>();
		private PropogationContext propCtx;
//		private PrevaylerServiceBranchContinuation<T> continuation;
		private boolean rejected;
//		private boolean created;
		
		private Branch(Branch<T> parent, SnapshottingPrevaylerService<T> prevaylerService, final PropogationContext propCtx, PrevaylerServiceBranchContinuation<T> continuation) {
			this.parent = parent;
			this.prevaylerService = prevaylerService;
//			this.continuation = continuation;
			this.propCtx = propCtx;
		}
		
		private void commit(final PropogationContext propCtx) {
			prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					commit(propCtx, SnapshottingPrevaylerService.Branch.this);
				}
			});
		}
		
		private static <T> void commit(final PropogationContext propCtx, SnapshottingPrevaylerService.Branch<T> connection) {
			DualCommand<T> reduction = connection.reduce();
			
			if(reduction != null) {
				connection.prevaylerService.registerTransaction(reduction);
				connection.prevaylerService.persistTransaction(propCtx, reduction);
			}
//			System.out.println("Commit");
		}
		
		private DualCommand<T> reduce() {
			ArrayList<DualCommand<T>> transactionList = new ArrayList<DualCommand<T>>();
			
			if(transaction != null)
				transactionList.add(transaction);
			
			for(SnapshottingPrevaylerService.Branch<T> branch: absorbedBranches) {
				DualCommand<T> reduction = branch.reduce();
				
				if(reduction != null)
					transactionList.add(reduction);
			}
			
			if(transactionList.size() > 1) {
				DualCommand<T>[] transactionArray = (DualCommand<T>[])new DualCommand<?>[transactionList.size()];
				transactionList.toArray(transactionArray);
				
				return new DualCommandSequence<T>(transactionArray);
			} else if(transactionList.size() == 1)
				return transactionList.get(0);
			return null;
		}

		@Override
		public void absorb() {
			this.prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					if(parent != null)
						parent.absorbBranch(Branch.this);
					else {
						// Every effect has been absorbed
						commit(null);
					}
				}
			});
		}
		
		private void absorbBranch(final Branch<T> branch) {
			absorbedBranches.add(branch);

			if(isClosed) {
//				System.out.println("checkAbsorbed@absorbBranch");
				checkAbsorbed();
			}
//			System.out.println("Absorb branch performed");
		}
		
		private void checkAbsorbed() {
			if(absorbedBranches.size() == branches.size()) {
				if(parent != null) {
					parent.absorbBranch(Branch.this);
				} else {
					// Every effect has been absorbed
					commit(null);
				}
				
				for(PrevaylerServiceBranchContinuation<T> continuation: continuations)
					continuation.doContinue(propCtx, this);
			}
		}

		@Override
		public void reject() {
//			System.out.println("reject enqueue");
			this.prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
//					System.out.println("reject do");
					rejectSync();
				}
			});
		}
		
		private void rejectSync() {
			if(parent != null)
				parent.rejectSync();
			else
				rejectDownwards();
		}
		
		private void rejectDownwards() {
			rejected = true;
			
			// Reject in reverse order, i.e. start with the branches first
			for(Branch<T> branch: branches)
				branch.rejectDownwards();
			
			if(transaction != null)
				transaction.executeBackwardOn(propCtx, prevaylerService.prevalentSystem(), null, isolatedBranch());
		}
		
		private ArrayList<PrevaylerServiceBranchContinuation<T>> continuations = new ArrayList<PrevaylerServiceBranchContinuation<T>>();
		
		@Override
		public void onAbsorbed(final PrevaylerServiceBranchContinuation<T> continuation) {
			this.prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					if(!isClosed)
						continuations.add(continuation);
				}
			});
		}
		
		@Override
		public PrevaylerServiceBranch<T> branch() {
			final SnapshottingPrevaylerService.Branch<T> branch = new Branch<T>(this, prevaylerService, propCtx, null);
//			branch.isClosed = true;
			
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
			final SnapshottingPrevaylerService.Branch<T> branch = new Branch<T>(this, prevaylerService, propCtx, null);
			branch.transactionFactory = transactionFactory;
//			branch.isClosed = true;
			
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
						flushBranches();
						
						isClosed = true;
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
						t.executeForwardOn(branch.propCtx, branch.prevaylerService.prevalentSystem(), null, b);
						
						b.prevaylerService.transactionExecutor.execute(new Runnable() {
							@Override
							public void run() {
								b.flushBranches();
								
								b.isClosed = true;
								
								if(b.branches.size() > 0) {
									boolean hasExecutions = false;
									
									for(final Branch<T> branch: b.branches) {
										if(branch.transactionFactory != null) {
											hasExecutions = true;
											break;
										}
									}
									
									if(!hasExecutions)
										b.checkAbsorbed();
								}
							}
						});
						
//						b.close();
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
					
//					branch.close();
//					
//					branch.prevaylerService.transactionExecutor.execute(new Runnable() {
//						@Override
//						public void run() {
//							// Is there a scenario where branch is absorbed before this point?
//							// Shouldn't be possible, since the branch isn't closed?
//							branch.checkAbsorbed();
//						}
//					});
				}
			}
		}

//		@Override
//		public void flush() {
//			/*
//			Is flush really necessary? It could be replaced be making a branch and then closing that branch. 
//			*/
//			this.prevaylerService.transactionExecutor.execute(new Runnable() {
//				@Override
//				public void run() {
//					flushBranches();
//				}
//			});
//		}
		
		@Override
		public void doContinue() {
//			continuation.doContinue(propCtx, this);
		}
		
		@Override
		public void sendChangeToObservers(Model sender,
				ArrayList<Observer> observers, Object change,
				PropogationContext propCtx, int nextPropDistance,
				int nextChangeDistance) {
			int branchCount = 0;
			for(Observer observer: observers) {
				if(observer instanceof Model)
					branchCount++;
			}
			
			for(int i = 0; i < observers.size(); i++) {
				Observer observer = observers.get(i);
				PropogationContext propCtxBranch = propCtx.branch();
				PrevaylerServiceBranch<Model> innerBranch = (PrevaylerServiceBranch<Model>)this;
//				PrevaylerServiceBranch<Model> innerBranch;
//				if(observer instanceof Model)
//					innerBranch = (PrevaylerServiceBranch<Model>)this.branch();
//				else
//					innerBranch = (PrevaylerServiceBranch<Model>)this;
				observer.changed(sender, change, propCtxBranch, nextPropDistance, nextChangeDistance, innerBranch);
//				if(observer instanceof Model)
//					innerBranch.close();
			}
			
			if(branchCount == 0) {
				// Probably, each route of logic should be responsible for absorbtion
				this.absorb();
			}
		}
		
		@Override
		public PrevaylerServiceBranch<T> isolatedBranch() {
			return new IsolatedBranch<T>();
		}
		
//		private Hashtable<String, Object> variables = new Hashtable<String, Object>();
//
//		@Override
//		public void setVariable(String variableName, Object value) {
//			variables.put(variableName, value);
//		}
//		
//		@Override
//		public Object getVariable(String variableName) {
//			Object value = variables.get(variableName);
//			if(value != null)
//				return value;
//			if(parent != null)
//				return parent.getVariable(variableName);
//			return null;
//		}
	}
	
	@Override
	public PrevaylerServiceBranch<T> createBranch() {
		SnapshottingPrevaylerService.Branch<T> rootBranch = new SnapshottingPrevaylerService.Branch<T>(null, this, null, null);
//		rootBranch.created = true;
		return rootBranch;
	}
}
