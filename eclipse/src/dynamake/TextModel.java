package dynamake;

import java.awt.Color;
import java.awt.Point;
import java.util.Date;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.prevayler.Transaction;

public class TextModel extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private StringBuilder text = new StringBuilder();
	
	public void setText(String text) {
		this.text.setLength(0);
		this.text.append(text);
	}
	
	public String getText() {
		return text.toString();
	}
	
	public void remove(int start, int end) {
		
	}
	
	private static class InsertTransaction implements Transaction<TextModel> {
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

		public void executeOn(TextModel prevalentSystem, Date executionTime) {
			prevalentSystem.text.insert(offset, text);
		}
	}
	
	private static class RemoveTransaction implements Transaction<TextModel> {
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

		public void executeOn(TextModel prevalentSystem, Date executionTime) {
			prevalentSystem.text.delete(start, end);
		}
	}
	
	private static class ModelScrollPane extends JScrollPane implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private TextModel model;
		private TransactionFactory transactionFactory;
		private TransactionFactory metaTransactionFactory;
		private JTextPane view;

		public ModelScrollPane(TextModel model, TransactionFactory transactionFactory, final ViewManager viewManager, JTextPane view) {
			super(view);
			this.model = model;
			this.transactionFactory = transactionFactory;
			this.metaTransactionFactory = transactionFactory.extend(new Model.MetaModelLocator());
			this.view = view;
		}		

		@Override
		public Model getModel() {
			return model;
		}

		@Override
		public void appendContainerTransactions(
			TransactionMapBuilder transactions, ModelComponent child) {
		}

		@Override
		public void appendTransactions(TransactionMapBuilder transactions) {
			Model.appendComponentPropertyChangeTransactions(model, transactionFactory, transactions);
			
			Color caretColor = (Color)model.getMetaModel().get("CaretColor");
			if(caretColor == null)
				caretColor = view.getCaretColor();
			transactions.addTransaction("Set Caret Color", new ColorTransactionBuilder(caretColor, new Action1<Color>() {
				@Override
				public void run(Color color) {
					metaTransactionFactory.execute(new Map.SetPropertyTransaction("CaretColor", color));
				}
			}));
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}
		
		@Override
		public Transaction<Model> getDefaultDropTransaction(
				ModelComponent dropped, Point dropPoint) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void appendDroppedTransactions(TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}
	}
	
	@Override
	public Binding<ModelComponent> createView(final ViewManager viewManager, final TransactionFactory transactionFactory) {
		final JTextPane view = new JTextPane();
		final ModelScrollPane viewScrollPane = new ModelScrollPane(this, transactionFactory, viewManager, view);
		view.setText(text.toString());
		
		// TODO: Investigate: Is caret color loaded anywhere?
		final RemovableListener removeListenerForCaretColor = Model.bindProperty(this, "CaretColor", new Action1<Color>() {
			@Override
			public void run(Color value) {
				view.setCaretColor(value);
				viewManager.repaint(view);
			}
		});
		
		final RemovableListener removeListenerForBoundChanges = Model.wrapForBoundsChanges(this, viewScrollPane, viewManager);
		final Model.RemovableListener removeListenerForComponentPropertyChanges = Model.wrapForComponentPropertyChanges(this, viewScrollPane, view, viewManager);
		
		Model.loadComponentProperties(this, view);
		
		view.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				final int start = e.getOffset();
				final int end = e.getOffset() + e.getLength();
				transactionFactory.execute(new RemoveTransaction(start, end));
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				final int offset = e.getOffset();
				final String str;
				try {
					str = e.getDocument().getText(e.getOffset(), e.getLength());
					transactionFactory.execute(new InsertTransaction(offset, str));
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) { }
		});

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
