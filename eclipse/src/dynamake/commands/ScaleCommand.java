package dynamake.commands;

import java.util.Date;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.numbers.Fraction;
import dynamake.transcription.Collector;

public class ScaleCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location location;
	private Fraction xDelta; 
	private Fraction yDelta; 
	private Fraction hChange; 
	private Fraction vChange;

	public ScaleCommand(Location location, Fraction xDelta, Fraction yDelta, Fraction hChange, Fraction vChange) {
		this.location = location;
		this.xDelta = xDelta;
		this.yDelta = yDelta;
		this.hChange = hChange;
		this.vChange = vChange;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector) {
		Model model = (Model)location.getChild(prevalentSystem);
		model.scale(xDelta, yDelta, hChange, vChange, propCtx, 0, collector);
	}
}
