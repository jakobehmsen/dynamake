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
		void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance);
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
					return "New Background";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
					if(change instanceof Model.PropertyChanged && ((Model.PropertyChanged)change).name.equals(Model.PROPERTY_BACKGROUND)) {
						Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;
						receiver.sendChanged(new Model.Atom(propertyChanged.value), propCtx, propDistance, 0);
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
					if(change instanceof Model.Atom) {
						Model.Atom atom = (Model.Atom)change;
						Model.SetProperty setProperty = new Model.SetProperty(Model.PROPERTY_BACKGROUND, atom.value);
						receiver.sendChanged(setProperty, propCtx, propDistance, 0);
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
					return "New Foreground";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
					if(change instanceof Model.PropertyChanged && ((Model.PropertyChanged)change).name.equals(Model.PROPERTY_FOREGROUND)) {
						Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;
						receiver.sendChanged(new Model.Atom(propertyChanged.value), propCtx, propDistance, 0);
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
					if(change instanceof Model.Atom) {
						Model.Atom atom = (Model.Atom)change;
						Model.SetProperty setProperty = new Model.SetProperty(Model.PROPERTY_FOREGROUND, atom.value);
						receiver.sendChanged(setProperty, propCtx, propDistance, 0);
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
					if(change instanceof Model.Atom) {
						Model.Atom atom = (Model.Atom)change;
						Color darkenedColor = ((Color)atom.value).darker();
						receiver.sendChanged(new Model.Atom(darkenedColor), propCtx, propDistance, 0);
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
					if(change instanceof Model.Atom) {
						Model.Atom atom = (Model.Atom)change;
						Color brightenedColor = ((Color)atom.value).brighter();
						receiver.sendChanged(new Model.Atom(brightenedColor), propCtx, propDistance, 0);
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
					return "From Here";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
					if(propDistance == 1)
						receiver.sendChanged(change, propCtx, propDistance, changeDistance);
				}
			},
			new Implementation() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;
				
				@Override
				public String getName() {
					return "From Other";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
					if(propDistance > 1)
						receiver.sendChanged(change, propCtx, propDistance, changeDistance);
				}
			},
			new Implementation() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;
				
				@Override
				public String getName() {
					return "New Look";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
					if(change instanceof Model.PropertyChanged)
						receiver.sendChanged(change, propCtx, propDistance, changeDistance);
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
						public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
							PropogationContext newPropCtx = propCtx.markVisitedBy(model);
							receiver.sendChanged(change, newPropCtx, propDistance, changeDistance);
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
	public void modelChanged(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance) {
		implementation.execute(this, sender, change, propCtx, propDistance, changeDistance);
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
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendTransactions(TransactionMapBuilder transactions) {
			Model.appendComponentPropertyChangeTransactions(model, transactionFactory, transactions);
		}

		@Override
		public void appendDroppedTransactions(ModelComponent target, Rectangle droppedBounds, TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendDropTargetTransactions(ModelComponent dropped,
				Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
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
		
		viewManager.wasCreated(view);
		
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
