package dynamake.tools;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JComponent;

import dynamake.commands.DualCommand;
import dynamake.commands.DualCommandPair;
import dynamake.models.CanvasModel;
import dynamake.models.Location;
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
			List<DualCommand<Model>> dualCommands, Location location, ModelComponent selection, Rectangle currentBounds, Rectangle newBounds) {
		Model selectionModel = selection.getModelBehind();
//		Rectangle currentBounds = ((JComponent)selection).getBounds();
		
		if(currentBounds.x != newBounds.x) {
			dualCommands.add(new DualCommandPair<Model>(
				new Model.SetPropertyTransaction(location, "X", new Fraction(newBounds.x)), 
				new Model.SetPropertyTransaction(location, "X", selectionModel.getProperty("X"))
			));
			
			if(selectionModel instanceof CanvasModel) {
				for(Component childComponent: ((JComponent)selection).getComponents()) {
					ModelComponent child = (ModelComponent)childComponent;
					int offset = currentBounds.x - newBounds.x;
					Number newX = ((Fraction)child.getModelBehind().getProperty("X")).add(new Fraction(offset));
					dualCommands.add(new DualCommandPair<Model>(
						new Model.SetPropertyTransaction(location, "X", newX), 
						new Model.SetPropertyTransaction(location, "X", selectionModel.getProperty("X"))
					));
				}
			}
		}

		if(currentBounds.y != newBounds.y) {
			dualCommands.add(new DualCommandPair<Model>(
				new Model.SetPropertyTransaction(location, "Y", new Fraction(newBounds.y)), 
				new Model.SetPropertyTransaction(location, "Y", selectionModel.getProperty("Y"))
			));
			
			if(selectionModel instanceof CanvasModel) {
				for(Component childComponent: ((JComponent)selection).getComponents()) {
					ModelComponent child = (ModelComponent)childComponent;
					int offset = currentBounds.y - newBounds.y;
					Number newY = ((Fraction)child.getModelBehind().getProperty("Y")).add(new Fraction(offset));
					dualCommands.add(new DualCommandPair<Model>(
						new Model.SetPropertyTransaction(location, "Y", newY), 
						new Model.SetPropertyTransaction(location, "Y", selectionModel.getProperty("Y"))
					));
				}
			}
		}

		if(currentBounds.width != newBounds.width) {
			dualCommands.add(new DualCommandPair<Model>(
				new Model.SetPropertyTransaction(location, "Width", new Fraction(newBounds.width)), 
				new Model.SetPropertyTransaction(location, "Width", selectionModel.getProperty("Width"))
			));
		}

		if(currentBounds.height != newBounds.height) {
			dualCommands.add(new DualCommandPair<Model>(
				new Model.SetPropertyTransaction(location, "Height", new Fraction(newBounds.height)), 
				new Model.SetPropertyTransaction(location, "Height", selectionModel.getProperty("Height"))
			));
		}
	}
}
