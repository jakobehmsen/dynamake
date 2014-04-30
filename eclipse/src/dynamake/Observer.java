package dynamake;

// TODO: Consider renaming to Peer
public interface Observer {
	void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance);
	void addObservee(Observer observee);
	void removeObservee(Observer observee);
}