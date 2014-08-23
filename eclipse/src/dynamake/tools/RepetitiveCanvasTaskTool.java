package dynamake.tools;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import dynamake.commands.CommandState;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.PendingCommandFactory;
import dynamake.transcription.NewChangeTransactionHandler;
import dynamake.transcription.Trigger;

public abstract class RepetitiveCanvasTaskTool implements Tool {
	@Override
	public void mouseReleased(ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
		targetPresenter.reset(collector);
		
		collector.commitTransaction();
	}
	
	private ModelComponent canvas;
	private TargetPresenter targetPresenter;

	@Override
	public void mousePressed(final ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
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
	public void mouseDragged(final ProductionPanel productionPanel, final ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection, JComponent sourceComponent, Point mousePoint) {
		if(modelOver != canvas) {
			final ModelComponent modelOverParent = ModelComponent.Util.getParent(modelOver);
			
			if(modelOverParent == canvas) {
				collector.execute(new Trigger<Model>() {
					@Override
					public void run(Collector<Model> collector) {
						ArrayList<CommandState<Model>> pendingCommands = new ArrayList<CommandState<Model>>();
						createCommandStatesForSingleTask(productionPanel, pendingCommands, modelOverParent, modelOver);
						PendingCommandFactory.Util.executeSequence(collector, modelOverParent.getModelBehind(), pendingCommands, NewChangeTransactionHandler.class);
					}
				});
			}
		}
	}

	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) {
		targetPresenter.reset(collector);
	}
	
	protected abstract void createCommandStatesForSingleTask(ProductionPanel productionPanel, List<CommandState<Model>> pendingCommands, ModelComponent canvas, ModelComponent modelOver);
}
