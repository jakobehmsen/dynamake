package dynamake.commands;

import java.util.Date;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.numbers.Fraction;
import dynamake.transcription.Collector;

public class ResizeCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location location;
	private Fraction xDelta; 
	private Fraction yDelta; 
	private Fraction widthDelta; 
	private Fraction heightDelta;

	public ResizeCommand(Location location, Fraction xDelta, Fraction yDelta, Fraction widthDelta, Fraction heightDelta) {
		this.location = location;
		this.xDelta = xDelta;
		this.yDelta = yDelta;
		this.widthDelta = widthDelta;
		this.heightDelta = heightDelta;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector) {
		Model model = (Model)location.getChild(prevalentSystem);
		model.resize(xDelta, yDelta, widthDelta, heightDelta, propCtx, 0, collector);
	}
}
