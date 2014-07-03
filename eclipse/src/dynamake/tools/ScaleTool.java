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
	protected void appendDualCommandsForSameCanvasBoundsChange(
			List<DualCommand<Model>> dualCommands, ModelComponent selection,
			Rectangle newBounds) {
		selection.getModelBehind().appendScale(newBounds, dualCommands);
	}

	@Override
	public void rollback() {
		// TODO Auto-generated method stub
		
	}
}
