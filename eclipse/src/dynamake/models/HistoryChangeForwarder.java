package dynamake.models;

import dynamake.transcription.Collector;

public class HistoryChangeForwarder extends ObserverAdapter {
	private Model inhereter;
	private Model inheretee;
	
	public HistoryChangeForwarder(Model inheretee) {
		this.inheretee = inheretee;
	}
	
	@Override
	public void addObservee(Observer observee) {
		this.inhereter = (Model)observee;
	}
	
	@Override
	public void removeObservee(Observer observee) {
		this.inhereter = null;
	}

	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
		if(change instanceof Model.HistoryAppendLogChange) {
			Model.HistoryAppendLogChange historyAppendLogChange = (Model.HistoryAppendLogChange)change;
			
//			inheretee.appendLog(historyAppendLogChange.change, propCtx, propDistance, collector);
		} else if(change instanceof Model.HistoryChange) {
			Model.HistoryChange historyChange = (Model.HistoryChange)change;
			
			switch(historyChange.type) {
			case Model.HistoryChange.TYPE_UNDO:
//				inheretee.undo(propCtx, propDistance, collector);
				break;
			case Model.HistoryChange.TYPE_REDO:
//				inheretee.redo(propCtx, propDistance, collector);
				break;
			}
		} else if(change instanceof Model.HistoryLogChange) {
			Model.HistoryLogChange historyLogChange = (Model.HistoryLogChange)change;
			
			switch(historyLogChange.type) {
			case Model.HistoryLogChange.TYPE_COMMIT_LOG:
//				inheretee.commitLog(historyLogChange.length, propCtx, propDistance, collector);
				break;
			case Model.HistoryLogChange.TYPE_REJECT_LOG:
//				inheretee.rejectLog(historyLogChange.length, propCtx, propDistance, collector);
				break;
			}
		}
	}
}
