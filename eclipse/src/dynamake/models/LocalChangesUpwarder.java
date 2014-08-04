package dynamake.models;

import java.io.Serializable;
import java.util.ArrayList;

import dynamake.transcription.Collector;

public class LocalChangesUpwarder extends ObserverAdapter implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private LocalChangesForwarder historyChangeForwarder;
	private Location offset;

	public LocalChangesUpwarder(LocalChangesForwarder historyChangeForwarder, Location offset) {
		this.historyChangeForwarder = historyChangeForwarder;
		this.offset = offset;
	}
	
	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		if(change instanceof Model.HistoryAppendLogChange) {
			Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
			
			ArrayList<Model.PendingUndoablePair> newPendingUndoablePairs = new ArrayList<Model.PendingUndoablePair>();
			for(Model.PendingUndoablePair pup: historyAppendLogChange.pendingUndoablePairs)
				newPendingUndoablePairs.add(pup.offset(offset));
			
			historyChangeForwarder.changed(sender, new Model.HistoryAppendLogChange(newPendingUndoablePairs), propCtx, propDistance, changeDistance, collector);
		}
	}
}
