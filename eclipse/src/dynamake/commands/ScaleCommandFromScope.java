package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.numbers.Fraction;
import dynamake.transcription.Collector;

public class ScaleCommandFromScope implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
		Fraction vChange = (Fraction)scope.consume();
		Fraction hChange = (Fraction)scope.consume();
		Fraction yDelta = (Fraction)scope.consume();
		Fraction xDelta = (Fraction)scope.consume();
		
		Model model = (Model)location.getChild(prevalentSystem);
//		System.out.println("Performed scale(hChange=" + hChange + ",vChange=" + vChange + ") on " + model);
		
		model.scale(xDelta, yDelta, hChange, vChange, propCtx, 0, collector);
		
		scope.produce(xDelta.multiply(new Fraction(-1)));
		scope.produce(yDelta.multiply(new Fraction(-1)));
		scope.produce(hChange.reciprocal());
		scope.produce(vChange.reciprocal());
		
//		return new Output(xDelta, yDelta, hChange, vChange);
		return null;
	}
}
