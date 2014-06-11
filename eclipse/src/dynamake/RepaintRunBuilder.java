package dynamake;

import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public class RepaintRunBuilder implements RunBuilder {
	private JComponent componentToRepaint;
	
	public RepaintRunBuilder(JComponent componentToRepaint) {
		this.componentToRepaint = componentToRepaint;
//		System.out.println("Created RepaintRunBuilder " + this);
	}

	private ArrayList<Runnable> runnables = new ArrayList<Runnable>();
	
	@Override
	public void execute() {
//		System.out.println("execute " + this);
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
//		System.out.println("add runnable to " + this);
		runnables.add(runnable);
	}
}
