package dynamake;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Date;
import java.util.Hashtable;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.prevayler.Transaction;

public class TextModel extends Model {
	public static final String PROPERTY_CARET_COLOR = "Caret Color";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private StringBuilder text = new StringBuilder();
	
	@Override
	public Model modelCloneIsolated() {
		TextModel clone = new TextModel();
		clone.text.append(this.text);
		return clone;
	}
	
	public void setText(String text) {
		this.text.setLength(0);
		this.text.append(text);
	}
	
	public String getText() {
		return text.toString();
	}
	
	public void remove(int start, int end) {
		
	}
	
	private static class InsertTransaction implements Command<TextModel> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private int offset;
		private String text;

		public InsertTransaction(int offset, String text) {
			this.offset = offset;
			this.text = text;
		}

		public void executeOn(PropogationContext propCtx, TextModel prevalentSystem, Date executionTime) {
			prevalentSystem.text.insert(offset, text);
		}

//		@Override
//		public Command<TextModel> antagonist() {
//			// TODO Auto-generated method stub
//			return null;
//		}
	}
	
	private static class RemoveTransaction implements Command<TextModel> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private int start;
		private int end;

		public RemoveTransaction(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public void executeOn(PropogationContext propCtx, TextModel prevalentSystem, Date executionTime) {
			prevalentSystem.text.delete(start, end);
		}

//		@Override
//		public Command<TextModel> antagonist() {
//			// TODO Auto-generated method stub
//			return null;
//		}
	}
	
	private static class ModelScrollPane extends JScrollPane implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private TextModel model;
		private TransactionFactory transactionFactory;
//		private TransactionFactory metaTransactionFactory;
		private JTextPane view;

		public ModelScrollPane(TextModel model, TransactionFactory transactionFactory, final ViewManager viewManager, JTextPane view) {
			super(view);
			this.model = model;
			this.transactionFactory = transactionFactory;
//			this.metaTransactionFactory = transactionFactory.extend(new Model.MetaModelLocator());
			this.view = view;
		}		

		@Override
		public Model getModelBehind() {
			return model;
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}

		@Override
		public void appendContainerTransactions(
			TransactionMapBuilder transactions, ModelComponent child) {
		}

		@Override
		public void appendTransactions(TransactionMapBuilder transactions) {
			Model.appendComponentPropertyChangeTransactions(model, transactionFactory, transactions);
			
			Color caretColor = (Color)model.getProperty(PROPERTY_CARET_COLOR);
			if(caretColor == null)
				caretColor = view.getCaretColor();
			transactions.addTransaction("Set " + PROPERTY_CARET_COLOR, new ColorTransactionBuilder(caretColor, new Action1<Color>() {
				@Override
				public void run(Color color) {
					PropogationContext propCtx = new PropogationContext();
					transactionFactory.execute(propCtx, new Model.SetPropertyTransaction(PROPERTY_CARET_COLOR, color));
				}
			}));
		}
		
		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, final ModelComponent target, final Rectangle droppedBounds, TransactionMapBuilder transactions) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, transactions);
			
//			if(target.getModelBehind() instanceof CanvasModel) {
//				transactions.addTransaction("For new button", new Runnable() {
//					@Override
//					public void run() {
//						Rectangle creationBounds = droppedBounds;
//						Hashtable<String, Object> creationArgs = new Hashtable<String, Object>();
//						creationArgs.put("Text", model.text.toString());
//						getTransactionFactory().executeOnRoot(
//							new CanvasModel.AddModelTransaction(target.getTransactionFactory().getLocation(), creationBounds, creationArgs, new ButtonModelFactory())
//						);
//					}
//				});
//			}
		}

		@Override
		public void appendDropTargetTransactions(ModelComponent livePanel,
				ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Command<Model> getImplicitDropAction(ModelComponent target) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	@Override
	public Binding<ModelComponent> createView(final ViewManager viewManager, final TransactionFactory transactionFactory) {
		this.setLocation(transactionFactory.getModelLocation());
		
		final JTextPane view = new JTextPane();
		final ModelScrollPane viewScrollPane = new ModelScrollPane(this, transactionFactory, viewManager, view);
		view.setText(text.toString());
		
		// TODO: Investigate: Is caret color loaded anywhere?
		final RemovableListener removeListenerForCaretColor = Model.bindProperty(this, PROPERTY_CARET_COLOR, new Action1<Color>() {
			@Override
			public void run(Color value) {
				view.setCaretColor(value);
				viewManager.repaint(view);
			}
		});
		
		final RemovableListener removeListenerForBoundChanges = Model.wrapForBoundsChanges(this, viewScrollPane, viewManager);
		final Model.RemovableListener removeListenerForComponentPropertyChanges = Model.wrapForComponentColorChanges(this, viewScrollPane, view, viewManager, Model.COMPONENT_COLOR_FOREGROUND);
		
		Model.loadComponentProperties(this, view, Model.COMPONENT_COLOR_FOREGROUND);
		
		view.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				final int start = e.getOffset();
				final int end = e.getOffset() + e.getLength();
				PropogationContext propCtx = new PropogationContext();
				transactionFactory.execute(propCtx, new RemoveTransaction(start, end));
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				final int offset = e.getOffset();
				final String str;
				try {
					str = e.getDocument().getText(e.getOffset(), e.getLength());
					PropogationContext propCtx = new PropogationContext();
					transactionFactory.execute(propCtx, new InsertTransaction(offset, str));
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) { }
		});
		
		viewManager.wasCreated(viewScrollPane);

		return new Binding<ModelComponent>() {
			@Override
			public void releaseBinding() {
				removeListenerForCaretColor.releaseBinding();
				removeListenerForBoundChanges.releaseBinding();
				removeListenerForComponentPropertyChanges.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return viewScrollPane;
			}
		};
	}
}
