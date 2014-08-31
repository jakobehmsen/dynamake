package dynamake.tools;

import java.awt.Rectangle;
import java.util.List;

import dynamake.commands.CommandSequence;
import dynamake.commands.ResizeCommandFromScope;
import dynamake.commands.ReversibleCommandPair;
import dynamake.commands.TriStatePURCommand;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.numbers.Fraction;
import dynamake.transcription.Collector;

public class EditTool extends BoundsChangeTool {
	@Override
	protected void appendCommandStatesForResize(List<Object> pendingCommands, ModelComponent selection, Rectangle newBounds, Collector<Model> collector) {
		// TODO: The x and t deltas should be performed negatively on each of the immediately contained models
		Fraction currentX = (Fraction)selection.getModelBehind().getProperty("X");
		Fraction currentY = (Fraction)selection.getModelBehind().getProperty("Y");
		
		Fraction xDeltaForward = new Fraction(newBounds.x).subtract(currentX);
		Fraction yDeltaForward = new Fraction(newBounds.y).subtract(currentY);
		
		Fraction currentWidth = (Fraction)selection.getModelBehind().getProperty("Width");
		Fraction currentHeight = (Fraction)selection.getModelBehind().getProperty("Height");
		
		Fraction widthDeltaForward = new Fraction(newBounds.width).subtract(currentWidth);
		Fraction heightDeltaForward = new Fraction(newBounds.height).subtract(currentHeight);
		
		pendingCommands.add(new TriStatePURCommand<Model>(
			new CommandSequence<Model>(
				collector.createProduceCommand(xDeltaForward),
				collector.createProduceCommand(yDeltaForward),
				collector.createProduceCommand(widthDeltaForward),
				collector.createProduceCommand(heightDeltaForward),
				new ReversibleCommandPair<Model>(new ResizeCommandFromScope(), new ResizeCommandFromScope())
			),
			new ReversibleCommandPair<Model>(new ResizeCommandFromScope(), new ResizeCommandFromScope()),
			new ReversibleCommandPair<Model>(new ResizeCommandFromScope(), new ResizeCommandFromScope())
		));
	}
}
