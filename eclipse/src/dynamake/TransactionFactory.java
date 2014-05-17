package dynamake;

import java.util.Date;

import org.prevayler.Transaction;

public class TransactionFactory {
	private PrevaylerService<Model> prevaylerService;
	private TransactionFactory parent;
	private Locator locator;
	
	public TransactionFactory(PrevaylerService<Model> prevaylerService, Locator locator) {
		this.prevaylerService = prevaylerService;
		this.locator = locator;
	}
	
	public TransactionFactory getParent() {
		return parent;
	}
	
	public Locator getLocator() {
		return locator;
	}
	
	public Location getLocation() {
		if(parent != null)
			return new CompositeLocation(parent.getLocation(), locator.locate());
		return locator.locate();
	}
	
//	private static class LocationTransaction<T> implements Transaction<Model> {
	private static class LocationTransaction<T> implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
//		private Location location;
//		private Transaction<T> transaction;
//
//		public LocationTransaction(Location location, Transaction<T> transaction) {
//			this.location = location;
//			this.transaction = transaction;
//		}
		
		private Location location;
		private Command<T> transaction;

		public LocationTransaction(Location location, Command<T> transaction) {
			this.location = location;
			this.transaction = transaction;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime) {
			T obj = (T)location.getChild(prevalentSystem);
			transaction.executeOn(propCtx, obj, executionTime);
		}

		@Override
		public Command<Model> antagonist() {
			Command<T> transactionAntagonist = transaction.antagonist();
			return new LocationTransaction<T>(location, transactionAntagonist);
		}
	}
	
//	public <T extends Model> void execute(final Transaction<T> transaction) {
//		final Location location = getLocation();
//		prevaylerService.execute(new LocationTransaction<T>(location, transaction));
//	}
//	
//	public void executeOnRoot(final Transaction<Model> transaction) {
//		prevaylerService.execute(transaction);
//	}
	
	public <T extends Model> void execute(PropogationContext propCtx, final Command<T> transaction) {
		final Location location = getLocation();
		prevaylerService.execute(propCtx, new LocationTransaction<T>(location, transaction));
	}
	
	public void executeOnRoot(PropogationContext propCtx, final Command<Model> transaction) {
		prevaylerService.execute(propCtx, transaction);
	}
	
	public TransactionFactory extend(final Locator locator) {
		TransactionFactory extended = new TransactionFactory(prevaylerService, locator);
		
		extended.parent = this;
		
		return extended;
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
	public void undo(PropogationContext propCtx) {
		prevaylerService.undo(propCtx);
	}

	public void redo(PropogationContext propCtx) {
		prevaylerService.redo(propCtx);
	}
}
