package dynamake.tools;

import java.awt.event.MouseEvent;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandState;
import dynamake.commands.RedoCommand;
import dynamake.commands.UndoCommand;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.TranscribeOnlyCommandStateFactory;

public class RedoTool implements Tool {
	@Override
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e, final ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		collector.execute(new TranscribeOnlyCommandStateFactory<Model> () {
			@Override
			public Model getReference() {
				return modelOver.getModelBehind();
			}
			
			@Override
			public void createPendingCommand(List<CommandState<Model>> commandStates) {
				commandStates.add(
					new PendingCommandState<Model>(
						new RedoCommand(false),
						new UndoCommand(false)
					)
				);
			}
		});
		collector.commit();
	}

	@Override
	public void mousePressed(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) { }

	@Override
	public void mouseDragged(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection) { }

	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) { }
}
