package dynamake.tools;

import java.util.List;

import dynamake.commands.CommandState;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.CanvasModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.transcription.Collector;

public class UnwrapTool extends RepetitiveCanvasTaskTool {
	@Override
	protected void createCommandStatesForSingleTask(ProductionPanel productionPanel, List<CommandState<Model>> pendingCommands, ModelComponent canvas, ModelComponent modelOver) {
		if(modelOver.getModelBehind() instanceof CanvasModel && ((CanvasModel)modelOver.getModelBehind()).getModelCount() > 0) {
			CanvasModel.appendUnwrapTransaction(pendingCommands, modelOver, canvas);
		}
	}
	
	@Override
	protected void createCommandStatesForSingleTask2(ProductionPanel productionPanel, List<Object> pendingCommands, ModelComponent canvas, ModelComponent modelOver, Collector<Model> collector) {
		if(modelOver.getModelBehind() instanceof CanvasModel && ((CanvasModel)modelOver.getModelBehind()).getModelCount() > 0) {
			// TODO: Replace above implementation!!!!!
//			CanvasModel.appendUnwrapTransaction(pendingCommands, modelOver, canvas);
		}
	}
}
