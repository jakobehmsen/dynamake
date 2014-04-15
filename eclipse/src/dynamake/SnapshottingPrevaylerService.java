package dynamake;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.prevayler.Prevayler;
import org.prevayler.Transaction;

public class SnapshottingPrevaylerService<T> implements PrevaylerService<T> {
	private Prevayler<T> prevayler;
	private int transactionCount = 0;
	private int snapshotThreshold = 50;
	private ExecutorService snapshotTaker;
	
	public SnapshottingPrevaylerService(Prevayler<T> prevayler) {
		this.prevayler = prevayler;
		snapshotTaker = Executors.newSingleThreadExecutor();
	}

	@Override
	public void execute(Transaction<T> transaction) {
		transactionCount++;
		prevayler.execute(transaction);
		if(transactionCount >= snapshotThreshold) {
			System.out.println("Enlisted snapshot on thread " + Thread.currentThread().getId());
			snapshotTaker.execute(new Runnable() {
				@Override
				public void run() {
					try {
						prevayler.takeSnapshot();
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println("Took snapshot on thread " + Thread.currentThread().getId());
				}
			});
			transactionCount = 0;
		}
	}

	@Override
	public void close() {
		try {
			snapshotTaker.shutdown();
			prevayler.takeSnapshot();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
