package dynamake;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Date;
import java.util.List;

import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import dynamake.LiveModel.LivePanel;

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
	
	@Override
	protected void modelAppendScale(Fraction hChange, Fraction vChange,
			List<DualCommand<Model>> dualCommands) {
		Fraction fontSize = (Fraction)getProperty("FontSize");
		if(fontSize == null)
			fontSize = new Fraction(12);
		fontSize = fontSize.multiply(hChange);
		
		dualCommands.add(SetPropertyTransaction.createDual(this, "FontSize", fontSize));
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
	
	private static class InsertedText {
		public final int offset;
		public final String text;
		
		public InsertedText(int offset, String text) {
			this.offset = offset;
			this.text = text;
		}
	}
	
	private static class RemovedText {
		public final int start;
		public final int end;
		
		public RemovedText(int start, int end) {
			this.start = start;
			this.end = end;
		}
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

		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
			TextModel textModel = (TextModel)textLocation.getChild(prevalentSystem);
			textModel.text.insert(offset, text);
			textModel.sendChanged(new InsertedText(offset, text), propCtx, 0, 0, branch);
		}
		
		@Override
		public boolean occurredWithin(Location location) {
			return true;
		}
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

		public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, PrevaylerServiceBranch<Model> branch) {
			TextModel textModel = (TextModel)textLocation.getChild(prevalentSystem);
			textModel.text.delete(start, end);
			textModel.sendChanged(new RemovedText(start, end), propCtx, 0, 0, branch);
		}
		
		@Override
		public boolean occurredWithin(Location location) {
			return true;
		}
	}
	
	private static class FloatingTextModelView extends JTextField implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private TextModel model;
		private TransactionFactory transactionFactory;

		public FloatingTextModelView(TextModel model, TransactionFactory transactionFactory, final ViewManager viewManager) {
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
			LivePanel livePanel, TransactionMapBuilder transactions, ModelComponent child, PrevaylerServiceBranch<Model> branch) {
		}

		@Override
		public void appendTransactions(final ModelComponent livePanel, TransactionMapBuilder transactions, final PrevaylerServiceBranch<Model> branch) {
			Model.appendComponentPropertyChangeTransactions(livePanel, model, transactionFactory, transactions, branch);
			
			Color caretColor = (Color)model.getProperty(PROPERTY_CARET_COLOR);
			if(caretColor == null)
				caretColor = this.getCaretColor();
			final Color currentCaretColor = caretColor;
			transactions.addTransaction("Set " + PROPERTY_CARET_COLOR, new ColorTransactionBuilder(caretColor, new Action1<Color>() {
				@Override
				public void run(final Color color) {
					PropogationContext propCtx = new PropogationContext();
					
					branch.execute(propCtx, new DualCommandFactory<Model>() {
						@Override
						public void createDualCommands(List<DualCommand<Model>> dualCommands) {
							dualCommands.add(new DualCommandPair<Model>(
								new Model.SetPropertyTransaction(transactionFactory.getModelLocation(), PROPERTY_CARET_COLOR, color),
								new Model.SetPropertyTransaction(transactionFactory.getModelLocation(), PROPERTY_CARET_COLOR, currentCaretColor)
							));
							
							dualCommands.add(LiveModel.SetOutput.createDual((LiveModel.LivePanel)livePanel, transactionFactory.getModelLocation())); // Absolute location
						}
					});
				}
			}));
		}
		
		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, final ModelComponent target, final Rectangle droppedBounds, TransactionMapBuilder transactions, PrevaylerServiceBranch<Model> branch) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, transactions, branch);
		}

		@Override
		public void appendDropTargetTransactions(
			ModelComponent livePanel,ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions, PrevaylerServiceBranch<Model> branch) {

		}

		@Override
		public DualCommandFactory<Model> getImplicitDropAction(ModelComponent target) {
			return null;
		}

		@Override
		public void initialize() {
			
		}
		
		@Override
		public void visitTree(Action1<ModelComponent> visitAction) {
			visitAction.run(this);
		}
	}
	
	// The same as LiveModel.TAG_CAUSED_BY_TOGGLE_BUTTON; 
	// TODO: Consider: Perhaps, there should be a general "caused by view" tag?
	private static final int TAG_CAUSED_BY_VIEW = 2; 
	
	private static class ViewDocument extends PlainDocument {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private TextModel model;
		private TransactionFactory transactionFactory;
		
		public ViewDocument(TextModel model, TransactionFactory transactionFactory) {
			this.model = model;
			this.transactionFactory = transactionFactory;
		}
		
		@Override
		public void insertString(final int offs, final String str, AttributeSet a)
				throws BadLocationException {
			documentInsert(offs, str, a);
			
			PropogationContext propCtx = new PropogationContext(TAG_CAUSED_BY_VIEW);
			
			PrevaylerServiceBranch<Model> branch = transactionFactory.createBranch();
			branch.execute(propCtx, new DualCommandFactory<Model>() {
				@Override
				public void createDualCommands(List<DualCommand<Model>> dualCommands) {
					dualCommands.add(new DualCommandPair<Model>(
						new InsertTransaction(transactionFactory.getModelLocation(), offs, str), 
						new RemoveTransaction(transactionFactory.getModelLocation(), offs, offs + str.length())
					));
				}
			});
			branch.close();
		}
		
		public void documentInsert(int offs, String str, AttributeSet a) throws BadLocationException {
			super.insertString(offs, str, a);
		}
		
		@Override
		public void remove(int offs, int len) throws BadLocationException {
			documentRemove(offs, len);

			final int start = offs;
			final int end = offs + len;
			
			PropogationContext propCtx = new PropogationContext(TAG_CAUSED_BY_VIEW);
			
			PrevaylerServiceBranch<Model> branch = transactionFactory.createBranch();
			branch.execute(propCtx, new DualCommandFactory<Model>() {
				@Override
				public void createDualCommands(List<DualCommand<Model>> dualCommands) {
					String removedText = model.text.substring(start, end);
					dualCommands.add(new DualCommandPair<Model>(
						new RemoveTransaction(transactionFactory.getModelLocation(), start, end), 
						new InsertTransaction(transactionFactory.getModelLocation(), start, removedText)
					));
				}
			});
			branch.close();
		}
		
		public void documentRemove(int offs, int len) throws BadLocationException {
			super.remove(offs, len);
		}
	}
	
	@Override
	public Binding<ModelComponent> createView(ModelComponent rootView, final ViewManager viewManager, final TransactionFactory transactionFactory) {
		this.setLocation(transactionFactory.getModelLocator());
		
		final FloatingTextModelView view = new FloatingTextModelView(this, transactionFactory, viewManager);
		
		final RemovableListener removeListenerForCaretColor = Model.bindProperty(this, PROPERTY_CARET_COLOR, new Action1<Color>() {
			@Override
			public void run(Color value) {
				view.setCaretColor(value);
				viewManager.repaint(view);
			}
		});
		
		final ViewDocument document = new ViewDocument(this, transactionFactory);
		
		final RemovableListener removeListenerForBoundChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		final Model.RemovableListener removeListenerForComponentPropertyChanges = Model.wrapForComponentColorChanges(this, view, view, viewManager, Model.COMPONENT_COLOR_FOREGROUND);
		final Binding<Model> removableListener = RemovableListener.addAll(this, 
			bindProperty(this, "FontSize", new Action1<Fraction>() {
				public void run(Fraction value) {
					Font font = view.getFont();
					view.setFont(new Font(font.getFamily(), font.getStyle(), value.intValue()));
					viewManager.refresh(view);
				}
			}),
			RemovableListener.addObserver(this, new Observer() {
				@Override
				public void removeObservee(Observer observee) { }
				
				@Override
				public void addObservee(Observer observee) { }
				
				@Override
				public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
					if(!propCtx.isTagged(TAG_CAUSED_BY_VIEW)) {
						if(change instanceof InsertedText) {
							InsertedText insertText = (InsertedText)change;
							
							try {
								document.documentInsert(insertText.offset, insertText.text, null);
							} catch (BadLocationException e) {
								e.printStackTrace();
							}
						} else if(change instanceof RemovedText) {
							RemovedText removedText = (RemovedText)change;
							
							try {
								document.documentRemove(removedText.start, removedText.end - removedText.start);
							} catch (BadLocationException e) {
								e.printStackTrace();
							}
						}
					}
				}
			})
		);
		
		Model.loadComponentProperties(this, view, Model.COMPONENT_COLOR_FOREGROUND);
		
		try {
			document.documentInsert(0, text.toString(), null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		view.setDocument(document);
		
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
