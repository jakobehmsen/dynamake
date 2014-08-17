package dynamake.models;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.concurrent.ExecutionException;

import javax.swing.JLabel;


import dynamake.delegates.Action1;
import dynamake.menubuilders.CompositeMenuBuilder;
import dynamake.models.LiveModel.LivePanel;
import dynamake.numbers.Fraction;
import dynamake.resources.ResourceManager;
import dynamake.transcription.Collector;

public class Primitive extends Model {
	public interface Implementation extends Serializable {
		String getName();
		void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector);
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
					if(change instanceof Model.PropertyChanged && ((Model.PropertyChanged)change).name.equals(Model.PROPERTY_COLOR)) {
						Model.PropertyChanged propertyChanged = (Model.PropertyChanged)change;
						receiver.sendChanged(new Model.Atom(propertyChanged.value), propCtx, propDistance, 0, collector);
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
					if(change instanceof Model.Atom) {
						Model.Atom atom = (Model.Atom)change;
						Model.SetProperty setProperty = new Model.SetProperty(Model.PROPERTY_COLOR, atom.value);
						receiver.sendChanged(setProperty, propCtx, propDistance, 0, collector);
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
					if(change instanceof Model.Atom) {
						Model.Atom atom = (Model.Atom)change;
						if(atom.value != null) {
							Color darkenedColor = ((Color)atom.value).darker();
							receiver.sendChanged(new Model.Atom(darkenedColor), propCtx, propDistance, 0, collector);
						}
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
					if(change instanceof Model.Atom) {
						Model.Atom atom = (Model.Atom)change;
						if(atom.value != null) {
							Color brightenedColor = ((Color)atom.value).brighter();
							receiver.sendChanged(new Model.Atom(brightenedColor), propCtx, propDistance, 0, collector);
						}
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
					if(propDistance == 1)
						receiver.sendChanged(change, propCtx, propDistance, changeDistance, collector);
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
					if(propDistance > 1)
						receiver.sendChanged(change, propCtx, propDistance, changeDistance, collector);
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
					if(change instanceof Model.PropertyChanged)
						receiver.sendChanged(change, propCtx, propDistance, changeDistance, collector);
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
					if(change instanceof Model.MouseDown) {
						receiver.sendChanged(change, propCtx, propDistance, changeDistance, collector);
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
					if(change instanceof Model.MouseUp) {
						receiver.sendChanged(change, propCtx, propDistance, changeDistance, collector);
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
				public void execute(Model receiver, Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
					receiver.sendChanged(new Model.TellProperty(Model.PROPERTY_COLOR), propCtx, propDistance, 0, collector);
				}
			},
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
	protected void modelScale(Fraction hChange, Fraction vChange, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		Fraction fontSize = (Fraction)getProperty("FontSize");
		if(fontSize == null)
			fontSize = new Fraction(12);
		fontSize = fontSize.multiply(hChange);
		
		setProperty("FontSize", fontSize, propCtx, propDistance, collector);
	}
	
	@Override
	public void modelChanged(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		implementation.execute(this, sender, change, propCtx, propDistance, changeDistance, collector);
	}
	
	private static class PrimitiveView extends JLabel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Primitive model;
		private ModelTranscriber modelTranscriber;

		public PrimitiveView(Primitive model, ModelTranscriber modelTranscriber) {
			super(model.implementation.getName(), JLabel.CENTER);
			this.model = model;
			this.modelTranscriber = modelTranscriber;
			
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
		public ModelTranscriber getModelTranscriber() {
			return modelTranscriber;
		}

		@Override
		public void appendContainerTransactions(
				LivePanel livePanel, CompositeMenuBuilder menuBuilder, ModelComponent child) {

		}

		@Override
		public void appendTransactions(ModelComponent livePanel, CompositeMenuBuilder menuBuilder) {
			Model.appendComponentPropertyChangeTransactions(livePanel, model, modelTranscriber, menuBuilder);
		}

		@Override
		public void appendDroppedTransactions(ModelComponent livePanel, ModelComponent target, Rectangle droppedBounds, CompositeMenuBuilder menuBuilder) {
			Model.appendGeneralDroppedTransactions(livePanel, this, target, droppedBounds, menuBuilder);
		}

		@Override
		public void appendDropTargetTransactions(ModelComponent livePanel,
				ModelComponent dropped, Rectangle droppedBounds, Point dropPoint, CompositeMenuBuilder menuBuilder) {

		}

		@Override
		public void initialize() {
			
		}
	}

	@Override
	public Binding<ModelComponent> createView(ModelComponent rootView,
			final ViewManager viewManager, ModelTranscriber modelTranscriber) {
		this.setLocator(modelTranscriber.getModelLocator());
		
		final PrimitiveView view = new PrimitiveView(this, modelTranscriber);
		
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
	
	@Override
	public Model cloneBase() {
		return new Primitive(implementation);
	}
}
