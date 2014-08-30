package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class UnwrapCommandFromScope implements Command<Model> {

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem,
			Collector<Model> collector, Location location, ExecutionScope scope) {
		// TODO Auto-generated method stub
		return null;
	}

}
