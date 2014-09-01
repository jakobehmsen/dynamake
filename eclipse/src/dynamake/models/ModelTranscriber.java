package dynamake.models;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.transcription.TriggerHandler;
import dynamake.transcription.Transcriber;
import dynamake.transcription.Connection;

public class ModelTranscriber {
	private Transcriber<Model> transcriber;
	private ModelTranscriber parent;
	private Locator<Model> locator;
	private JComponent componentToRepaint;
	
	public ModelTranscriber(Transcriber<Model> transcriber, Locator<Model> locator) {
		this.transcriber = transcriber;
		this.locator = locator;
	}
	
	public void setComponentToRepaint(JComponent componentToRepaint) {
		this.componentToRepaint = componentToRepaint;
	}
	
	public ModelTranscriber getParent() {
		return parent;
	}
	
	public Locator<Model> getModelLocator() {
		if(parent != null)
			return new CompositeModelLocator(parent.getModelLocator(), locator);
		return locator;
	}
	
	public Location<Model> getModelLocation() {
		if(parent != null)
			return new CompositeLocation<Model>(parent.getModelLocation(), locator.locate());
		return locator.locate();
	}

	public ModelTranscriber extend(final Locator<Model> locator) {
		ModelTranscriber extended = new ModelTranscriber(transcriber, locator);
		
		extended.parent = this;
		extended.componentToRepaint = this.componentToRepaint;
		
		return extended;
	}
	
	private static class CompositeModelLocator implements Locator<Model> {
		private Locator<Model> head;
		private Locator<Model> tail;
		
		public CompositeModelLocator(Locator<Model> head, Locator<Model> tail) {
			this.head = head;
			this.tail = tail;
		}

		@Override
		public Location<Model> locate() {
			return new CompositeLocation<Model>(head.locate(), tail.locate());
		}
	}

	public Connection<Model> createConnection() {
		return transcriber.createConnection(new TriggerHandler<Model>() {
			@Override
			public void handleAfterTrigger(final List<Runnable> runnables) {
				if(componentToRepaint != null) {
//					SwingUtilities.invokeLater(new Runnable() {
//						@Override
//						public void run() {
//							for(Runnable r: runnables)
//								r.run();
//							
//							componentToRepaint.repaint();
//						}
//					});

					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							@Override
							public void run() {
								for(Runnable r: runnables)
									r.run();
								
								componentToRepaint.repaint();
							}
						});
					} catch (InvocationTargetException | InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
	}
}
