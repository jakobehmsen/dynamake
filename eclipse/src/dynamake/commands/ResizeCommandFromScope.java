package dynamake.commands;

import dynamake.commands.ResizeCommand.Output;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.numbers.Fraction;
import dynamake.transcription.Collector;

public class ResizeCommandFromScope implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location, ExecutionScope scope) {
		Fraction heightDelta = (Fraction)scope.consume();
		Fraction widthDelta = (Fraction)scope.consume();
		Fraction yDelta = (Fraction)scope.consume();
		Fraction xDelta = (Fraction)scope.consume();
		
		Model model = (Model)location.getChild(prevalentSystem);
		
		model.resize(xDelta, yDelta, widthDelta, heightDelta, propCtx, 0, collector);
		
		scope.produce(xDelta.multiply(new Fraction(-1)));
		scope.produce(yDelta.multiply(new Fraction(-1)));
		scope.produce(widthDelta.multiply(new Fraction(-1)));
		scope.produce(heightDelta.multiply(new Fraction(-1)));
		
		return null;
	}
}
