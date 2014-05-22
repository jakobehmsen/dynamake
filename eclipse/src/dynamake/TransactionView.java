package dynamake;

public interface TransactionView {
	void hide();

	void execute(Runnable action);
}
