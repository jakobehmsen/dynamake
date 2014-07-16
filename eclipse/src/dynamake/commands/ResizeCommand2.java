package dynamake.commands;

import java.io.Serializable;
import java.util.Date;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.numbers.Fraction;
import dynamake.transcription.Collector;

public class ResizeCommand2 implements Command2<Model> {
	public static class AfterResize implements Command2Factory<Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Command2<Model> createCommand(Object output) {
			ResizeCommand2.Output resizeOutput = (ResizeCommand2.Output)output;
			return new ResizeCommand2(
				resizeOutput.xDelta.multiply(new Fraction(-1)), 
				resizeOutput.yDelta.multiply(new Fraction(-1)), 
				resizeOutput.widthDelta.multiply(new Fraction(-1)), 
				resizeOutput.heightDelta.multiply(new Fraction(-1))
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
		public final Fraction widthDelta;
		public final Fraction heightDelta;
		
		public Output(Fraction xDelta, Fraction yDelta, Fraction widthDelta, Fraction heightDelta) {
			this.xDelta = xDelta;
			this.yDelta = yDelta;
			this.widthDelta = widthDelta;
			this.heightDelta = heightDelta;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Fraction xDelta; 
	private Fraction yDelta; 
	private Fraction widthDelta; 
	private Fraction heightDelta;

	public ResizeCommand2(Fraction xDelta, Fraction yDelta, Fraction widthDelta, Fraction heightDelta) {
		this.xDelta = xDelta;
		this.yDelta = yDelta;
		this.widthDelta = widthDelta;
		this.heightDelta = heightDelta;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
		Model model = (Model)location.getChild(prevalentSystem);
		
		model.resize(xDelta, yDelta, widthDelta, heightDelta, propCtx, 0, collector);
		
		return new Output(xDelta, yDelta, widthDelta, heightDelta);
	}
}
