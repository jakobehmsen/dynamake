package dynamake.models;

import java.io.Serializable;
import java.util.ArrayList;

import dynamake.transcription.Collector;

public class LocalChangesUpwarder extends ObserverAdapter implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location sourceLocation;
	private Location offsetFromSource;

	public LocalChangesUpwarder(Location sourceLocation, Location offsetFromSource) {
		this.sourceLocation = sourceLocation;
		this.offsetFromSource = offsetFromSource;
	}
	
	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		if(change instanceof Model.HistoryAppendLogChange) {
			Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
			Model source = (Model)sourceLocation.getChild(sender);
			
			ArrayList<Model.PendingUndoablePair> newPendingUndoablePairs = new ArrayList<Model.PendingUndoablePair>();
			for(Model.PendingUndoablePair pup: historyAppendLogChange.pendingUndoablePairs) {
				// Be sensitive to undo/redo commands here; they should be handled differently
				// Somehow, it is the undone/redone command that should be offset instead
				newPendingUndoablePairs.add(pup.offset(offsetFromSource));
			}
			
			source.sendChanged(new Model.HistoryAppendLogChange(newPendingUndoablePairs), propCtx, propDistance, changeDistance, collector);
		}
	}
}
