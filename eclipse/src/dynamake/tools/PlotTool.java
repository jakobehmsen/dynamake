package dynamake.tools;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import dynamake.commands.Command2;
import dynamake.commands.Command2Factory;
import dynamake.commands.CommandState;
import dynamake.commands.CommandStateFactory;
import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.commands.PendingCommandState;
import dynamake.commands.UnwrapToLocationsCommand;
import dynamake.commands.UnwrapToLocationsCommand2;
import dynamake.commands.WrapCommand;
import dynamake.commands.WrapCommand2;
import dynamake.models.CanvasModel;
import dynamake.models.CanvasModel.AddModelCommand2.AfterRemove;
import dynamake.models.CompositeModelLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelLocation;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.factories.CanvasModelFactory;
import dynamake.models.factories.Factory;
import dynamake.numbers.RectangleF;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.DualCommandFactory;

public class PlotTool implements Tool {
	@Override
	public String getName() {
		return "Plot";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {

	}

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e, Connection<Model> connection, Collector<Model> collector) {

	}

	@Override
	public void mouseReleased(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		if(mouseDown != null) {
			final Rectangle creationBoundsInProductionPanel = interactionPresenter.getPlotBounds(mouseDown, e.getPoint());
			final Rectangle creationBoundsInSelection = SwingUtilities.convertRectangle(productionPanel, creationBoundsInProductionPanel, interactionPresenter.getSelectionFrame());
			
			// Find components within the creation bounds of the selection
			final ArrayList<ModelComponent> componentsWithinBounds = new ArrayList<ModelComponent>();
			for(Component c: ((JComponent)interactionPresenter.getSelection()).getComponents()) {
				if(creationBoundsInSelection.contains(c.getBounds())) {
					// Add in reverse order because views are positioned in the reverse order in the CanvasModel
					// This way, the views are sorted ascending index-wise
					componentsWithinBounds.add(0, (ModelComponent)c);
				}
			}

			if(interactionPresenter.getSelection().getModelBehind() instanceof CanvasModel) {
				final ModelComponent selection = interactionPresenter.getSelection();
				// Wrap if one more models are contained within the effect frame
				if(componentsWithinBounds.size() > 0) {
//					collector.execute(new DualCommandFactory<Model>() {
//						@Override
//						public Model getReference() {
//							return selection.getModelBehind();
//						}
//						
//						@Override
//						public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//							CanvasModel target = (CanvasModel)selection.getModelBehind();
//							int indexOfWrapper = target.getModelCount() - componentsWithinBounds.size();
//							ModelLocation wrapperLocationInTarget = new CanvasModel.IndexLocation(indexOfWrapper);
//							
//							// Each of the model locations should be moved from target to wrapper
//							Location[] modelLocations = new Location[componentsWithinBounds.size()];
//							int[] modelIndexes = new int[componentsWithinBounds.size()];
//							for(int i = 0; i < modelLocations.length; i++) {
//								ModelComponent view = componentsWithinBounds.get(i);
//								modelLocations[i] = new CompositeModelLocation(
//									(ModelLocation)location,
//									target.getLocationOf(view.getModelBehind())
//								);
//								modelIndexes[i] = target.indexOfModel(view.getModelBehind());
//							}
//							
//							dualCommands.add(new DualCommandPair<Model>(
//								new WrapCommand(location, creationBoundsInSelection, modelLocations), 
//								new UnwrapToLocationsCommand(location, wrapperLocationInTarget, modelIndexes, creationBoundsInSelection)
//							));
//						}
//					});
					
					collector.execute(new CommandStateFactory<Model>() {
						@Override
						public Model getReference() {
							return selection.getModelBehind();
						}
						
						@Override
						public void createDualCommands(List<CommandState<Model>> commandStates) {
							CanvasModel target = (CanvasModel)selection.getModelBehind();
							
							Location[] modelLocations = new Location[componentsWithinBounds.size()];
							for(int i = 0; i < modelLocations.length; i++) {
								ModelComponent view = componentsWithinBounds.get(i);
								modelLocations[i] = target.getLocationOf(view.getModelBehind());
							}
							
							commandStates.add(new PendingCommandState<Model>(
								new WrapCommand2(new RectangleF(creationBoundsInSelection), modelLocations), 
								new UnwrapToLocationsCommand2.AfterWrap(),
								new WrapCommand2.AfterUnwrap()
							));
						}
					});
				} else {
					
//					collector.execute(new DualCommandFactory<Model>() {
//						@Override
//						public Model getReference() {
//							return selection.getModelBehind();
//						}
//						
//						@Override
//						public void createDualCommands(Location location, List<DualCommand<Model>> dualCommands) {
//							ModelComponent target = selection;
//							
//							CanvasModel canvasModel = (CanvasModel)target.getModelBehind();
//							int index = canvasModel.getModelCount();
//							
//							dualCommands.add(new DualCommandPair<Model>(
//								new CanvasModel.AddModelCommand(location, creationBoundsInSelection, factory), 
//								new CanvasModel.RemoveModelCommand(location, index)
//								// The model removed here, should be cloned before removing and then used for the redo in an as-is factory
//							));
//						}
//					});
					
					collector.execute(new CommandStateFactory<Model>() {
						@Override
						public Model getReference() {
							return selection.getModelBehind();
						}
						
						@Override
						public void createDualCommands(List<CommandState<Model>> commandStates) {
							Factory factory = new CanvasModelFactory();
							commandStates.add(new PendingCommandState<Model>(
								new CanvasModel.AddModelCommand2(creationBoundsInSelection, factory),
								new CanvasModel.RemoveModelCommand2.AfterAdd(),
								new CanvasModel.AddModelCommand2.AfterRemove()
							));
//							commandStates.add(new PendingCommandState<Model>(
//								new CanvasModel.AddModelCommand2(creationBoundsInSelection, factory),
//								new CanvasModel.RemoveModelCommand2.AfterAdd()
//							));
						}
					});
				}
			}

			interactionPresenter.reset(collector);
			interactionPresenter = null;
			
			collector.commit();
			
			mouseDown = null;
		}
	}
	
	private Point mouseDown;
	private InteractionPresenter interactionPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, final MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		if(modelOver.getModelBehind() instanceof CanvasModel) {
			mouseDown = e.getPoint();
			Point referencePoint = SwingUtilities.convertPoint((JComponent)e.getSource(), e.getPoint(), (JComponent)modelOver);
			
			interactionPresenter = new InteractionPresenter(productionPanel);
			interactionPresenter.selectFromEmpty(modelOver, referencePoint, collector);
		}
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, final MouseEvent e, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection) {
		if(mouseDown != null) {
			final Rectangle plotBoundsInProductionPanel = interactionPresenter.getPlotBounds(mouseDown, e.getPoint());
			
			interactionPresenter.changeEffectFrameDirect2(plotBoundsInProductionPanel, collector);
		}
	}

	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) {
		if(mouseDown != null) {
			interactionPresenter.reset(collector);
			interactionPresenter = null;
		}
	}
}
