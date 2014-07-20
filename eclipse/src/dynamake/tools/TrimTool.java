package dynamake.tools;

import java.util.List;

import dynamake.commands.CommandState;
import dynamake.models.CanvasModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;

public class TrimTool extends RepetitiveCanvasTaskTool {
	@Override
	protected void createCommandStatesForSingleTask(ProductionPanel productionPanel, List<CommandState<Model>> commandStates, ModelComponent canvas, ModelComponent modelOver) {
		CanvasModel.appendRemoveTransaction(commandStates, productionPanel.livePanel, modelOver, (CanvasModel)canvas.getModelBehind());
	}
}
