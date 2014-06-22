package dynamake;

import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public class RepaintRunBuilder implements RunBuilder {
	private JComponent componentToRepaint;
	
	public RepaintRunBuilder(JComponent componentToRepaint) {
		this.componentToRepaint = componentToRepaint;
		boundsToRepaint = new Rectangle();
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
					
					componentToRepaint.repaint(boundsToRepaint);
				}
			});
		}
	}
	
	private Rectangle boundsToRepaint;
	
	@Override
	public void addRunnable(Runnable runnable) {
//		runnables.add(runnable);
//		boundsToRepaint = componentToRepaint.getBounds();
		addRunnable(runnable, componentToRepaint.getBounds());
	}
	
	public void addRunnable(Runnable runnable, Rectangle bounds) {
		int minX = Math.min(boundsToRepaint.x, bounds.x);
		int minY = Math.min(boundsToRepaint.y, bounds.y);
		int maxRight = Math.max(boundsToRepaint.x + boundsToRepaint.width, bounds.x + bounds.width);
		int maxBottom = Math.max(boundsToRepaint.y + boundsToRepaint.height, bounds.y + bounds.height);
		
		boundsToRepaint = new Rectangle(
			minX, minY, maxRight, maxBottom
		);
		
		runnables.add(runnable);
	}
}
