package dynamake.commands;

import dynamake.models.Location;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.transcription.Collector;

public class IfNoForwardersEnsureNotForwardLocalChangesUpwardsCommand implements Command<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IfNoForwardersEnsureNotForwardLocalChangesUpwardsCommand(Location<Model> locationOfSourceFromTarget) {

	}

	@Override
	public Object executeOn(PropogationContext propCtx, Model prevalentSystem, Collector<Model> collector, Location<Model> location, ExecutionScope<Model> scope) {
		return null;
	}
}
