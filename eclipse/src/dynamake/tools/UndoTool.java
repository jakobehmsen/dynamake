package dynamake.tools;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.List;

import dynamake.Command;
import dynamake.DualCommand;
import dynamake.DualCommandFactory;
import dynamake.DualCommandPair;
import dynamake.RepaintRunBuilder;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.PropogationContext;
import dynamake.models.LiveModel.ProductionPanel;
import dynamake.transcription.TranscriberBranch;

public class UndoTool implements Tool {
@Override
	public String getName() {
		return "Undo";
	}

	@Override
	public void mouseMoved(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) { }

	@Override
	public void mouseExited(ProductionPanel productionPanel, MouseEvent e) { }

	@Override
	public void mouseReleased(ProductionPanel productionPanel, MouseEvent e, final ModelComponent modelOver) {
//		productionPanel.livePanel.undo();
		
		TranscriberBranch<Model> branch = modelOver.getTransactionFactory().createBranch();
		branch.setOnFinishedBuilder(new RepaintRunBuilder(productionPanel.livePanel));
		branch.execute(new PropogationContext(), new DualCommandFactory<Model>() {
			@Override
			public void createDualCommands(List<DualCommand<Model>> dualCommands) {
				dualCommands.add(
					new DualCommandPair<Model>(
						new Model.UndoTransaction(modelOver.getTransactionFactory().getModelLocation()),
						new Command.Null<Model>()
					)
				);
			}
		});
		
		branch.close();
	}

	@Override
	public void mousePressed(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) { }

	@Override
	public void mouseDragged(ProductionPanel productionPanel, MouseEvent e, ModelComponent modelOver) { }

	@Override
	public void paint(Graphics g) {
		// TODO Auto-generated method stub
		
	}
}
