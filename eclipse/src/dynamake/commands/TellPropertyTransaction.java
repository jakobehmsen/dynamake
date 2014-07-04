package dynamake.commands;

import java.util.Date;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.TranscriberBranch;
import dynamake.transcription.TranscriberCollector;

public class TellPropertyTransaction implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Location modelLocation;
	private String propertyName;
	
	public TellPropertyTransaction(Location modelLocation, String propertyName) {
		this.modelLocation = modelLocation;
		this.propertyName = propertyName;
	}

	@Override
	public void executeOn(PropogationContext propCtx, Model prevalentSystem, Date executionTime, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) {
		Model receiver = (Model)modelLocation.getChild(prevalentSystem);
		receiver.changed(null, new Model.TellProperty(propertyName), propCtx, 0, 1, branch);
	}
}
