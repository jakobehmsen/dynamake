package dynamake.commands;

import java.util.ArrayList;
import java.util.List;

import dynamake.models.CompositeLocation;
import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.RestorableModel;
import dynamake.transcription.Collector;
import dynamake.transcription.SimpleExPendingCommandFactory2;

public class PushForwardFromCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Location locationOfSourceFromTarget;
	private int forwardCount;
	
	public PushForwardFromCommand(Location locationOfSourceFromTarget) {
		this.locationOfSourceFromTarget = locationOfSourceFromTarget;
		this.forwardCount = 1;
	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location location) {
		Model target = (Model)location.getChild(prevalentSystem);
		Model source = (Model)locationOfSourceFromTarget.getChild(target);
		
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> creation = (List<CommandState<Model>>)source.getProperty(RestorableModel.PROPERTY_CREATION);
		
		if(creation != null) {
			ArrayList<CommandState<Model>> forwardedCreation = new ArrayList<CommandState<Model>>();
			for(CommandState<Model> creationPart: creation) {
				creationPart = creationPart.mapToReferenceLocation(source, target);
				for(int i = 0; i < forwardCount; i++)
					creationPart = creationPart.forForwarding();
				forwardedCreation.add(creationPart);
			}
			
			collector.execute(new SimpleExPendingCommandFactory2<Model>(target, forwardedCreation));
		}

		return null;
	}
}
