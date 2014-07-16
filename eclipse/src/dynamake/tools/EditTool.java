package dynamake.tools;

import java.awt.Rectangle;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandState;
import dynamake.commands.ResizeCommand2;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.numbers.Fraction;

public class EditTool extends BoundsChangeTool {
	@Override
	public String getName() {
		return "Edit";
	}

	@Override
	protected void appendCommandStatesForResize(List<CommandState<Model>> commandStates, ModelComponent selection, Rectangle newBounds) {
		// TODO: The x and t deltas should be performed negatively on each of the immediately contained models
		Fraction currentX = (Fraction)selection.getModelBehind().getProperty("X");
		Fraction currentY = (Fraction)selection.getModelBehind().getProperty("Y");
		
		Fraction xDeltaForward = new Fraction(newBounds.x).subtract(currentX);
		Fraction yDeltaForward = new Fraction(newBounds.y).subtract(currentY);
		
		Fraction currentWidth = (Fraction)selection.getModelBehind().getProperty("Width");
		Fraction currentHeight = (Fraction)selection.getModelBehind().getProperty("Height");
		
		Fraction widthDeltaForward = new Fraction(newBounds.width).subtract(currentWidth);
		Fraction heightDeltaForward = new Fraction(newBounds.height).subtract(currentHeight);
		
		commandStates.add(new PendingCommandState<Model>(
			new ResizeCommand2(xDeltaForward, yDeltaForward, widthDeltaForward, heightDeltaForward),
			new ResizeCommand2.AfterResize()
		));
	}
}
