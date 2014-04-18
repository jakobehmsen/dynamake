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
//		System.out.println("Enlisted transaction on thread " + Thread.currentThread().getId());
		transactionExecutor.execute(new Runnable() {
			@Override
			public void run() {
				// TODO: Consider grouping transaction pr. 1 millisecond, such that time-wise close transaction or logically performed as a single transaction.
				prevayler.execute(transaction);
//				System.out.println("Executed transaction on thread " + Thread.currentThread().getId());
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
					System.out.println("Took snapshot on thread " + Thread.currentThread().getId());
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
