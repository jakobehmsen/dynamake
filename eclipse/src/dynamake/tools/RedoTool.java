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
import dynamake.transcription.SimplePendingCommandFactory;

public class RedoTool implements Tool {
	@Override
	public void mouseReleased(ProductionPanel productionPanel, final ModelComponent modelOver, Connection<Model> connection, Collector<Model> collector, JComponent sourceComponent, Point mousePoint) {
		collector.startTransaction(modelOver.getModelBehind(), PostOnlyTransactionHandler.class);
		collector.execute(new SimplePendingCommandFactory<Model>(new PendingCommandState<Model>(
			new RedoCommand(false),
			new UndoCommand(false)
		)));
		
		collector.commitTransaction();
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
