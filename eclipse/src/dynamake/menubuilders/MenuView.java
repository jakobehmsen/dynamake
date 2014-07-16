package dynamake.menubuilders;

import dynamake.delegates.Action1;

public interface MenuView {
	void hide();
	void execute(Action1<ActionRunner> runnerUsage);
}
