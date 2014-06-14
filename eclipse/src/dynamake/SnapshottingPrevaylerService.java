package dynamake;

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
import java.util.Date;
import java.util.Stack;
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
		
		if(snapshotExisted) {
			Snapshot<T> snapshot = loadSnapshot(prevalanceDirectory + "/" + snapshotFile);
			prevalentSystem = snapshot.prevalentSystem;
			transactionUndoStack = snapshot.transactionUndoStack;
			transactionRedoStack = snapshot.transactionRedoStack;
		} else
			prevalentSystem = prevalentSystemFunc.call();
		
		if(journalExisted)
			replay(new PropogationContext(), prevalentSystem, prevalanceDirectory + "/" + journalFile, transactionUndoStack, transactionRedoStack);
	}
	
	private static <T> Snapshot<T> loadAndReplay(PropogationContext propCtx, Func0<T> prevalantSystemFunc, String journalPath, String snapshotPath) throws ClassNotFoundException, IOException {
		Snapshot<T> snapshot;
		
		Path snapshotFilePath = Paths.get(snapshotPath);
		
		if(java.nio.file.Files.exists(snapshotFilePath))
			snapshot = loadSnapshot(snapshotPath);
		else {
			T prevalantSystem = prevalantSystemFunc.call();
			
			snapshot = new Snapshot<T>(prevalantSystem, new Stack<DualCommand<T>>(), new Stack<DualCommand<T>>());
		}
		
		replay(propCtx, snapshot.prevalentSystem, journalPath, snapshot.transactionUndoStack, snapshot.transactionRedoStack);
		
		return snapshot;
	}
	
	private static <T> void replay(PropogationContext propCtx, T prevalentSystem, String journalPath, Stack<DualCommand<T>> transactionUndoStack, Stack<DualCommand<T>> transactionRedoStack) throws ClassNotFoundException, IOException {
		FileInputStream fileOutput = new FileInputStream(journalPath);
		BufferedInputStream bufferedOutput = new BufferedInputStream(fileOutput);
		
		while(bufferedOutput.available() != 0) {
			// Should be read in chunks
			@SuppressWarnings("resource")
			ObjectInputStream objectOutput = new ObjectInputStream(bufferedOutput);
			@SuppressWarnings("unchecked")
			DualCommand<T> transaction = (DualCommand<T>)objectOutput.readObject();
			
			// Probably, there should an extra layer of interface here, where there are three implementations:
			// One for undo
			// One for redo
			// One for do
			// Such command should be called a meta-command?
			if(transaction instanceof UndoTransaction) {
				UndoTransaction<T> undoTransaction = (UndoTransaction<T>)transaction;
				
				int i;
				for(i = transactionUndoStack.size() - 1; i >= 0; i--) {
					if(transactionUndoStack.get(i).occurredWithin(undoTransaction.location))
						break;
				}
				
				DualCommand<T> transactionToUndo = transactionUndoStack.get(i);
				transactionUndoStack.remove(i);
				transactionToUndo.executeBackwardOn(propCtx, prevalentSystem, null, new IsolatedBranch<T>(null));
				transactionRedoStack.push(transactionToUndo);
				
//				DualCommand<T> transactionToUndo = transactionUndoStack.pop();
//				transactionToUndo.executeBackwardOn(propCtx, prevalentSystem, null, new IsolatedBranch<T>());
//				transactionRedoStack.push(transactionToUndo);
			} else if(transaction instanceof RedoTransaction) {
				RedoTransaction<T> redoTransaction = (RedoTransaction<T>)transaction;
				
				int i;
				for(i = transactionRedoStack.size() - 1; i >= 0; i--) {
					if(transactionRedoStack.get(i).occurredWithin(redoTransaction.location))
						break;
				}
				
				DualCommand<T> transactionToRedo = transactionRedoStack.get(i);
				transactionRedoStack.remove(i);
				transactionToRedo.executeForwardOn(propCtx, prevalentSystem, null, new IsolatedBranch<T>(null));
				transactionUndoStack.add(transactionToRedo);
				
//				DualCommand<T> transactionToRedo = transactionRedoStack.pop();
//				transactionToRedo.executeForwardOn(propCtx, prevalentSystem, null, new IsolatedBranch<T>());
//				transactionUndoStack.push(transactionToRedo);
			} else {
				transaction.executeForwardOn(propCtx, prevalentSystem, null, new IsolatedBranch<T>(null));
				transactionUndoStack.push(transaction);
				transactionRedoStack.clear();
			}
		}
		
		bufferedOutput.close();
	}
	
	private static class Snapshot<T> implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final T prevalentSystem;
		public final Stack<DualCommand<T>> transactionUndoStack;
		public final Stack<DualCommand<T>> transactionRedoStack;
		
		public Snapshot(T prevalentSystem, Stack<DualCommand<T>> transactionUndoStack, Stack<DualCommand<T>> transactionRedoStack) {
			this.prevalentSystem = prevalentSystem;
			this.transactionUndoStack = transactionUndoStack;
			this.transactionRedoStack = transactionRedoStack;
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
	
	private static class IsolatedBranch<T> implements PrevaylerServiceBranch<T> {
		private PrevaylerServiceBranch<T> parent;
		private RunBuilder finishedBuilder;
		
		public IsolatedBranch() { }
		
		public IsolatedBranch(PrevaylerServiceBranch<T> parent) {
			this.parent = parent;
		}

		@Override
		public void absorb() { }

		@Override
		public void reject() { }

		@Override
		public PrevaylerServiceBranch<T> branch() {
			return new IsolatedBranch<T>(this);
		}

		@Override
		public void execute(PropogationContext propCtx, DualCommandFactory<T> transactionFactory) { }

		@Override
		public void close() { }

		@SuppressWarnings("unchecked")
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
		
		@Override
		public void onFinished(Runnable runnable) {
			if(finishedBuilder != null)
				finishedBuilder.addRunnable(runnable);
			else {
				if(parent != null)
					parent.onFinished(runnable);
			}
		}
		
		@Override
		public void setOnFinishedBuilder(RunBuilder finishedBuilder) { 
			this.finishedBuilder = finishedBuilder;
		}
	}
	
	private Stack<DualCommand<T>> transactionUndoStack = new Stack<DualCommand<T>>();
	private Stack<DualCommand<T>> transactionRedoStack = new Stack<DualCommand<T>>();
	
	private static class UndoTransaction<T> implements DualCommand<T> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final Location location;
		
		public UndoTransaction(Location location) {
			this.location = location;
		}
		
		@Override
		public void executeBackwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, PrevaylerServiceBranch<T> branch) { }
		
		@Override
		public void executeForwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, PrevaylerServiceBranch<T> branch) { }
		
		@Override
		public boolean occurredWithin(Location location) {
			return false;
		}
	}
	
	private static class RedoTransaction<T> implements DualCommand<T> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public final Location location;
		
		public RedoTransaction(Location location) {
			this.location = location;
		}
		
		@Override
		public void executeBackwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, PrevaylerServiceBranch<T> branch) { }
		
		@Override
		public void executeForwardOn(PropogationContext propCtx, T prevalentSystem, Date executionTime, PrevaylerServiceBranch<T> branch) { }
		
		@Override
		public boolean occurredWithin(Location location) {
			return false;
		}
	}
	
	@Override
	public void undo(final PropogationContext propCtx, final Location location, final RunBuilder finishedBuilder) {
		transactionExecutor.execute(new Runnable() {
			@Override
			public void run() {
				if(transactionUndoStack.size() > 0) {
					int i;
					for(i = transactionUndoStack.size() - 1; i >= 0; i--) {
						if(transactionUndoStack.get(i).occurredWithin(location))
							break;
					}
					
					if(i >= 0) {
						DualCommand<T> transaction = transactionUndoStack.get(i);
						transactionUndoStack.remove(i);
						IsolatedBranch<T> isolatedBranch = new IsolatedBranch<T>();
						isolatedBranch.setOnFinishedBuilder(finishedBuilder);
						transaction.executeBackwardOn(propCtx, prevalentSystem, null, isolatedBranch);
						transactionRedoStack.push(transaction);
						
						persistTransaction(propCtx, new UndoTransaction<T>(location));
						
						finishedBuilder.execute();
					}
				}
			}
		});
	}

	@Override
	public void redo(final PropogationContext propCtx, final Location location, final RunBuilder finishedBuilder) {
		transactionExecutor.execute(new Runnable() {
			@Override
			public void run() {
				if(transactionRedoStack.size() > 0) {
					int i;
					for(i = transactionRedoStack.size() - 1; i >= 0; i--) {
						if(transactionRedoStack.get(i).occurredWithin(location))
							break;
					}
					
					if(i >= 0) {
						DualCommand<T> offsetTransaction = transactionRedoStack.get(i);
						transactionRedoStack.remove(i);
						IsolatedBranch<T> isolatedBranch = new IsolatedBranch<T>();
						isolatedBranch.setOnFinishedBuilder(finishedBuilder);
						offsetTransaction.executeForwardOn(propCtx, prevalentSystem, null, isolatedBranch);
						transactionUndoStack.add(offsetTransaction);
						persistTransaction(propCtx, new RedoTransaction<T>(location));
						
						finishedBuilder.execute();
					}
				}
			}
		});
	}

	public void executeTransient(Runnable runnable) {
		transactionExecutor.execute(runnable);
	}
	
	private void registerTransaction(final DualCommand<T> transaction) {
		transactionUndoStack.push(transaction);
		transactionRedoStack.clear();
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
		private boolean rejected;
		
		private Branch(Branch<T> parent, SnapshottingPrevaylerService<T> prevaylerService, final PropogationContext propCtx) {
			this.parent = parent;
			this.prevaylerService = prevaylerService;
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
				System.out.println("Committed: " + reduction);
				connection.prevaylerService.registerTransaction(reduction);
				connection.prevaylerService.persistTransaction(propCtx, reduction);
			}
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
				@SuppressWarnings("unchecked")
				DualCommand<T>[] transactionArray = (DualCommand<T>[])new DualCommand<?>[transactionList.size()];
				transactionList.toArray(transactionArray);
				
				return new DualCommandSequence<T>(transactionArray);
			} else if(transactionList.size() == 1)
				return transactionList.get(0);
			return null;
		}
		
		private boolean isAbsorbed;

		@Override
		public void absorb() {
			this.prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					if(!isClosed) {
						doAbsorb();
						
						sendFinished();
					} else {
						System.out.println("Attempted to explicitly absorb closed branch.");
					}
				}
			});
		}
		
		private void sendFinished() {
			if(finishBuilder != null)
				finishBuilder.execute();
		}
		
		private void doAbsorb() {
			if(parent != null) {
				parent.absorbBranch(Branch.this);
				isAbsorbed = true;
			} else {
				// Every effect has been absorbed
				commit(null);
			}
		}
		
		private void absorbBranch(final Branch<T> branch) {
			absorbedBranches.add(branch);

			if(isClosed)
				checkAbsorbed();
		}
		
		private void checkAbsorbed() {
			if(absorbedBranches.size() == branches.size()) {
				if(parent != null) {
					parent.absorbBranch(Branch.this);
				} else {
					// Every effect has been absorbed
					commit(null);
				}
				
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
			
			sendFinished();
		}
		
		@Override
		public PrevaylerServiceBranch<T> branch() {
			final SnapshottingPrevaylerService.Branch<T> branch = new Branch<T>(this, prevaylerService, propCtx);
			
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
			final SnapshottingPrevaylerService.Branch<T> branch = new Branch<T>(this, prevaylerService, propCtx);
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
						flushBranches();
						
						isClosed = true;
						
						if(branches.size() == 0)
							sendFinished();
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
									// Branch may have been absorbed at this point
									if(!b.isAbsorbed) {
										b.checkAbsorbed();
									}
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
				@SuppressWarnings("unchecked")
				PrevaylerServiceBranch<Model> innerBranch = (PrevaylerServiceBranch<Model>)this;
				observer.changed(sender, change, propCtxBranch, nextPropDistance, nextChangeDistance, innerBranch);
			}
			
			if(branchCount == 0)
				this.absorb();
		}
		
		@Override
		public PrevaylerServiceBranch<T> isolatedBranch() {
			return new IsolatedBranch<T>(this);
		}
		
		@Override
		public void onFinished(final Runnable runnable) {
			this.prevaylerService.transactionExecutor.execute(new Runnable() {
				@Override
				public void run() {
					doOnFinished(runnable);
				}
			});
		}
		
		private void doOnFinished(final Runnable runnable) {
			if(finishBuilder != null)
				finishBuilder.addRunnable(runnable);
			else {
				if(parent != null)
					parent.doOnFinished(runnable);
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
	}
	
	@Override
	public PrevaylerServiceBranch<T> createBranch() {
		return new SnapshottingPrevaylerService.Branch<T>(null, this, null);
	}
}
