package dynamake.tools;

import java.util.List;

import dynamake.commands.DualCommand;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;

public class UnwrapTool extends RepetitiveCanvasTaskTool {
	@Override
	public String getName() {
		return "Unwrap";
	}
	
	@Override
	protected void createDualCommandsForSingleTask(ProductionPanel productionPanel, List<DualCommand<Model>> dualCommands, ModelComponent canvas, Location canvasLocation, ModelComponent modelOver) {
		if(modelOver.getModelBehind() instanceof CanvasModel && ((CanvasModel)modelOver.getModelBehind()).getModelCount() > 0) {
			CanvasModel.appendUnwrapTransaction(dualCommands, modelOver);
		}
	}
}
