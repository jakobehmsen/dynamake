package dynamake;

//import java.util.Date;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.prevayler.Transaction;

import dynamake.Model.Observer;
import dynamake.Model.PropertyChanged;
import dynamake.Model.RemovableListener;

//import org.prevayler.Transaction;

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
		private ViewManager viewManager;
		private JTextPane view;

		public ModelScrollPane(TextModel model, TransactionFactory transactionFactory, final ViewManager viewManager, JTextPane view) {
			super(view);
			this.model = model;
			this.transactionFactory = transactionFactory;
			this.view = view;
			
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if(e.getButton() == 3)
						viewManager.selectAndActive(ModelScrollPane.this, e.getX(), e.getY());
				}
			});
			
			final MouseListener[] viewMouseListeners = view.getMouseListeners();
			for(MouseListener l: viewMouseListeners)
				view.removeMouseListener(l);
			
			view.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if(e.getButton() == 3)
						viewManager.selectAndActive(ModelScrollPane.this, e.getX(), e.getY());
				}
				
				public void mousePressed(MouseEvent e) {
					if(viewManager.getState() == LiveModel.STATE_USE) {
						for(MouseListener l: viewMouseListeners)
							l.mousePressed(e);
					} else if(viewManager.getState() == LiveModel.STATE_EDIT) {
//						viewManager.select(ModelScrollPane.this, e.getX(), e.getY());
					}
				}
				
				Cursor cursor;
				
				@Override
				public void mouseEntered(MouseEvent e) {
					if(viewManager.getState() != LiveModel.STATE_USE) {
						cursor = ((Component)e.getSource()).getCursor();
						((Component)e.getSource()).setCursor(Cursor.getDefaultCursor());
					}
				}
				
				@Override
				public void mouseExited(MouseEvent e) {
					if(viewManager.getState() != LiveModel.STATE_USE) {
						((Component)e.getSource()).setCursor(cursor);
					}
				}
			});
		}		

		@Override
		public Model getModel() {
			return model;
		}

		@Override
		public void appendContainerTransactions(
				TransactionMapBuilder transactions, ModelComponent child) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public Color getPrimaryColor() {
			return getViewport().getComponent(0).getBackground();
		}

		@Override
		public void appendTransactions(TransactionMapBuilder transactions) {
			Model.appendComponentPropertyChangeTransactions(model, transactionFactory, transactions);
			
			Color caretColor = (Color)model.getProperty("CaretColor");
			if(caretColor == null)
				caretColor = view.getCaretColor();
			transactions.addTransaction("Set Caret Color", new ColorTransactionBuilder(caretColor, new Action1<Color>() {
				@Override
				public void run(Color color) {
					transactionFactory.execute(new Model.SetPropertyTransaction("CaretColor", color));
				}
			}));
//			LinkedHashMap<String, Color> colors = new LinkedHashMap<String, Color>();
//			colors.put("Black", Color.BLACK);
//			colors.put("Blue", Color.BLUE);
//			colors.put("Cyan", Color.CYAN);
//			colors.put("Dark Gray", Color.DARK_GRAY);
//			colors.put("Gray", Color.GRAY);
//			colors.put("Green", Color.GREEN);
//			colors.put("Light Gray", Color.LIGHT_GRAY);
//			colors.put("Magenta", Color.MAGENTA);
//			colors.put("Orange", Color.ORANGE);
//			colors.put("Pink", Color.PINK);
//			colors.put("Red", Color.RED);
//			colors.put("White", Color.WHITE);
//			colors.put("Yellow", Color.YELLOW);
//			
//			TransactionMapBuilder backgroundMapBuilder = new TransactionMapBuilder(); 
//			
//			for(final Map.Entry<String, Color> entry: colors.entrySet()) {
//				backgroundMapBuilder.addTransaction(entry.getKey(), new Runnable() {
//					@Override
//					public void run() {
//						transactionFactory.execute(new Model.SetPropertyTransaction("Background", entry.getValue()));
//					}
//				});
//			}
//			
//			TransactionMapBuilder foregroundMapBuilder = new TransactionMapBuilder(); 
//			
//			for(final Map.Entry<String, Color> entry: colors.entrySet()) {
//				foregroundMapBuilder.addTransaction(entry.getKey(), new Runnable() {
//					@Override
//					public void run() {
//						transactionFactory.execute(new Model.SetPropertyTransaction("Foreground", entry.getValue()));
//					}
//				});
//			}
//			
//			transactions.addTransaction("Set Background", backgroundMapBuilder);
//			transactions.addTransaction("Set Foreground", foregroundMapBuilder);
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}
	}
	
	@Override
	public Binding<ModelComponent> createView(final ViewManager viewManager, final TransactionFactory transactionFactory) {
		final JTextPane view = new JTextPane();
		final ModelScrollPane viewScrollPane = new ModelScrollPane(this, transactionFactory, viewManager, view);
		view.setText(text.toString());
		
		final RemovableListener removeListenerForCaretColor = Model.bindProperty(this, "CaretColor", new Action1<Color>() {
			@Override
			public void run(Color value) {
				view.setCaretColor(value);
			}
		});
		
		final RemovableListener removeListenerForBoundChanges = Model.wrapForBoundsChanges(this, viewScrollPane);
//		final RemovableListener removeListenerForComponentPropertyChanges = Model.wrapForComponentPropertyChanges(this, view);
		final RemovableListener removeListenerForComponentPropertyChanges = RemovableListener.addObserver(this, new Observer() {
			@Override
			public void changed(Model sender, Object change) {
				if(change instanceof PropertyChanged) {
					PropertyChanged propertyChanged = (PropertyChanged)change;
					if(propertyChanged.name.equals("Background")) {
						view.setBackground((Color)propertyChanged.value);
						view.revalidate();
						view.repaint();
					} else if(propertyChanged.name.equals("Foreground")) {
						view.setForeground((Color)propertyChanged.value);
//						view.validate();
//						view.repaint();
						viewManager.repaint(view);
					}
				}
			}
		});
		
		Model.loadComponentProperties(this, view);
//		final Model.RemovableListener removableListenerForComponentPropertyChanges = Model.wrapForComponentPropertyChanges(this, view);

		Model.wrapForFocus(viewManager, viewScrollPane, view);
		Model.wrapForFocus(viewManager, viewScrollPane, viewScrollPane);
		
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
		
//		final Model.RemovableListener removableListener = RemovableListener.addObserver(this, new Observer() {
//			@Override
//			public void changed(Model sender, Object change) {
//				if(change instanceof Model.PropertyChanged) {
//					Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;
//					if(propertyChanged.name.equals("Background")) {
//						view.setBackground((Color)propertyChanged.value);
//					} else if(propertyChanged.name.equals("Foreground")) {
//						view.setForeground((Color)propertyChanged.value);
//					}
//				}
//			}
//		});

		return new Binding<ModelComponent>() {
			@Override
			public void releaseBinding() {
				removeListenerForCaretColor.releaseBinding();
				removeListenerForBoundChanges.releaseBinding();
//				removableListenerForComponentPropertyChanges.releaseBinding();
				removeListenerForComponentPropertyChanges.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return viewScrollPane;
			}
		};
	}
}
