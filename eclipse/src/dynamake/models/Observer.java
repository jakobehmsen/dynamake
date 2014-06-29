package dynamake.models;

import dynamake.TranscriberBranch;

// TODO: Consider renaming to Peer
public interface Observer {
	/**
	
	Instead of calling the change method, a createDualCommand (like DualCommandFactory) is called, which creates both
	the forward transaction and the backward transaction. The created dual command is forwarded to the PrevaylerService.
	 * @param branch TODO
	
	 */
	void changed(Model sender, Object change, PropogationContext propCtx, int propDistance, int changeDistance, TranscriberBranch<Model> branch);
	void addObservee(Observer observee);
	void removeObservee(Observer observee);
}