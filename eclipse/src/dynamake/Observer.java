package dynamake;

public interface Observer {
	void changed(Model sender, Object change, PropogationContext propCtx);
}