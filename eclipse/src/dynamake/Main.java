package dynamake;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;

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
//			final JLabel loadIndicator = new JLabel("Loading Dynamake...");
//			loadIndicator.setBorder(BorderFactory.createEtchedBorder());
//			final JFrame loadIndicator = new JFrame();
			final Frame loadIndicator = new Frame();
//			loadIndicator.getContentPane().setLayout(new BorderLayout());
//			((JComponent)loadIndicator.getContentPane()).setBorder(BorderFactory.createEtchedBorder());
//			loadIndicator.getContentPane().add(new JLabel("Loading Dynamake...", JLabel.CENTER), BorderLayout.CENTER);
			final JLabel loadIndicatorLabel = new JLabel("Loading Dynamake...", JLabel.CENTER);
			loadIndicatorLabel.setBorder(BorderFactory.createEtchedBorder());
			loadIndicator.add(loadIndicatorLabel, BorderLayout.CENTER);
			loadIndicator.setUndecorated(true);
			loadIndicator.setSize(320, 240);
			loadIndicator.setLocationRelativeTo(null);
			loadIndicator.setVisible(true);
			
			ArrayList<Factory> factoryBuilder = new ArrayList<Factory>();
			
			factoryBuilder.add(new TextModelFactory());
			factoryBuilder.add(new CanvasModelFactory());
			
			for(Primitive.Implementation implementationSingleton: Primitive.getImplementationSingletons())
				factoryBuilder.add(new PrimitiveSingletonFactory(implementationSingleton));
			
			final Factory[] factories = new Factory[factoryBuilder.size()];
			factoryBuilder.toArray(factories);
			
//			final Factory[] factories = new Factory[]{
//				new TextModelFactory(), 
//				new CanvasModelFactory(), 
////				new BGBindingCreationFactory(),
//				new BackgroundGetterFactory(),
//				new BackgroundSetterFactory(),
//				new ColorDarknerFactory(),
//				new ColorBrigthnerFactory()
//			};
			RootModel rootModel = new RootModel(new LiveModel(new CanvasModel()));
//			long loadStart = System.nanoTime();
			final Prevayler<Model> pModel = PrevaylerFactory.createPrevayler((Model)rootModel);
//			long loadEnd = System.nanoTime();
//			System.out.println("Load time: " + (loadEnd - loadStart) / 1000000.0 + " millis.");veTo(null);
			
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
				public void repaint(JComponent view) {
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
					loadIndicator.setVisible(false);
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
