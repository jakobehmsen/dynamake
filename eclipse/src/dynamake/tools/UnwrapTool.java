package dynamake.tools;

import java.util.List;

import dynamake.commands.CommandState;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.CanvasModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;

public class UnwrapTool extends RepetitiveCanvasTaskTool {
	@Override
	protected void createCommandStatesForSingleTask(ProductionPanel productionPanel, List<CommandState<Model>> pendingCommands, ModelComponent canvas, ModelComponent modelOver) {
		if(modelOver.getModelBehind() instanceof CanvasModel && ((CanvasModel)modelOver.getModelBehind()).getModelCount() > 0) {
			CanvasModel.appendUnwrapTransaction(pendingCommands, modelOver, canvas);
		}
	}
}
