package dynamake.transcription;

import java.util.List;

import dynamake.commands.DualCommand;
import dynamake.models.Location;

public interface DualCommandFactory2<T> {

	T getReference();
	
	void createDualCommands(Location location, List<DualCommand<T>> dualCommands);
}
