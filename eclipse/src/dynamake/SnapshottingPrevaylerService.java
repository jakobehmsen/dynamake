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
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.text.DateFormatter;

import org.prevayler.Prevayler;
import org.prevayler.PrevaylerFactory;
import org.prevayler.Transaction;

public class SnapshottingPrevaylerService<T> implements PrevaylerService<T> {
//	private Prevayler<T> prevayler;
	private Func0<T> prevalentSystemFunc;
	private T prevalentSystem;
	private int transactionEnlistingCount = 0;
	private int snapshotThreshold = 100;
//	private ExecutorService snapshotTaker;
	private ExecutorService transactionExecutor;
	
	private String prevalanceDirectory;
	private static String journalFileName = "log";
	private static String journalFile = journalFileName + ".jnl";
	private static String snapshotFile = "snap.shot";
	private ExecutorService journalLogger;
	
//	public SnapshottingPrevaylerService(Prevayler<T> prevayler) {
//		this.prevayler = prevayler;
//		snapshotTaker = Executors.newSingleThreadExecutor();
//		transactionExecutor = Executors.newSingleThreadExecutor();
//	}
	
	public SnapshottingPrevaylerService(Func0<T> prevalentSystemFunc) throws Exception {
//		this.prevayler = PrevaylerFactory.createPrevayler(newPrevalentSystem);
		this.prevalentSystemFunc = prevalentSystemFunc; 
//		this.prevalentSystem = newPrevalentSystem;
//		snapshotTaker = Executors.newSingleThreadExecutor();
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
			replay(prevalentSystem, prevalanceDirectory + "/" + journalFile);
	}
	
	private static <T> T loadAndReplay(Func0<T> prevalantSystemFunc, String journalPath, String snapshotPath) throws ClassNotFoundException, IOException {
		T prevalantSystem;
		
		Path snapshotFilePath = Paths.get(snapshotPath);
		
		if(java.nio.file.Files.exists(snapshotFilePath))
			prevalantSystem = loadSnapshot(snapshotPath);
		else
			prevalantSystem = prevalantSystemFunc.call();
		
		replay(prevalantSystem, journalPath);
		
		return prevalantSystem;
	}
	
	private static <T> void replay(T prevalentSystem, String journalPath) throws ClassNotFoundException, IOException {
		FileInputStream fileOutput = new FileInputStream(journalPath);
		BufferedInputStream bufferedOutput = new BufferedInputStream(fileOutput);
		
		while(bufferedOutput.available() != 0) {
			// Should be read in chunks
			ObjectInputStream objectOutput = new ObjectInputStream(bufferedOutput);
			Command<T> transaction = (Command<T>)objectOutput.readObject();
			transaction.executeOn(prevalentSystem, null);
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
	
	private static <T> void saveSnapshot(Func0<T> prevalantSystemFunc, String journalPath, String snapshotPath) throws ClassNotFoundException, IOException, ParseException {
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
		T prevalantSystem = loadAndReplay(prevalantSystemFunc, closedJournalFilePath.toString(), closedSnapshotFilePath.toString());
		
		
		// Save modified snapshot
		FileOutputStream fileOutput = new FileOutputStream(snapshotPath, true);
		BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput);
		ObjectOutputStream objectOutput = new ObjectOutputStream(bufferedOutput);
		
		objectOutput.writeObject(prevalantSystem);
		
		objectOutput.close();
	}
	
	private void saveSnapshot() throws ClassNotFoundException, IOException, ParseException {
		saveSnapshot(prevalentSystemFunc, prevalanceDirectory + "/" + journalFile, prevalanceDirectory + "/" + snapshotFile);
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

	@Override
//	public void execute(final Transaction<T> transaction) {
	public void execute(final Command<T> transaction) {
//		transactionExecutor.execute(new Runnable() {
//			@Override
//			public void run() {
//				// TODO: Consider grouping transaction pr. 1 millisecond, such that time-wise close transaction or logically performed as a single transaction.
//				// Probably not to important (right now, at least), because most transactions are caused by end-user interaction
//				
//				// Seems there is a significant inconsistent delay for this call which may be caused by IO latencies or scheduling or the like
//				prevayler.execute(transaction);
////				transaction.executeOn(prevayler.prevalentSystem(), null);
//			}
//		});
		
		transactionExecutor.execute(new Runnable() {
			@Override
			public void run() {
				transaction.executeOn(prevalentSystem(), null);
			}
		});
		
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
			}
		});
		
		transactionEnlistingCount++;
		if(transactionEnlistingCount >= snapshotThreshold) {
			System.out.println("Enlisted snapshot on thread " + Thread.currentThread().getId());
			journalLogger.execute(new Runnable() {
				@Override
				public void run() {
					try {
//						prevayler.takeSnapshot();
						saveSnapshot();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
//			snapshotTaker.execute(new Runnable() {
//				@Override
//				public void run() {
//					try {
////						prevayler.takeSnapshot();
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
//			});
			transactionEnlistingCount = 0;
		}
	}

	@Override
	public void close() {
		try {
//			snapshotTaker.shutdown();
			transactionExecutor.shutdown();
			journalLogger.shutdown();
//			prevayler.takeSnapshot();
//			prevayler.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public T prevalentSystem() {
		// TODO Auto-generated method stub
//		return prevayler.prevalentSystem();
		return prevalentSystem;
	}
}
