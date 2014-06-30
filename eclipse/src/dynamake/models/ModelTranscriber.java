package dynamake.models;

import dynamake.transcription.Transcriber;
import dynamake.transcription.TranscriberBranch;

public class ModelTranscriber {
	private Transcriber<Model> prevaylerService;
	private ModelTranscriber parent;
	private ModelLocator locator;
	
	public ModelTranscriber(Transcriber<Model> prevaylerService, ModelLocator locator) {
		this.prevaylerService = prevaylerService;
		this.locator = locator;
	}
	
	public ModelTranscriber getParent() {
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
	
	// TODO: Consider: Could be replaced by a using a TranscribtionBranch without committing?
	public void executeTransient(Runnable runnable) {
		prevaylerService.executeTransient(runnable);
	}

	public ModelTranscriber extend(final ModelLocator locator) {
		ModelTranscriber extended = new ModelTranscriber(prevaylerService, locator);
		
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
	
	public static class CompositeLocation<T> implements Location {
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
	}

	public ModelLocator extendLocator(ModelLocator otherLocator) {
		return new CompositeModelLocator(getModelLocator(), otherLocator); 
	}

	public ModelLocation extendLocation(ModelLocation otherLocation) {
		return new CompositeModelLocation(getModelLocation(), otherLocation);
	}

	public TranscriberBranch<Model> createBranch() {
		return prevaylerService.createBranch();
	}
}
