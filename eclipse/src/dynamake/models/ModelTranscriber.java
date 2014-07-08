package dynamake.models;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.transcription.FlushHandler;
import dynamake.transcription.Transcriber;
import dynamake.transcription.TranscriberConnection;
import dynamake.transcription.TranscriberOnFlush;

public class ModelTranscriber {
	private Transcriber<Model> transcriber;
	private ModelTranscriber parent;
	private ModelLocator locator;
	private JComponent componentToRepaint;
	
	public ModelTranscriber(Transcriber<Model> transcriber, ModelLocator locator) {
		this.transcriber = transcriber;
		this.locator = locator;
	}
	
	public void setComponentToRepaint(JComponent componentToRepaint) {
		this.componentToRepaint = componentToRepaint;
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
		transcriber.executeTransient(runnable);
	}

	public ModelTranscriber extend(final ModelLocator locator) {
		ModelTranscriber extended = new ModelTranscriber(transcriber, locator);
		
		extended.parent = this;
		extended.componentToRepaint = this.componentToRepaint;
		
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

	public TranscriberConnection<Model> createConnection() {
		return transcriber.createConnection(new FlushHandler<Model>() {
			@Override
			public void handleFlush(final List<TranscriberOnFlush<Model>> runnables) {
				if(componentToRepaint != null) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							for(TranscriberOnFlush<Model> r: runnables)
								r.run(null);
							
							componentToRepaint.repaint();
						}
					});
				}
			}
		});
	}
}
