package dynamake.commands;

import java.io.Serializable;

public class ReversibleCommand<T> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public final Command<T> forth;
	public final Command<T> back;
	
	public ReversibleCommand(Command<T> forth, Command<T> back) {
		this.forth = forth;
		this.back = back;
	}
}
