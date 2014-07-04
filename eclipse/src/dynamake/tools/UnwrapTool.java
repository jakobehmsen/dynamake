package dynamake.tools;

import java.util.List;

import dynamake.commands.DualCommand;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.CanvasModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;

public class UnwrapTool extends RepetitiveCanvasTaskTool {
	@Override
	public String getName() {
		return "Unwrap";
	}
	
	@Override
	protected void createDualCommandsForSingleTask(ProductionPanel productionPanel, List<DualCommand<Model>> dualCommands, ModelComponent canvas, ModelComponent modelOver) {
		CanvasModel.appendUnwrapTransaction(dualCommands, modelOver);
	}
}
