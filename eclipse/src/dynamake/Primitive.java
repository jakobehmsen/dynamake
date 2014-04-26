package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.prevayler.Transaction;

public class Primitive extends Model {
	public interface Implementation extends Serializable {
		String getName();
		void execute(Model receiver, Model sender, Object change, PropogationContext propCtx);
	}
	
	public static Implementation[] getImplementationSingletons() {
		return new Implementation[] {
			new Implementation() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;
				
				@Override
				public String getName() {
					return "Take Background";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx) {
					if(change instanceof Map.PropertyChanged && ((Map.PropertyChanged)change).name.equals("Background")) {
						Map.PropertyChanged propertyChanged = (Map.PropertyChanged)change;
						receiver.sendChanged(new Model.Atom(propertyChanged.value), propCtx);
					}
				}
			},
			new Implementation() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;
				
				@Override
				public String getName() {
					return "Change Background";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx) {
					if(change instanceof Model.Atom) {
						Model.Atom atom = (Model.Atom)change;
						Map.SetProperty setProperty = new Map.SetProperty("Background", atom.value);
						receiver.sendChanged(setProperty, propCtx);
					}
				}
			},
			new Implementation() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;
				
				@Override
				public String getName() {
					return "Take Foreground";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx) {
					if(change instanceof Map.PropertyChanged && ((Map.PropertyChanged)change).name.equals("Foreground")) {
						Map.PropertyChanged propertyChanged = (Map.PropertyChanged)change;
						receiver.sendChanged(new Model.Atom(propertyChanged.value), propCtx);
					}
				}
			},
			new Implementation() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;
				
				@Override
				public String getName() {
					return "Change Foreground";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx) {
					if(change instanceof Model.Atom) {
						Model.Atom atom = (Model.Atom)change;
						Map.SetProperty setProperty = new Map.SetProperty("Foreground", atom.value);
						receiver.sendChanged(setProperty, propCtx);
					}
				}
			},
			new Implementation() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;
				
				@Override
				public String getName() {
					return "Darken";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx) {
					if(change instanceof Model.Atom) {
						Model.Atom atom = (Model.Atom)change;
						Color darkenedColor = ((Color)atom.value).darker();
						receiver.sendChanged(new Model.Atom(darkenedColor), propCtx);
					}
				}
			},
			new Implementation() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;
				
				@Override
				public String getName() {
					return "Brighten";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx) {
					if(change instanceof Model.Atom) {
						Model.Atom atom = (Model.Atom)change;
						Color darkenedColor = ((Color)atom.value).brighter();
						receiver.sendChanged(new Model.Atom(darkenedColor), propCtx);
					}
				}
			}
		};
	}
	
	public static Factory[] getImplementationsFromModels(final Location modelLocation) {
		return new Factory[] {
			new Factory() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public String getName() {
					return "Mark Visit";
				}
				
				@Override
				public Object create(Model rootModel, Hashtable<String, Object> arguments) {
					final Model model = (Model)modelLocation.getChild(rootModel);
					
					return new Implementation() {
						/**
						 * 
						 */
						private static final long serialVersionUID = 1L;

						@Override
						public String getName() {
							return "Mark Visit";
						}
						
						@Override
						public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx) {
							PropogationContext newPropCtx = propCtx.markVisitedBy(model);
							receiver.sendChanged(change, newPropCtx);
						}
					};
				}
			}
		};
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Implementation implementation;
	
	public Primitive(Implementation implementation) {
		this.implementation = implementation;
	}
	
	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx) {
		implementation.execute(this, sender, change, propCtx);
	}
	
	private static class PrimitiveView extends JPanel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Primitive model;
		private TransactionFactory transactionFactory;

		public PrimitiveView(Primitive model, TransactionFactory transactionFactory) {
			this.model = model;
			this.transactionFactory = transactionFactory;
			
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
			setLayout(new BorderLayout());
			add(new JLabel(model.implementation.getName(), JLabel.CENTER), BorderLayout.CENTER);
		}

		@Override
		public Model getModel() {
			return model;
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}
		
		@Override
		public TransactionPublisher getObjectTransactionPublisher() {
			return new TransactionPublisher() {
				@Override
				public void appendContainerTransactions(
						TransactionMapBuilder transactions, ModelComponent child) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void appendTransactions(TransactionMapBuilder transactions) {
					Model.appendComponentPropertyChangeTransactions(model, transactionFactory, transactions);
				}

				@Override
				public void appendDroppedTransactions(TransactionMapBuilder transactions) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void appendDropTargetTransactions(ModelComponent dropped,
						Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions) {
					// TODO Auto-generated method stub
					
				}
			};
		}

		@Override
		public Transaction<Model> getImplicitDropAction(ModelComponent target) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Override
	public Binding<ModelComponent> createView(final ViewManager viewManager,
			TransactionFactory transactionFactory) {
		final PrimitiveView view = new PrimitiveView(this, transactionFactory);
		
		final Binding<Model> removableListener = RemovableListener.addAll(this, 
			bindProperty(this, "Background", new Action1<Color>() {
				public void run(Color value) {
					view.setBackground(value);
					viewManager.refresh(view);
				}
			}),
			bindProperty(this, "Foreground", new Action1<Color>() {
				public void run(Color value) {
					view.setForeground(value);
					viewManager.refresh(view);
				}
			})
		);
		
		final RemovableListener removableListenerForBoundChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		
		return new Binding<ModelComponent>() {
			@Override
			public void releaseBinding() {
				removableListener.releaseBinding();
				removableListenerForBoundChanges.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}
}
