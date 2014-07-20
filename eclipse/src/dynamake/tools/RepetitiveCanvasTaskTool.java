package dynamake.tools;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandFactory;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;

public abstract class RepetitiveCanvasTaskTool implements Tool {
	@Override
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		targetPresenter.reset(collector);
		
		collector.commit();
	}
	
	private ModelComponent canvas;
	private TargetPresenter targetPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		canvas = ModelComponent.Util.closestCanvasModelComponent(modelOver);
		
		targetPresenter = new TargetPresenter(
			productionPanel,
			new TargetPresenter.Behavior() {
				@Override
				public Color getColorForTarget(ModelComponent target) {
					return ProductionPanel.TARGET_OVER_COLOR;
				}
				
				@Override
				public boolean acceptsTarget(ModelComponent target) {
					return target == canvas;
				}
			}
		);
		
		targetPresenter.update(canvas, collector);
	}

	@Override
	public void mouseDragged(final ProductionPanel productionPanel, MouseEvent e, final ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection) {
		if(modelOver != canvas) {
			final ModelComponent modelOverParent = ModelComponent.Util.getParent(modelOver);
			
			if(modelOverParent == canvas) {
				collector.execute(new PendingCommandFactory<Model>() {
					@Override
					public Model getReference() {
						return modelOverParent.getModelBehind();
					}
					
					@Override
					public void createPendingCommand(List<CommandState<Model>> commandStates) {
						createCommandStatesForSingleTask(productionPanel, commandStates, modelOverParent, modelOver);
					}
				});
			}
		}
	}

	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) {
		targetPresenter.reset(collector);
	}
	
	protected abstract void createCommandStatesForSingleTask(ProductionPanel productionPanel, List<CommandState<Model>> commandStates, ModelComponent canvas, ModelComponent modelOver);
}
