package dynamake;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTextPane;

import org.prevayler.Prevayler;
import org.prevayler.PrevaylerFactory;

public class Main {
	private static class RootLocator implements Locator {
		@Override
		public Location locate() {
			return new RootLocation();
		}
	}
	
	private static class RootLocation implements Location {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void setChild(Object holder, Object child) { }
		
		@Override
		public Object getChild(Object holder) {
			return holder;
		}
	}
	
	public static void main(String[] args) {
		try {
			final Factory[] factories = new Factory[]{new TextModelFactory(), new CanvasModelFactory(), new BGBindingCreationFactory()};
			RootModel rootModel = new RootModel(new LiveModel(new CanvasModel()));
			final Prevayler<Model> pModel = PrevaylerFactory.createPrevayler((Model)rootModel);
			
			final PrevaylerService<Model> prevaylerService = new SnapshottingPrevaylerService<Model>(pModel);
			TransactionFactory rootTransactionFactory = new TransactionFactory(prevaylerService, new RootLocator());
//			JFrame frame = pModel.prevalentSystem().toFrame(null, rootTransactionFactory);
			ViewManager rootViewManager = new ViewManager() {
				@Override
				public void setFocus(JComponent component) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void selectAndActive(ModelComponent view, int x, int y) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void repaint(JTextPane view) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public int getState() {
					// TODO Auto-generated method stub
					return 0;
				}
				
				@Override
				public Factory[] getFactories() {
					return factories;
				}
				
				@Override
				public void clearFocus() {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void refresh(ModelComponent view) {
					// TODO Auto-generated method stub
					
				}
			};
			final Binding<ModelComponent> rootView = pModel.prevalentSystem().createView(rootViewManager, rootTransactionFactory);
			JFrame frame = (JFrame)rootView.getBindingTarget();
			
//			final Thread snapshotService = new Thread(new Runnable() {
//				@Override
//				public void run() {
//					try {
//						while(true) {
//							Thread.sleep(1000);
//							try {
//								pModel.takeSnapshot();
//							} catch (Exception e) {
//								e.printStackTrace();
//							}
//						}
//					} catch (InterruptedException e) {
//					}
//				}
//			});
			
			frame.setTitle("Dynamake 0.0.1");
			
			if(frame.getBounds().isEmpty()) {
				frame.setSize(1280, 768);
				frame.setLocationRelativeTo(null);
			}
			
			// Ensure bounds are appropriate for the current screen resolution
			// If not, then resize and reposition the frame, such that it is fully contained within the screen resolution
			
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowOpened(WindowEvent e) {
//					snapshotService.start();
				}
				
				@Override
				public void windowClosing(WindowEvent e) {
//					snapshotService.interrupt();
					
					try {
						rootView.releaseBinding();
//						pModel.close();
						prevaylerService.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});
			
			frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
