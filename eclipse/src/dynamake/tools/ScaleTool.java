package dynamake.tools;

import java.awt.Rectangle;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.commands.ScaleCommand;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.numbers.Fraction;

public class ScaleTool extends BoundsChangeTool {
	@Override
	public String getName() {
		return "Scale";
	}
	
	@Override
	protected void appendDualCommandsForResize(List<DualCommand<Model>> dualCommands, Location location, ModelComponent selection, Rectangle newBounds) {
		Fraction currentX = (Fraction)selection.getModelBehind().getProperty("X");
		Fraction currentY = (Fraction)selection.getModelBehind().getProperty("Y");
		
		Fraction xDeltaForward = new Fraction(newBounds.x).subtract(currentX);
		Fraction yDeltaForward = new Fraction(newBounds.y).subtract(currentY);
		Fraction xDeltaBackward = currentX.subtract(new Fraction(newBounds.x));
		Fraction yDeltaBackward = currentY.subtract(new Fraction(newBounds.y));
		
		Fraction currentWidth = (Fraction)selection.getModelBehind().getProperty("Width");
		Fraction currentHeight = (Fraction)selection.getModelBehind().getProperty("Height");
		
		Fraction hChangeForward = new Fraction(newBounds.width).divide(currentWidth);
		Fraction vChangeForward = new Fraction(newBounds.height).divide(currentHeight);
		Fraction hChangeBackward = currentWidth.divide(new Fraction(newBounds.width));
		Fraction vChangeBackward = currentHeight.divide(new Fraction(newBounds.height));
		
		dualCommands.add(new DualCommandPair<Model>(
			new ScaleCommand(location, xDeltaForward, yDeltaForward, hChangeForward, vChangeForward), 
			new ScaleCommand(location, xDeltaBackward, yDeltaBackward, hChangeBackward, vChangeBackward)
		));
	}

	@Override
	protected void appendCommandStatesForResize(List<CommandState<Model>> commandStates, ModelComponent selection, Rectangle newBounds) {
		// TODO Auto-generated method stub
		
	}
}
