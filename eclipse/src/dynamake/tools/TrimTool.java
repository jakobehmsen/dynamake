package dynamake.tools;

import java.util.List;

import dynamake.commands.CommandState;
import dynamake.models.CanvasModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.Collector;

public class TrimTool extends RepetitiveCanvasTaskTool {
	@Override
	protected void createCommandStatesForSingleTask(ProductionPanel productionPanel, List<CommandState<Model>> pendingCommands, ModelComponent canvas, ModelComponent modelOver) {
		CanvasModel.appendRemoveTransaction(pendingCommands, productionPanel.livePanel, modelOver, (CanvasModel)canvas.getModelBehind());
	}
	
	@Override
	protected void createCommandStatesForSingleTask2(ProductionPanel productionPanel, List<Object> pendingCommands, ModelComponent canvas, ModelComponent modelOver, Collector<Model> collector) {
		CanvasModel.appendRemoveTransaction2(pendingCommands, productionPanel.livePanel, modelOver, (CanvasModel)canvas.getModelBehind(), collector);
	}
}
