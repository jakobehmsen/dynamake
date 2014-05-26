package dynamake;

/*

Possible alternative names:
Session, Recorder, Mediator, Thread

*/
public interface PrevaylerServiceConnection<T> {
	/**
	 * Take something which has two methods:
	 * - One method, which creates zero or more forward transactions
	 * - Another method, which creates zero or more backward transactions (if requested - e.g., this won't be requested during undo/redo) 
	 * 
	 * 
	 * @param propCtx
	 * @param transactionFactory
	 */
	void execute2(PropogationContext propCtx, DualCommandFactory2<T> transactionFactory);
	void execute(PropogationContext propCtx, DualCommandFactory<T> transactionFactory);
	void commit(PropogationContext propCtx);
	void rollback(PropogationContext propCtx);
	
	/*
	
	Instead of directly invoking commit, then absorb is used to indicate committing a certain execution branch which then flows upwards in the branch.
	
	This is to replace commit. Likely, a reject method (or named the like) with equivalent behavior as to rollback is added.
	
	*/
	void absorb();
	// This method also?
	// void reject()
	
	/*
	
	Freezes this connection and create branchCount branches.
	
	*/
	PrevaylerServiceConnection<T>[] branch(int branchCount);
}
