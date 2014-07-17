package dynamake.commands;

import java.util.Date;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class TellPropertyCommand2 implements Command2<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String propertyName;
	
	public TellPropertyCommand2(String propertyName) {
		this.propertyName = propertyName;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, Collector<Model> collector, Location location) {
		Model receiver = (Model)location.getChild(prevalentSystem);
		receiver.changed(null, new Model.TellProperty(propertyName), propCtx, 0, 1, collector);
		
		return null;
	}
}
