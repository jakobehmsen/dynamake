package dynamake.transcription;

import java.util.List;

public interface ExecutionsHandler<T> {
	void handleExecutions(List<Execution<T>> executions, Collector<T> collector);
}