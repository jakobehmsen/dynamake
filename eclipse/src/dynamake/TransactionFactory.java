package dynamake;

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
		if(parent != null)
			return new CompositeModelLocator(parent.getModelLocator(), locator);
		return locator;
	}
	
	public ModelLocation getModelLocation() {
		if(parent != null)
			return new CompositeModelLocation(parent.getModelLocation(), (ModelLocation)locator.locate());
		return (ModelLocation)locator.locate();
	}
	
	// TODO: Consider: Could be replaced by a using a PrevaylerServiceConnection without committing?
	public void executeTransient(Runnable runnable) {
		prevaylerService.executeTransient(runnable);
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
	
	static class CompositeLocation<T> implements Location {
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
//			tail.setChild(head.getChild(holder, branch), child);
		}
	}
	
	public void undo(PropogationContext propCtx, Location location) {
		prevaylerService.undo(propCtx, location);
	}

	public void redo(PropogationContext propCtx, Location location) {
		prevaylerService.redo(propCtx, location);
	}

	public ModelLocator extendLocator(ModelLocator otherLocator) {
		return new CompositeModelLocator(getModelLocator(), otherLocator); 
	}

	public ModelLocation extendLocation(ModelLocation otherLocation) {
		return new CompositeModelLocation(getModelLocation(), otherLocation);
	}

	public PrevaylerServiceBranch<Model> createBranch() {
		return prevaylerService.createBranch();
	}
}
