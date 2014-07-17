package dynamake.commands;

import java.io.Serializable;

public interface CommandFactory<T> extends Serializable {
	Command<T> createCommand(Object output);
}
