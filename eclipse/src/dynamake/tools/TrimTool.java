package dynamake.tools;

import java.util.List;

import dynamake.commands.DualCommand;
import dynamake.models.CanvasModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.LiveModel.ProductionPanel;

public class TrimTool extends RepetitiveCanvasTaskTool {
	@Override
	public String getName() {
		return "Trim";
	}
	
	@Override
	protected void createDualCommandsForSingleTask(ProductionPanel productionPanel, List<DualCommand<Model>> dualCommands, ModelComponent canvas, ModelComponent modelOver) {
		CanvasModel.appendRemoveTransaction(dualCommands, productionPanel.livePanel, modelOver, canvas.getModelTranscriber().getModelLocation(), (CanvasModel)canvas.getModelBehind());
	}
}
