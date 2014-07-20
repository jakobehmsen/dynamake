package dynamake.commands;

import java.io.Serializable;
/**
 * Instances of implementors are supposed to be able to create commands based on an output made from the execution of a previous command.
 */
public interface CommandFactory<T> extends Serializable {
	Command<T> createCommand(Object output);
}
