package dynamake.tools;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JComponent;

import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.commands.ResizeCommand;
import dynamake.models.CanvasModel;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.numbers.Fraction;

public class EditTool extends BoundsChangeTool {
	@Override
	public String getName() {
		return "Edit";
	}

	@Override
	protected void appendDualCommandsForResize(
			List<DualCommand<Model>> dualCommands, Location location, ModelComponent selection, Rectangle currentBounds, Rectangle newBounds) {
		Fraction currentX = (Fraction)selection.getModelBehind().getProperty("X");
		Fraction currentY = (Fraction)selection.getModelBehind().getProperty("Y");
		
		Fraction xDeltaForward = new Fraction(newBounds.x).subtract(currentX);
		Fraction yDeltaForward = new Fraction(newBounds.y).subtract(currentY);
		Fraction xDeltaBackward = currentX.subtract(new Fraction(newBounds.x));
		Fraction yDeltaBackward = currentY.subtract(new Fraction(newBounds.y));
		
		Fraction currentWidth = (Fraction)selection.getModelBehind().getProperty("Width");
		Fraction currentHeight = (Fraction)selection.getModelBehind().getProperty("Height");
		
		Fraction widthDeltaForward = new Fraction(newBounds.width).subtract(currentWidth);
		Fraction heightDeltaForward = new Fraction(newBounds.height).subtract(currentHeight);
		Fraction widthDeltaBackward = currentWidth.subtract(new Fraction(newBounds.width));
		Fraction heightDeltaBackward = currentHeight.subtract(new Fraction(newBounds.height));
		
		dualCommands.add(new DualCommandPair<Model>(
			new ResizeCommand(location, xDeltaForward, yDeltaForward, widthDeltaForward, heightDeltaForward),
			new ResizeCommand(location, xDeltaBackward, yDeltaBackward, widthDeltaBackward, heightDeltaBackward)
		));
	}
}
