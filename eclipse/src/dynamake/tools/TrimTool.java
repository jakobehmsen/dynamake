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
	protected void createDualCommandsForSingleTask(ProductionPanel productionPanel, List<DualCommand<Model>> dualCommands, ModelComponent canvas, Location canvasLocation, ModelComponent modelOver) {
		CanvasModel.appendRemoveTransaction(dualCommands, productionPanel.livePanel, modelOver, canvasLocation, (CanvasModel)canvas.getModelBehind());
	}

	@Override
	protected void createCommandStatesForSingleTask(
			ProductionPanel productionPanel,
			List<CommandState<Model>> commandStates, ModelComponent canvas,
			ModelComponent modelOver) {
		// TODO Auto-generated method stub
		
	}
}
