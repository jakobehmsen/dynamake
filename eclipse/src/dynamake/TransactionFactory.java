package dynamake;

import java.util.Date;

import org.prevayler.Transaction;

public class TransactionFactory {
	private PrevaylerService<Model> prevaylerService;
	private Locator locator;
	
	public TransactionFactory(PrevaylerService<Model> prevaylerService, Locator locator) {
		this.prevaylerService = prevaylerService;
		this.locator = locator;
	}
	
	private static class LocationTransaction<T> implements Transaction<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location location;
		private Transaction<T> transaction;

		public LocationTransaction(Location location, Transaction<T> transaction) {
			this.location = location;
			this.transaction = transaction;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void executeOn(Model prevalentSystem, Date executionTime) {
			T obj = (T)location.getChild(prevalentSystem);
			transaction.executeOn(obj, executionTime);
		}
	}
	
	public <T> void execute(final Transaction<T> transaction) {
		final Location location = locator.locate();
		prevaylerService.execute(new LocationTransaction<T>(location, transaction));
	}
	
	public TransactionFactory extend(final Locator locator) {
		return new TransactionFactory(prevaylerService, new Locator() {
			@Override
			public Location locate() {
				Location currentLocation = TransactionFactory.this.locator.locate();
				Location innerLocation = locator.locate();
				
				return new CompositeLocation(currentLocation, innerLocation);
			}
		});
	}
	
	private static class CompositeLocation implements Location {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Location head;
		private Location tail;
		
		public CompositeLocation(Location head, Location tail) {
			this.head = head;
			this.tail = tail;
		}

		@Override
		public Object getChild(Object holder) {
			return tail.getChild(head.getChild(holder));
		}

		@Override
		public void setChild(Object holder, Object child) {
			tail.setChild(head.getChild(holder), child);
		}
	}
}
