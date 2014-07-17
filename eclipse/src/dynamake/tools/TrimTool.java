package dynamake.tools;

import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.DualCommand;
import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;

public class TrimTool extends RepetitiveCanvasTaskTool {
	@Override
	public String getName() {
		return "Trim";
	}

	@Override
	protected void createCommandStatesForSingleTask(ProductionPanel productionPanel, List<CommandState<Model>> commandStates, ModelComponent canvas, ModelComponent modelOver) {
		CanvasModel.appendRemoveTransaction2(commandStates, productionPanel.livePanel, modelOver, (CanvasModel)canvas.getModelBehind());
	}
}
