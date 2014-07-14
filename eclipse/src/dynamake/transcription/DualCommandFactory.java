package dynamake.transcription;

import java.util.List;

import dynamake.commands.DualCommand;
import dynamake.models.Location;

/**
Instances of implementers each are able to build a transaction consisting of a sequence of dual commands
based on a supplied location. Such a location refers to a point which marks the center of the creation
of the transaction. More specifically, two transactions are requested to be built: 1) one for transcription
and 2) one for isolated model history. For 1), a location centered around the root of the persistent system.
For 2), a location centered around the told reference (see below).
Further, such instances each are able to tell which model is supposed to act as a reference. A reference
is used to maintain isolated model history. Lastly, the built transactions are to be transcribed.
*/
public interface DualCommandFactory<T> {
	T getReference();
	void createDualCommands(Location location, List<DualCommand<T>> dualCommands);
}
