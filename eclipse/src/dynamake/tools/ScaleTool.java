package dynamake.tools;

import java.awt.Rectangle;
import java.util.List;

import dynamake.commands.DualCommand;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelLocation;

public class ScaleTool extends BoundsChangeTool {
	@Override
	public String getName() {
		return "Scale";
	}
	
	@Override
	protected void appendDualCommandsForResize(
			List<DualCommand<Model>> dualCommands, Location location,
			ModelComponent selection, Rectangle currentBounds, Rectangle newBounds) {
		selection.getModelBehind().appendScale((ModelLocation)location, newBounds, dualCommands);
	}
}
