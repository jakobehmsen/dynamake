package dynamake;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Date;
import java.util.Hashtable;

import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.prevayler.Transaction;

import dynamake.Model.RemovableListener;

public class FloatingTextModel extends Model {
	public static final String PROPERTY_CARET_COLOR = "Caret Color";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private StringBuilder text = new StringBuilder();
	
	@Override
	public Model modelCloneIsolated() {
		FloatingTextModel clone = new FloatingTextModel();
		clone.text.append(this.text);
		return clone;
	}
	
	@Override
	protected void modelScale(Fraction hChange, Fraction vChange, PropogationContext propCtx, int propDistance) {
		Fraction fontSize = (Fraction)getProperty("FontSize");
		if(fontSize == null)
			fontSize = new Fraction(12);
		fontSize = fontSize.multiply(hChange);
		setProperty("FontSize", fontSize, propCtx, propDistance);
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
	
	private static class InsertTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location textLocation;
		private int offset;
		private String text;

		public InsertTransaction(Location textLocation, int offset, String text) {
			this.textLocation = textLocation;
			this.offset = offset;
			this.text = text;
		}

		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime) {
			FloatingTextModel textModel = (FloatingTextModel)textLocation.getChild(prevalentSystem);
			textModel.text.insert(offset, text);
		}

//		@Override
//		public Command<FloatingTextModel> antagonist() {
//			// TODO Auto-generated method stub
//			return null;
//		}
	}
	
	private static class RemoveTransaction implements Command<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Location textLocation;
		private int start;
		private int end;

		public RemoveTransaction(Location textLocation, int start, int end) {
			this.textLocation = textLocation;
			this.start = start;
			this.end = end;
		}

		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime) {
			FloatingTextModel textModel = (FloatingTextModel)textLocation.getChild(prevalentSystem);
			textModel.text.delete(start, end);
		}

//		@Override
//		public Command<FloatingTextModel> antagonist() {
//			// TODO Auto-generated method stub
//			return null;
//		}
	}
	
	private static class FloatingTextModelView extends JTextField implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private FloatingTextModel model;
		private TransactionFactory transactionFactory;
		private JTextPane view;

		public FloatingTextModelView(FloatingTextModel model, TransactionFactory transactionFactory, final ViewManager viewManager) {
			this.model = model;
			this.transactionFactory = transactionFactory;
			
			this.setOpaque(false);
			this.setBorder(null);
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
				caretColor = this.getCaretColor();
			transactions.addTransaction("Set " + PROPERTY_CARET_COLOR, new ColorTransactionBuilder(caretColor, new Action1<Color>() {
				@Override
				public void run(Color color) {
					transactionFactory.executeOnRoot(new PropogationContext(), new Model.SetPropertyOnRootTransaction(transactionFactory.getModelLocation(), PROPERTY_CARET_COLOR, color));
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
		public DualCommandFactory<Model> getImplicitDropAction(ModelComponent target) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void initialize() {
			// TODO Auto-generated method stub
			
		}
	}
	
	@Override
	public Binding<ModelComponent> createView(ModelComponent rootView, final ViewManager viewManager, final TransactionFactory transactionFactory) {
		this.setLocation(transactionFactory.getModelLocator());
		
		final FloatingTextModelView view = new FloatingTextModelView(this, transactionFactory, viewManager);
		view.setText(text.toString());
		
		// TODO: Investigate: Is caret color loaded anywhere?
		final RemovableListener removeListenerForCaretColor = Model.bindProperty(this, PROPERTY_CARET_COLOR, new Action1<Color>() {
			@Override
			public void run(Color value) {
				view.setCaretColor(value);
				viewManager.repaint(view);
			}
		});
		
		final RemovableListener removeListenerForBoundChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		final Model.RemovableListener removeListenerForComponentPropertyChanges = Model.wrapForComponentColorChanges(this, view, view, viewManager, Model.COMPONENT_COLOR_FOREGROUND);
		final Binding<Model> removableListener = RemovableListener.addAll(this, 
			bindProperty(this, "FontSize", new Action1<Fraction>() {
				public void run(Fraction value) {
					Font font = view.getFont();
					view.setFont(new Font(font.getFamily(), font.getStyle(), value.intValue()));
					viewManager.refresh(view);
				}
			})
		);
		
		Model.loadComponentProperties(this, view, Model.COMPONENT_COLOR_FOREGROUND);
		
		view.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				final int start = e.getOffset();
				final int end = e.getOffset() + e.getLength();
				transactionFactory.executeOnRoot(new PropogationContext(), new RemoveTransaction(transactionFactory.getModelLocation(), start, end));
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				final int offset = e.getOffset();
				final String str;
				try {
					str = e.getDocument().getText(e.getOffset(), e.getLength());
					transactionFactory.executeOnRoot(new PropogationContext(), new InsertTransaction(transactionFactory.getModelLocation(), offset, str));
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) { }
		});
		
		viewManager.wasCreated(view);

		return new Binding<ModelComponent>() {
			@Override
			public void releaseBinding() {
				removeListenerForCaretColor.releaseBinding();
				removeListenerForBoundChanges.releaseBinding();
				removeListenerForComponentPropertyChanges.releaseBinding();
				removableListener.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}
}
