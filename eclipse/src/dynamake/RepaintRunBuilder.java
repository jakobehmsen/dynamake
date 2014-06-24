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
			final ArrayList<Runnable> runnablesCopy = new ArrayList<Runnable>(runnables);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					for(Runnable runnable: runnablesCopy)
						runnable.run();
					
					componentToRepaint.repaint(boundsToRepaint);
//					System.out.println("boundsToRepaint=" + boundsToRepaint);
				}
			});
		}
	}
	
	private Rectangle boundsToRepaint;
	
	@Override
	public void addRunnable(Runnable runnable) {
//		runnables.add(runnable);
//		boundsToRepaint = componentToRepaint.getBounds();
		addRunnable(runnable, componentToRepaint, componentToRepaint.getBounds());
	}
	
	public void addRunnable(Runnable runnable, JComponent referenceComponent, Rectangle bounds) {
		int currentX = !boundsToRepaint.isEmpty() ? boundsToRepaint.x : Integer.MAX_VALUE;
		int currentY = !boundsToRepaint.isEmpty() ? boundsToRepaint.y : Integer.MAX_VALUE;
		int currentWidth = !boundsToRepaint.isEmpty() ? boundsToRepaint.width : Integer.MIN_VALUE;
		int currentHeight = !boundsToRepaint.isEmpty() ? boundsToRepaint.height : Integer.MIN_VALUE;
		
		bounds = SwingUtilities.convertRectangle(referenceComponent, bounds, componentToRepaint);
		int minX = Math.min(currentX, bounds.x);
		int minY = Math.min(currentY, bounds.y);
		int maxRight = Math.max(currentX + currentWidth, bounds.x + bounds.width);
		int maxBottom = Math.max(currentY + currentHeight, bounds.y + bounds.height);
		
		boundsToRepaint = new Rectangle(minX, minY, maxRight - minX, maxBottom - minY);
		
		runnables.add(runnable);
	}

	@Override
	public void clear() {
		runnables.clear();
	}
}
