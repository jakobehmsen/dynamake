package dynamake.transcription;

import java.util.List;

import dynamake.commands.DualCommand;
import dynamake.delegates.Holder;
import dynamake.models.Model;

public interface DualCommandFactory<T> {
//	DualCommand<T> createDualCommand();
	
	// TODO: Consider: Perhaps some sort of "transaction builder" or "transaction composer" should be supplied
	// for these calls, where such objects have special methods for adding dual command in more simple manners
	// from the client side.
	// E.g. an "AddModelToCanvas" transaction which not only takes the arguments to create the forward transaction: 
	// canvas location, creation bounds, factory
	// and takes the arguments to create the backward transaction
	// index ***THIS ARGUMENT MAY BE DERIVED***
	void createDualCommands(Holder<Model> referenceHolder, List<DualCommand<T>> dualCommands);
}
