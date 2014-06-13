package dynamake;

import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public class RepaintRunBuilder implements RunBuilder {
	private JComponent componentToRepaint;
	
	public RepaintRunBuilder(JComponent componentToRepaint) {
		this.componentToRepaint = componentToRepaint;
	}

	private ArrayList<Runnable> runnables = new ArrayList<Runnable>();
	
	@Override
	public void execute() {
		if(runnables.size() > 0) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					for(Runnable runnable: runnables)
						runnable.run();
					
					componentToRepaint.repaint();
				}
			});
		}
	}
	
	@Override
	public void addRunnable(Runnable runnable) {
		runnables.add(runnable);
	}
}
