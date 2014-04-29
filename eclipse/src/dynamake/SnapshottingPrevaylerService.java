package dynamake;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.prevayler.Prevayler;
import org.prevayler.Transaction;

public class SnapshottingPrevaylerService<T> implements PrevaylerService<T> {
	private Prevayler<T> prevayler;
	private int transactionEnlistingCount = 0;
	private int snapshotThreshold = 50;
	private ExecutorService snapshotTaker;
	private ExecutorService transactionExecutor;
	
	public SnapshottingPrevaylerService(Prevayler<T> prevayler) {
		this.prevayler = prevayler;
		snapshotTaker = Executors.newSingleThreadExecutor();
		transactionExecutor = Executors.newSingleThreadExecutor();
	}

	@Override
	public void execute(final Transaction<T> transaction) {
		transactionExecutor.execute(new Runnable() {
			@Override
			public void run() {
				// TODO: Consider grouping transaction pr. 1 millisecond, such that time-wise close transaction or logically performed as a single transaction.
				// Probably not to important (right now, at least), because most transactions are caused by end-user interaction
				prevayler.execute(transaction);
			}
		});
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
//			prevayler.takeSnapshot();
			prevayler.clock();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
