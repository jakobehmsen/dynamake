package dynamake.models;

import dynamake.transcription.Collector;

public class ObserverAdapter implements Observer {
	@Override
	public void changed(Model sender, Object change,
		PropogationContext propCtx, int propDistance, int changeDistance, Collector<Model> collector) { }

	@Override
	public void addObservee(Observer observee) { }

	@Override
	public void removeObservee(Observer observee) { }
}
