package dynamake.tools;

import java.awt.Rectangle;
import java.util.List;

import dynamake.commands.CommandSequence;
import dynamake.commands.CommandState;
import dynamake.commands.PendingCommandState;
import dynamake.commands.ResizeCommandFromScope;
import dynamake.commands.ReversibleCommandPair;
import dynamake.commands.ScaleCommand;
import dynamake.commands.ScaleCommandFromScope;
import dynamake.commands.TriStatePURCommand;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.numbers.Fraction;
import dynamake.transcription.Collector;

public class ScaleTool extends BoundsChangeTool {
	@Override
	protected void appendCommandStatesForResize(List<Object> pendingCommands, ModelComponent selection, Rectangle newBounds, Collector<Model> collector) {
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
		
//		commandStates.add(new PendingCommandState<Model>(
//			new ScaleCommand(xDeltaForward, yDeltaForward, hChangeForward, vChangeForward), 
//			new ScaleCommand(xDeltaBackward, yDeltaBackward, hChangeBackward, vChangeBackward)
//		));
		
		pendingCommands.add(new TriStatePURCommand<Model>(
			new CommandSequence<Model>(
				collector.createProduceCommand(xDeltaForward),
				collector.createProduceCommand(yDeltaForward),
				collector.createProduceCommand(hChangeForward),
				collector.createProduceCommand(vChangeForward),
				new ReversibleCommandPair<Model>(new ScaleCommandFromScope(), new ScaleCommandFromScope())
			),
			new ReversibleCommandPair<Model>(new ScaleCommandFromScope(), new ScaleCommandFromScope()),
			new ReversibleCommandPair<Model>(new ScaleCommandFromScope(), new ScaleCommandFromScope())
		));
	}
}
