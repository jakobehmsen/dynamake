package dynamake.tools;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.List;

import dynamake.commands.Command;
import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.DualCommandFactory;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;

public class UndoTool implements Tool {
@Override
	public String getName() {
		return "Undo";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) { }

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e, Connection<Model> connection, Collector<Model> collector) { }

	@Override
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e, final ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		collector.execute(new DualCommandFactory<Model>() {
			@Override
			public void createDualCommands(List<DualCommand<Model>> dualCommands) {
				dualCommands.add(
					new DualCommandPair<Model>(
						new Model.UndoTransaction(modelOver.getModelTranscriber().getModelLocation()),
						new Command.Null<Model>()
					)
				);
			}
		});
		collector.enlistCommit();
	}

	@Override
	public void mousePressed(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) { }

	@Override
	public void mouseDragged(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection) { }

	@Override
	public void paint(Graphics g) { }

	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) { }
}
