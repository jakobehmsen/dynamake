package dynamake.models;

import java.io.Serializable;

import dynamake.transcription.Collector;

public class HistoryChangeUpwarder extends ObserverAdapter implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private HistoryChangeForwarder historyChangeForwarder;

	public HistoryChangeUpwarder(HistoryChangeForwarder historyChangeForwarder) {
		this.historyChangeForwarder = historyChangeForwarder;
	}
	
	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) {
//		if(!(change instanceof Model.HistoryAppendLogChange || change instanceof Model.HistoryChange/* || change instanceof Model.HistoryLogChange*/))
//			return;
		
		// Somehow, the location should be off set
		historyChangeForwarder.changed(sender, change, propCtx, propDistance, changeDistance, collector);
	}
}
