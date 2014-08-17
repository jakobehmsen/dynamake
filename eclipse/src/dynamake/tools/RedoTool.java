package dynamake.tools;

import java.awt.Point;

import javax.swing.JComponent;

import dynamake.commands.PendingCommandState;
import dynamake.commands.RedoCommand;
import dynamake.commands.UndoCommand;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.HistoryHandler;
import dynamake.transcription.PostOnlyHistoryHandler;
import dynamake.transcription.SimplePendingCommandFactory;
import dynamake.transcription.Trigger;

public class RedoTool implements Tool {
	@Override
	public void mouseReleased(ProductionPanel productionPanel, final ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
		collector.execute(new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
				if(modelOver.getModelBehind().canRedo()) {
					collector.execute(new SimplePendingCommandFactory<Model>(modelOver.getModelBehind(), new PendingCommandState<Model>(
						new RedoCommand(false),
						new UndoCommand(false)
					)) {
						@Override
						public Class<? extends HistoryHandler<Model>> getHistoryHandlerClass() {
							return PostOnlyHistoryHandler.class;
						}
					});
					
					collector.commit();
				}
			}
		});
	}

	@Override
	public void mousePressed(ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) { }

	@Override
	public void mouseDragged(ProductionPanel productionPanel, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection, JComponent sourceComponent, Point mousePoint) { }

	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) { }
}
