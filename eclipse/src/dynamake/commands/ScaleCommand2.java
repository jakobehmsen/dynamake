package dynamake.commands;

import java.io.Serializable;
import java.util.Date;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.numbers.Fraction;
import dynamake.transcription.Collector;

public class ScaleCommand2 implements Command2<Model> {
	public static class AfterScale implements Command2Factory<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Command2<Model> createCommand(Object output) {
			ScaleCommand2.Output resizeOutput = (ScaleCommand2.Output)output;
			return new ScaleCommand2(
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

	public ScaleCommand2(Fraction xDelta, Fraction yDelta, Fraction hChange, Fraction vChange) {
		this.xDelta = xDelta;
		this.yDelta = yDelta;
		this.hChange = hChange;
		this.vChange = vChange;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		
		model.scale(xDelta, yDelta, hChange, vChange, propCtx, 0, collector);
		
		return new Output(xDelta, yDelta, hChange, vChange);
	}
}
