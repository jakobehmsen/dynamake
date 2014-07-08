package dynamake.models;

import dynamake.transcription.TranscriberCollector;

// TODO: Consider renaming to Peer
public interface Observer {
	/**
	
	Instead of calling the change method, a createDualCommand (like DualCommandFactory) is called, which creates both
	the forward transaction and the backward transaction. The created dual command is forwarded to the PrevaylerService.
	 * @param collector TODO
	
	 */
	void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, TranscriberCollector<Model> collector);
	void addObservee(Observer observee);
	void removeObservee(Observer observee);
}