package dynamake;

import java.util.Date;
import java.util.List;

import org.prevayler.Transaction;

public class TransactionFactory {
	private PrevaylerService<Model> prevaylerService;
	private TransactionFactory parent;
	private ModelLocator locator;
	
	public TransactionFactory(PrevaylerService<Model> prevaylerService, ModelLocator locator) {
		this.prevaylerService = prevaylerService;
		this.locator = locator;
	}
	
	public TransactionFactory getParent() {
		return parent;
	}
	
	public ModelLocator getModelLocator() {
//		return locator;
		if(parent != null)
			return new CompositeModelLocator(parent.getModelLocator(), locator);
		return locator;
	}
	
	public ModelLocation getModelLocation() {
		if(parent != null)
			return new CompositeModelLocation(parent.getModelLocation(), (ModelLocation)locator.locate());
		return (ModelLocation)locator.locate();
	}
	
//	private static class LocationTransaction<T> implements Transaction<Model> {
	private static class LocationTransaction<T> implements DualCommand<Model> {
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
		private DualCommand<T> transaction;

		public LocationTransaction(Location location, DualCommand<T> transaction) {
			this.location = location;
			this.transaction = transaction;
		}

//		@SuppressWarnings("unchecked")
//		@Override
//		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime) {
//			T obj = (T)location.getChild(prevalentSystem);
//			transaction.executeOn(propCtx, obj, executionTime);
//		}
//
//		@Override
//		public Command<Model> antagonist() {
//			Command<T> transactionAntagonist = transaction.antagonist();
//			return new LocationTransaction<T>(location, transactionAntagonist);
//		}
		
		@Override
		public void executeForwardOn(PropogationContext propCtx,
				Model prevalentSystem, Date executionTime) {
			T obj = (T)location.getChild(prevalentSystem);
			transaction.executeForwardOn(propCtx, obj, executionTime);
		}
		
		@Override
		public void executeBackwardOn(PropogationContext propCtx,
				Model prevalentSystem, Date executionTime) {
			T obj = (T)location.getChild(prevalentSystem);
			transaction.executeBackwardOn(propCtx, obj, executionTime);
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
	
	public void executeTransient(Runnable runnable) {
		prevaylerService.executeTransient(runnable);
	}
	
//	public <T extends Model> void execute(PropogationContext propCtx, final Command<T> transaction) {
//		execute(propCtx, new DualCommandFactory<T>() {
//			@Override
//			public DualCommand<T> createDualCommand() {
//				return new DualCommandPair<T>(transaction, null);
//			}
//		});
//	}
	
//	public <T extends Model> void execute(PropogationContext propCtx, final DualCommand<T> transaction) {
//		execute(propCtx, new DualCommandFactory<T>() {
//			@Override
//			public DualCommand<T> createDualCommand() {
//				return transaction;
//			}
//		});
//	}
	
//	public <T extends Model> void execute(PropogationContext propCtx, final DualCommandFactory<T> transactionFactory) {
//		final Location location = getModelLocation();
//		prevaylerService.execute(propCtx, new DualCommandFactory<Model>() {
//			@Override
//			public DualCommand<Model> createDualCommand() {
//				DualCommand<T> transaction = transactionFactory.createDualCommand();
//				return new LocationTransaction<T>(location, transaction);
//			}
//		});
//	}
	
	public <T extends Model> void executeOnRoot(PropogationContext propCtx, final Command<T> transaction) {
		executeOnRoot(propCtx, new DualCommandFactory<T>() {
			public DualCommand<T> createDualCommand() {
				return new DualCommandPair<T>(transaction, null);
			}
			
			@Override
			public void createDualCommands(
					List<DualCommand<T>> dualCommands) {
				dualCommands.add(createDualCommand());
			}
		});
	}
	
	public <T extends Model> void executeOnRoot(PropogationContext propCtx, final DualCommand<T> transaction) {
		executeOnRoot(propCtx, new DualCommandFactory<T>() {
			@Override
			public void createDualCommands(
					List<DualCommand<T>> dualCommands) {
				dualCommands.add(transaction);
			}
		});
	}
	
	public <T extends Model> void executeOnRoot(PropogationContext propCtx, final DualCommandFactory<T> transactionFactory) {
		prevaylerService.execute(propCtx, (DualCommandFactory<Model>) transactionFactory);
	}
	
	public TransactionFactory extend(final ModelLocator locator) {
		TransactionFactory extended = new TransactionFactory(prevaylerService, locator);
		
		extended.parent = this;
		
		return extended;
	}
	
	private static class CompositeModelLocator implements ModelLocator {
		private ModelLocator head;
		private ModelLocator tail;
		
		public CompositeModelLocator(ModelLocator head, ModelLocator tail) {
			this.head = head;
			this.tail = tail;
		}

		@Override
		public ModelLocation locate() {
			return new CompositeModelLocation(head.locate(), tail.locate());
		}
	}
	
	private static class CompositeModelLocation implements ModelLocation {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private ModelLocation head;
		private ModelLocation tail;
		
		public CompositeModelLocation(ModelLocation head, ModelLocation tail) {
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

		@Override
		public Location getModelComponentLocation() {
			return new CompositeLocation(head.getModelComponentLocation(), tail.getModelComponentLocation());
		}
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

	public void beginTransaction() {
		prevaylerService.beginTransaction();
	}
	
	public void commitTransaction(PropogationContext propCtx) {
		prevaylerService.commitTransaction(propCtx);
	}

	public void rollbackTransaction(PropogationContext propCtx) {
		prevaylerService.rollbackTransaction(propCtx);
	}
}
