package dynamake.models;

import dynamake.transcription.TranscriberBranch;
import dynamake.transcription.TranscriberCollector;

public class ObserverAdapter implements Observer {
	@Override
	public void changed(Model sender, Object change,
		PropogationContext propCtx, int propDistance, int changeDistance, TranscriberBranch<Model> branch, TranscriberCollector<Model> collector) { }

	@Override
	public void addObservee(Observer observee) { }

	@Override
	public void removeObservee(Observer observee) { }
}
