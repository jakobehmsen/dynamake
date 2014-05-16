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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.prevayler.Prevayler;
import org.prevayler.PrevaylerFactory;
import org.prevayler.Transaction;

public class SnapshottingPrevaylerService<T> implements PrevaylerService<T> {
	private Prevayler<T> prevayler;
	private T prevalentSystem;
	private int transactionEnlistingCount = 0;
	private int snapshotThreshold = 50;
	private ExecutorService snapshotTaker;
	private ExecutorService transactionExecutor;
	
	private String journalDirectory;
	private ExecutorService journalLogger;
	
//	public SnapshottingPrevaylerService(Prevayler<T> prevayler) {
//		this.prevayler = prevayler;
//		snapshotTaker = Executors.newSingleThreadExecutor();
//		transactionExecutor = Executors.newSingleThreadExecutor();
//	}
	
	public SnapshottingPrevaylerService(T newPrevalentSystem) throws Exception {
		this.prevayler = PrevaylerFactory.createPrevayler(newPrevalentSystem);
		this.prevalentSystem = newPrevalentSystem;
		snapshotTaker = Executors.newSingleThreadExecutor();
		transactionExecutor = Executors.newSingleThreadExecutor();
		
		journalLogger = Executors.newSingleThreadExecutor();
		journalDirectory = "jnl";
		
		// One current journal with a predictive name and multiple old journals with predictive naming
		// One latest snapshot with a predictive name and multiple old snapshot with predictive naming
		// Each snapshot has a reference to its relative journal
		// When a snapshot is enqueue, then the current journal is closed and referenced to from the snapshot made followingly
		// such that it is known which journal to start from after the snapshot has been read.
		
//		boolean journalExisted = true;
//		
//		Path journalDirectoryPath = Paths.get(journalDirectory);
//		if(!java.nio.file.Files.exists(journalDirectoryPath)) {
//			java.nio.file.Files.createDirectory(journalDirectoryPath);
//			journalExisted = false;
//		}
//		
//		Path journalFilePath = Paths.get(journalDirectory + "/" + "log.jnl");
//		if(!java.nio.file.Files.exists(journalFilePath)) {
//			java.nio.file.Files.createFile(journalFilePath);
//			journalExisted = false;
//		}
//		
//		if(journalExisted) {
//			FileInputStream fileOutput = new FileInputStream(journalDirectory + "/" + "log.jnl");
//			BufferedInputStream bufferedOutput = new BufferedInputStream(fileOutput);
//			
//			while(bufferedOutput.available() != 0) {
//				ObjectInputStream objectOutput = new ObjectInputStream(bufferedOutput);
//				Transaction<T> transaction = (Transaction<T>)objectOutput.readObject();
//				transaction.executeOn(prevalentSystem, null);
//			}
//			
//			bufferedOutput.close();
//		}
	}

	@Override
	public void execute(final Transaction<T> transaction) {
		transactionExecutor.execute(new Runnable() {
			@Override
			public void run() {
				// TODO: Consider grouping transaction pr. 1 millisecond, such that time-wise close transaction or logically performed as a single transaction.
				// Probably not to important (right now, at least), because most transactions are caused by end-user interaction
				
				// Seems there is a significant inconsistent delay for this call which may be caused by IO latencies or scheduling or the like
				prevayler.execute(transaction);
//				transaction.executeOn(prevayler.prevalentSystem(), null);
			}
		});
		
//		transactionExecutor.execute(new Runnable() {
//			@Override
//			public void run() {
//				transaction.executeOn(prevalentSystem(), null);
//			}
//		});
//		
//		journalLogger.execute(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					FileOutputStream fileOutput = new FileOutputStream(journalDirectory + "/" + "log.jnl", true);
//					BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput);
//					ObjectOutputStream objectOutput = new ObjectOutputStream(bufferedOutput);
//					
//					objectOutput.writeObject(transaction);
//					
//					objectOutput.close();
//				} catch (FileNotFoundException e) {
//					e.printStackTrace();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		});
		
		transactionEnlistingCount++;
		if(transactionEnlistingCount >= snapshotThreshold) {
			System.out.println("Enlisted snapshot on thread " + Thread.currentThread().getId());
			snapshotTaker.execute(new Runnable() {
				@Override
				public void run() {
					try {
						prevayler.takeSnapshot();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			transactionEnlistingCount = 0;
		}
	}

	@Override
	public void close() {
		try {
			snapshotTaker.shutdown();
			transactionExecutor.shutdown();
			journalLogger.shutdown();
//			prevayler.takeSnapshot();
			prevayler.close();
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
