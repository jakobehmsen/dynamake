package dynamake;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JLabel;

import dynamake.LiveModel.LivePanel;

public class Primitive extends Model {
	public interface Implementation extends Serializable {
		String getName();
		void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch);
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
					return "New Color";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
					if(change instanceof Model.PropertyChanged && ((Model.PropertyChanged)change).name.equals(Model.PROPERTY_COLOR)) {
						Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;
						receiver.sendChanged(new Model.Atom(propertyChanged.value), propCtx, propDistance, 0, branch);
					} else {
						receiver.absorbChange(branch);
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
					return "Change Color";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
					if(change instanceof Model.Atom) {
						Model.Atom atom = (Model.Atom)change;
						Model.SetProperty setProperty = new Model.SetProperty(Model.PROPERTY_COLOR, atom.value);
						receiver.sendChanged(setProperty, propCtx, propDistance, 0, branch);
					} else {
						receiver.absorbChange(branch);
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
					if(change instanceof Model.Atom) {
						Model.Atom atom = (Model.Atom)change;
						Color darkenedColor = ((Color)atom.value).darker();
						receiver.sendChanged(new Model.Atom(darkenedColor), propCtx, propDistance, 0, branch);
					} else {
						receiver.absorbChange(branch);
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
					if(change instanceof Model.Atom) {
						Model.Atom atom = (Model.Atom)change;
						Color brightenedColor = ((Color)atom.value).brighter();
						receiver.sendChanged(new Model.Atom(brightenedColor), propCtx, propDistance, 0, branch);
					} else {
						receiver.absorbChange(branch);
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
					if(propDistance == 1)
						receiver.sendChanged(change, propCtx, propDistance, changeDistance, branch);
					else {
						receiver.absorbChange(branch);
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
					return "From Other";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
					if(propDistance > 1)
						receiver.sendChanged(change, propCtx, propDistance, changeDistance, branch);
					else {
						receiver.absorbChange(branch);
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
					return "New Look";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
					if(change instanceof Model.PropertyChanged)
						receiver.sendChanged(change, propCtx, propDistance, changeDistance, branch);
					else {
						receiver.absorbChange(branch);
					}
				}
			}
			
			,
			new Implementation() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;
				
				@Override
				public String getName() {
					return "Mouse Down";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
					if(change instanceof Model.MouseDown) {
						receiver.sendChanged(change, propCtx, propDistance, changeDistance, branch);
					} else {
						receiver.absorbChange(branch);
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
					return "Mouse Up";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
					if(change instanceof Model.MouseUp) {
						receiver.sendChanged(change, propCtx, propDistance, changeDistance, branch);
					} else {
						receiver.absorbChange(branch);
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
					return "Tell Color";
				}
				
				@Override
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
					receiver.sendChanged(new Model.TellProperty(Model.PROPERTY_COLOR), propCtx, propDistance, 0, branch);
				}
			},
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
				public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, PrevaylerServiceBranch<Model> branch) {
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
						public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
							propCtx.markVisitedBy(model);
							receiver.sendChanged(change, propCtx, propDistance, changeDistance, branch);
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
	protected void modelAppendScale(Fraction hChange, Fraction vChange,
			List<DualCommand<Model>> dualCommands) {
		Fraction fontSize = (Fraction)getProperty("FontSize");
		if(fontSize == null)
			fontSize = new Fraction(12);
		fontSize = fontSize.multiply(hChange);
		
		dualCommands.add(SetPropertyTransaction.createDual(this, "FontSize", fontSize));
	}
	
	@Override
	public Model modelCloneIsolated() {
		return new Primitive(implementation);
	}
	
	@Override
	public void modelChanged(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, PrevaylerServiceBranch<Model> branch) {
		implementation.execute(this, sender, change, propCtx, propDistance, changeDistance, branch);
	}
	
	private static class PrimitiveView extends JLabel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Primitive model;
		private TransactionFactory transactionFactory;

		public PrimitiveView(Primitive model, TransactionFactory transactionFactory) {
			super(model.implementation.getName(), JLabel.CENTER);
			this.model = model;
			this.transactionFactory = transactionFactory;
			
			try {
				Font font = ResourceManager.INSTANCE.getResource("Primitive Font", Font.class);
				setFont(font);
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
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
		public void appendTransactions(ModelComponent livePanel, TransactionMapBuilder transactions, PrevaylerServiceBranch<Model> branch) {
			Model.appendComponentPropertyChangeTransactions(livePanel, model, transactionFactory, transactions, branch);
		}

		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, TransactionMapBuilder transactions, PrevaylerServiceBranch<Model> branch) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, transactions, branch);
		}

		@Override
		public void appendDropTargetTransactions(ModelComponent livePanel,
				ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions, PrevaylerServiceBranch<Model> branch) {

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

	@Override
	public Binding<ModelComponent> createView(ModelComponent rootView,
			final ViewManager viewManager, TransactionFactory transactionFactory) {
		this.setLocation(transactionFactory.getModelLocator());
		
		final PrimitiveView view = new PrimitiveView(this, transactionFactory);
		
		final Binding<Model> removableListener = RemovableListener.addAll(this, 
			bindProperty(this, Model.PROPERTY_COLOR, new Action1<Color>() {
				public void run(Color value) {
					view.setBackground(value);
				}
			}),
			bindProperty(this, "FontSize", new Action1<Fraction>() {
				public void run(Fraction value) {
					Font font = view.getFont();
					view.setFont(new Font(font.getFamily(), font.getStyle(), value.intValue()));
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
