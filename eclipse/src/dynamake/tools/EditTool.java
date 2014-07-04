package dynamake.tools;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JComponent;

import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.models.CanvasModel;
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
	protected void appendDualCommandsForResize(
			List<DualCommand<Model>> dualCommands, ModelComponent selection, Rectangle newBounds) {
		ModelTranscriber selectionModelTranscriber = selection.getModelTranscriber();
		Model selectionModel = selection.getModelBehind();
		Rectangle currentBounds = ((JComponent)selection).getBounds();
		
		if(currentBounds.x != newBounds.x) {
			dualCommands.add(new DualCommandPair<Model>(
				new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "X", new Fraction(newBounds.x)), 
				new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "X", selectionModel.getProperty("X"))
			));
			
			if(currentBounds.width != newBounds.width) {
				if(selectionModel instanceof CanvasModel) {
					for(Component childComponent: ((JComponent)selection).getComponents()) {
						ModelComponent child = (ModelComponent)childComponent;
						int offset = currentBounds.x - newBounds.x;
						Number newX = ((Fraction)child.getModelBehind().getProperty("X")).add(new Fraction(offset));
						dualCommands.add(new DualCommandPair<Model>(
							new Model.SetPropertyTransaction(child.getModelTranscriber().getModelLocation(), "X", newX), 
							new Model.SetPropertyTransaction(child.getModelTranscriber().getModelLocation(), "X", selectionModel.getProperty("X"))
						));
					}
				}
			}
		}

		if(currentBounds.y != newBounds.y) {
			dualCommands.add(new DualCommandPair<Model>(
				new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Y", new Fraction(newBounds.y)), 
				new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Y", selectionModel.getProperty("Y"))
			));

			
			if(currentBounds.height != newBounds.height) {
				if(selectionModel instanceof CanvasModel) {
					for(Component childComponent: ((JComponent)selection).getComponents()) {
						ModelComponent child = (ModelComponent)childComponent;
						int offset = currentBounds.y - newBounds.y;
						Number newY = ((Fraction)child.getModelBehind().getProperty("Y")).add(new Fraction(offset));
						dualCommands.add(new DualCommandPair<Model>(
							new Model.SetPropertyTransaction(child.getModelTranscriber().getModelLocation(), "Y", newY), 
							new Model.SetPropertyTransaction(child.getModelTranscriber().getModelLocation(), "Y", selectionModel.getProperty("Y"))
						));
					}
				}
			}
		}

		if(currentBounds.width != newBounds.width) {
			dualCommands.add(new DualCommandPair<Model>(
				new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Width", new Fraction(newBounds.width)), 
				new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Width", selectionModel.getProperty("Width"))
			));
		}

		if(currentBounds.height != newBounds.height) {
			dualCommands.add(new DualCommandPair<Model>(
				new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Height", new Fraction(newBounds.height)), 
				new Model.SetPropertyTransaction(selectionModelTranscriber.getModelLocation(), "Height", selectionModel.getProperty("Height"))
			));
		}
	}
}
