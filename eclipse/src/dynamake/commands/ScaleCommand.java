package dynamake.commands;

import java.io.Serializable;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.numbers.Fraction;
import dynamake.transcription.Collector;

public class ScaleCommand implements Command<Model> {
	public static class AfterScale implements CommandFactory<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Command<Model> createCommand(Object output) {
			ScaleCommand.Output resizeOutput = (ScaleCommand.Output)output;
			return new ScaleCommand(
				resizeOutput.xDelta.multiply(new Fraction(-1)), 
				resizeOutput.yDelta.multiply(new Fraction(-1)), 
				resizeOutput.hChange.multiply(new Fraction(-1)), 
				resizeOutput.vChange.multiply(new Fraction(-1))
			);
		}
	}
	
	public static class Output implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		public final Fraction xDelta; 
		public final Fraction yDelta; 
		public final Fraction hChange; 
		public final Fraction vChange;
		
		public Output(Fraction xDelta, Fraction yDelta, Fraction hChange, Fraction vChange) {
			this.xDelta = xDelta;
			this.yDelta = yDelta;
			this.hChange = hChange;
			this.vChange = vChange;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Fraction xDelta; 
	private Fraction yDelta; 
	private Fraction hChange; 
	private Fraction vChange;

	public ScaleCommand(Fraction xDelta, Fraction yDelta, Fraction hChange, Fraction vChange) {
		this.xDelta = xDelta;
		this.yDelta = yDelta;
		this.hChange = hChange;
		this.vChange = vChange;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
//		System.out.println("Performed scale(hChange=" + hChange + ",vChange=" + vChange + ") on " + model);
		
		model.scale(xDelta, yDelta, hChange, vChange, propCtx, 0, collector);
		
		return new Output(xDelta, yDelta, hChange, vChange);
	}
}
