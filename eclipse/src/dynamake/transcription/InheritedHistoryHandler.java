package dynamake.transcription;

import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.models.Model;
import dynamake.models.Model.PendingUndoablePair;
import dynamake.models.PropogationContext;

public class InheritedHistoryHandler implements HistoryHandler<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void startLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		reference.setProperty("NewInherited", new ArrayList<CommandState<Model>>(), propCtx, propDistance, collector);
	}

	@Override
	public void logFor(Model reference, ArrayList<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> newInherited = (List<CommandState<Model>>)reference.getProperty("NewInherited");
		for(PendingUndoablePair pendingUndoablePair: pendingUndoablePairs)
			newInherited.add(pendingUndoablePair.pending);
	}

	@Override
	public void commitLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> newInherited = (List<CommandState<Model>>)reference.getProperty("NewInherited");
		reference.setProperty("NewInherited", null, propCtx, propDistance, collector);
		
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> inherited = (List<CommandState<Model>>)reference.getProperty("Inhereted");
		if(inherited == null) {
			inherited = new ArrayList<CommandState<Model>>();
			reference.setProperty("Inhereted", inherited, propCtx, propDistance, collector);
		}
		inherited.addAll(newInherited);
	}

	@Override
	public void rejectLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		reference.setProperty("NewInherited", null, propCtx, propDistance, collector);
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof InheritedHistoryHandler;
	}
}
