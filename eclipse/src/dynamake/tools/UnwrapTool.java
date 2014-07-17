package dynamake.tools;

import java.util.List;

import dynamake.commands.CommandState;
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
	protected void createCommandStatesForSingleTask(ProductionPanel productionPanel, List<CommandState<Model>> commandStates, ModelComponent canvas, ModelComponent modelOver) {
		if(modelOver.getModelBehind() instanceof CanvasModel && ((CanvasModel)modelOver.getModelBehind()).getModelCount() > 0) {
			CanvasModel.appendUnwrapTransaction2(commandStates, modelOver, canvas);
		}
	}
}
