package dynamake;

// TODO: Consider renaming to Peer
public interface Observer {
	/**
	
	Instead of calling the change method, a createDualCommand (like DualCommandFactory) is called, which creates both
	the forward transaction and the backward transaction. The created dual command is forwarded to the PrevaylerService.
	
	 */
	void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance);
	void addObservee(Observer observee);
	void removeObservee(Observer observee);
}