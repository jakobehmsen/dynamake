package dynamake.tools;

import java.awt.Rectangle;
import java.util.List;

import dynamake.commands.DualCommand;
import dynamake.models.Model;
import dynamake.models.ModelComponent;

public class ScaleTool extends BoundsChangeTool {
	@Override
	public String getName() {
		return "Scale";
	}
	
	@Override
	protected void appendDualCommandsForResize(
			List<DualCommand<Model>> dualCommands, ModelComponent selection,
			Rectangle currentBounds, Rectangle newBounds) {
		selection.getModelBehind().appendScale(newBounds, dualCommands);
	}
}
