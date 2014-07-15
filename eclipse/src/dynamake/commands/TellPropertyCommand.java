package dynamake.commands;

import java.util.Date;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class TellPropertyCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location modelLocation;
	private String propertyName;
	
	public TellPropertyCommand(Location modelLocation, String propertyName) {
		this.modelLocation = modelLocation;
		this.propertyName = propertyName;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector) {
		Model receiver = (Model)modelLocation.getChild(prevalentSystem);
		receiver.changed(null, new Model.TellProperty(propertyName), propCtx, 0, 1, collector);
	}
}