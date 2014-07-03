package dynamake.tools;

import java.awt.Rectangle;
import java.util.List;

import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelTranscriber;
import dynamake.numbers.Fraction;

public class EditTool extends BoundsChangeTool {
	@Override
	public String getName() {
		return "Edit";
	}

	@Override
	protected void appendDualCommandsForSameCanvasBoundsChange(
			List<DualCommand<Model>> dualCommands, ModelComponent selection, Rectangle newBounds) {
		ModelTranscriber selectionModelTranscriber = selection.getModelTranscriber();
		Model selectionModel = selection.getModelBehind();
		
		dualCommands.add(new DualCommandPair<Model>(
			new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "X", new Fraction(newBounds.x)), 
			new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "X", selectionModel.getProperty("X"))
		));
		
		dualCommands.add(new DualCommandPair<Model>(
			new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Y", new Fraction(newBounds.y)), 
			new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Y", selectionModel.getProperty("Y"))
		));
		
		dualCommands.add(new DualCommandPair<Model>(
			new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Width", new Fraction(newBounds.width)), 
			new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Width", selectionModel.getProperty("Width"))
		));
		
		dualCommands.add(new DualCommandPair<Model>(
			new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Height", new Fraction(newBounds.height)), 
			new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Height", selectionModel.getProperty("Height"))
		));
	}
}
