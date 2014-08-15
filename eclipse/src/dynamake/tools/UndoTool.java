package dynamake.tools;

import java.awt.event.MouseEvent;

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
import dynamake.transcription.SimpleExPendingCommandFactory2;
import dynamake.transcription.Trigger;

public class UndoTool implements Tool {
	@Override
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e, final ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) {
		collector.execute(new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
				if(modelOver.getModelBehind().canUndo()) {
					collector.execute(new SimpleExPendingCommandFactory2<Model>(modelOver.getModelBehind(), new PendingCommandState<Model>(
						new UndoCommand(false),
						new RedoCommand(false)
					)) {
						@Override
						public Class<? extends HistoryHandler<Model>> getHistoryHandlerClass() {
							return PostOnlyHistoryHandler.class;
						}
					});
					
//					collector.execute(
//						ExPendingCommandFactory2.Util.sequence(new TranscribeOnlyPendingCommandFactory<Model> () {
//							@Override
//							public Model getReference() {
//								return modelOver.getModelBehind();
//							}
//							
//							@Override
//							public void createPendingCommands(List<CommandState<Model>> commandStates) {
//								commandStates.add(
//									new PendingCommandState<Model>(
//										new UndoCommand(false),
//										new RedoCommand(false)
//									)
//								);
//							}
//						})
//					);
					
					collector.commit();
				}
			}
		});
	}

	@Override
	public void mousePressed(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector) { }

	@Override
	public void mouseDragged(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection) { }

	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) { }
}
