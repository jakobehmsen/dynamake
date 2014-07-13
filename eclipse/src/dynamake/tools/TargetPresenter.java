package dynamake.tools;

import java.awt.Color;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import dynamake.delegates.Runner;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.transcription.Collector;

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
	
	public void update(ModelComponent newTargetOver, final Collector<Model> collector) {
		update(newTargetOver, new Runner() {
			@Override
			public void run(Runnable runnable) {
				collector.afterNextTrigger(runnable);
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
//						System.out.println("Removed targetFrame");
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
//						System.out.println("Added targetFrame");
					}
				});
			}
		}
	}
	
	public void reset(final Collector<Model> collector) {
		reset(new Runner() {
			@Override
			public void run(Runnable runnable) {
				collector.afterNextTrigger(runnable);
			}
		});
	}

	public void reset(Runner runner) {
		if(this.targetFrame != null) {
			final JComponent container = this.container;
			final JPanel targetFrame = this.targetFrame;
			runner.run(new Runnable() {
				@Override
				public void run() {
					container.remove(targetFrame);
//					System.out.println("Removed targetFrame");
				}
			});
		}
	}

	public ModelComponent getTargetOver() {
		return currentTargetOver;
	}
}
