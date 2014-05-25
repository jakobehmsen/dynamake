package dynamake;

import java.util.List;

public interface DualCommandFactory2<T> {
	void createForwardTransactions(List<Command<T>> forwardTransactions);
	void createBackwardTransactions(List<Command<T>> forwardTransactions);
}
