package dynamake.tools;

import java.util.List;

import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.CanvasModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.transcription.Collector;

public class UnwrapTool extends RepetitiveCanvasTaskTool {
	@Override
	protected void createCommandStatesForSingleTask(ProductionPanel productionPanel, List<Object> pendingCommands, ModelComponent canvas, ModelComponent modelOver, Collector<Model> collector) {
		if(modelOver.getModelBehind() instanceof CanvasModel && ((CanvasModel)modelOver.getModelBehind()).getModelCount() > 0) {
			CanvasModel.appendUnwrapTransaction2(pendingCommands, modelOver, canvas, collector);
		}
	}
}
