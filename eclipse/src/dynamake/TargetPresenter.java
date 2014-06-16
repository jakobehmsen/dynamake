package dynamake;

import java.awt.Color;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class TargetPresenter {
	public interface Behavior {
		boolean acceptsTarget(ModelComponent target);
		Color getColorForTarget(ModelComponent target);
	}
	
	private JComponent container;
	private Behavior behavior;
	private ModelComponent currentTargetOver;
	private JPanel targetFrame;
	
	public TargetPresenter(JComponent container, Behavior behavior) {
		this.container = container;
		this.behavior = behavior;
	}
	
	public void update(ModelComponent newTargetOver, final RunBuilder runBuilder) {
		update(newTargetOver, new Runner() {
			@Override
			public void run(Runnable runnable) {
				runBuilder.addRunnable(runnable);
			}
		});
	}
	
	public void update(ModelComponent newTargetOver, final PrevaylerServiceBranch<Model> branch) {
		update(newTargetOver, new Runner() {
			@Override
			public void run(Runnable runnable) {
				branch.onFinished(runnable);
			}
		});
	}
	
	public void update(ModelComponent newTargetOver, Runner runner) {
		if(newTargetOver != currentTargetOver) {
			currentTargetOver = newTargetOver;
			if(targetFrame != null) {
				final JPanel oldTargetFrame = targetFrame;
				runner.run(new Runnable() {
					@Override
					public void run() {
						container.remove(oldTargetFrame);
					}
				});
			}
			
			if(newTargetOver != null && behavior.acceptsTarget(newTargetOver)) {
				targetFrame = new JPanel();
				final Color color = behavior.getColorForTarget(newTargetOver);
				targetFrame.setBorder(
					BorderFactory.createCompoundBorder(
						BorderFactory.createLineBorder(Color.BLACK, 1), 
						BorderFactory.createCompoundBorder(
							BorderFactory.createLineBorder(color, 3), 
							BorderFactory.createLineBorder(Color.BLACK, 1)
						)
					)
				);
				
				Rectangle targetFrameBounds = SwingUtilities.convertRectangle(
					((JComponent)newTargetOver).getParent(), ((JComponent)newTargetOver).getBounds(), container);
				targetFrame.setBounds(targetFrameBounds);
				targetFrame.setBackground(new Color(0, 0, 0, 0));

				final JPanel localTargetFrame = targetFrame;
				runner.run(new Runnable() {
					@Override
					public void run() {
						container.add(localTargetFrame);
					}
				});
			}
		}
	}

	public void reset(final PrevaylerServiceBranch<Model> branch) {
		reset(new Runner() {
			@Override
			public void run(Runnable runnable) {
				branch.onFinished(runnable);
			}
		});
	}

	public void reset(Runner runner) {
		if(this.targetFrame != null) {
			final JPanel targetFrame = this.targetFrame;
			runner.run(new Runnable() {
				@Override
				public void run() {
					container.remove(targetFrame);
				}
			});
		}
	}

	public ModelComponent getTargetOver() {
		return currentTargetOver;
	}
}