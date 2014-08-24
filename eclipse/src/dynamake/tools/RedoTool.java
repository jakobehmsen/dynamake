package dynamake.tools;

import java.awt.Point;

import javax.swing.JComponent;

import dynamake.commands.PendingCommandState;
import dynamake.commands.RedoCommand;
import dynamake.commands.UndoCommand;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.transcription.PostOnlyTransactionHandler;
import dynamake.transcription.Collector;
import dynamake.transcription.Connection;
import dynamake.transcription.TransactionHandler;
import dynamake.transcription.SimplePendingCommandFactory;
import dynamake.transcription.Trigger;

public class RedoTool implements Tool {
	@Override
	public void mouseReleased(ProductionPanel productionPanel, final ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
		collector.startTransaction(modelOver.getModelBehind(), PostOnlyTransactionHandler.class);
		collector.execute(new Trigger<Model>() {
			@Override
			public void run(Collector<Model> collector) {
				if(modelOver.getModelBehind().canRedo()) {
					collector.execute(new SimplePendingCommandFactory<Model>(modelOver.getModelBehind(), new PendingCommandState<Model>(
						new RedoCommand(false),
						new UndoCommand(false)
					)) {
						@Override
						public Class<? extends TransactionHandler<Model>> getTransactionHandlerClass() {
							return PostOnlyTransactionHandler.class;
						}
					});
					
					collector.commitTransaction();
				}
			}
		});
	}

	@Override
	public void mousePressed(ProductionPanel productionPanel, ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) { }

	@Override
	public void mouseDragged(ProductionPanel productionPanel, ModelComponent modelOver, Collector<Model> collector, Connection<Model> connection, JComponent sourceComponent, Point mousePoint) { }

	@Override
	public void rollback(ProductionPanel productionPanel, Collector<Model> collector) { 
		collector.rejectTransaction();
	}
}
