package dynamake.transcription;

import java.util.ArrayList;
import java.util.List;

import dynamake.commands.CommandState;
import dynamake.models.Model;
import dynamake.models.RestorableModel_TO_BE_OBSOLETED;
import dynamake.models.Model.PendingUndoablePair;
import dynamake.models.PropogationContext;

public class InheritedHistoryHandler implements HistoryHandler<Model> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void startLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		reference.setProperty("New" + RestorableModel_TO_BE_OBSOLETED.PROPERTY_CREATION, new ArrayList<CommandState<Model>>(), propCtx, propDistance, collector);
	}

	@Override
	public void logFor(Model reference, ArrayList<PendingUndoablePair> pendingUndoablePairs, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> newInherited = (List<CommandState<Model>>)reference.getProperty("New" + RestorableModel_TO_BE_OBSOLETED.PROPERTY_CREATION);
		for(PendingUndoablePair pendingUndoablePair: pendingUndoablePairs)
			newInherited.add(pendingUndoablePair.pending);
	}

	@Override
	public void commitLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> newInherited = (List<CommandState<Model>>)reference.getProperty("New" + RestorableModel_TO_BE_OBSOLETED.PROPERTY_CREATION);
		reference.setProperty("New" + RestorableModel_TO_BE_OBSOLETED.PROPERTY_CREATION, null, propCtx, propDistance, collector);
		
		@SuppressWarnings("unchecked")
		List<CommandState<Model>> inherited = (List<CommandState<Model>>)reference.getProperty(RestorableModel_TO_BE_OBSOLETED.PROPERTY_CREATION);
		if(inherited == null) {
			inherited = new ArrayList<CommandState<Model>>();
			reference.setProperty(RestorableModel_TO_BE_OBSOLETED.PROPERTY_CREATION, inherited, propCtx, propDistance, collector);
		}
		inherited.addAll(newInherited);
	}

	@Override
	public void rejectLogFor(Model reference, PropogationContext propCtx, int propDistance, Collector<Model> collector) {
		reference.setProperty("New" + RestorableModel_TO_BE_OBSOLETED.PROPERTY_CREATION, null, propCtx, propDistance, collector);
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof InheritedHistoryHandler;
	}
}
