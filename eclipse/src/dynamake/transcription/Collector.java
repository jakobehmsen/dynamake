package dynamake.transcription;

/**
 * Instances of implementors provided methods to indicate and manage different kinds of side effects.
 */
public interface Collector<T> {
	/*
	Somehow, the collector interface should be extended such that it is possible to indicate the start of a transaction
	where this start of the transaction must be accompanied by a history handler and a command scope. The history handler
	is then used for the subsequent logging of command executions - and the scope is supplied for the commands to be executed.
	Once a transaction is committed, the supplied scope is somehow given to the history handler, when it is requested to
	commit its collected commands. The scope seems to have a close relation to the history handler - maybe a history handler
	should be aware of the scope itself and even be able to yield the scope outwards.
	
	What about embedded transaction? 
	Intuitively, they should not be committed before they parent transactions are committed.
	Further, they should implicitly reject upwards throughout the transaction hierarchy.
	
	*/
	
	void startTransaction(T reference, Object transactionHandlerClass);
	Object createProduceCommand(Object value);
	Object createConsumeCommand();
	void execute(Object command);
	void commitTransaction();
	void rejectTransaction();
	void afterNextTrigger(Runnable runnable);
	void flushNextTrigger();
}
