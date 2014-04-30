package dynamake;

public class ObserverAdapter implements Observer {
	@Override
	public void changed(Model sender, Object change,
		PropogationContext propCtx, int propDistance, int changeDistance) { }

	@Override
	public void addObservee(Observer observee) { }

	@Override
	public void removeObservee(Observer observee) { }
}
