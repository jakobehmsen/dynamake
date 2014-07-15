package dynamake.commands;

import java.io.Serializable;

public interface Command2Factory<T> extends Serializable {
	Command2<T> createCommand(Object output);
}
