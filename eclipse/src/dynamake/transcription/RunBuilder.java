package dynamake.transcription;

public interface RunBuilder {
	void addRunnable(Runnable runnable);
	void execute();
	void clear();
}
