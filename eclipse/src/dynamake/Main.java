package dynamake;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
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
			// Can be used for intercepting mouse events?
//			JFrame.setDefaultLookAndFeelDecorated(true);
			
			final Frame loadIndicator = new Frame();
			final JLabel loadIndicatorLabel = new JLabel("Loading Dynamake...", JLabel.CENTER);
			loadIndicatorLabel.setBorder(BorderFactory.createEtchedBorder());
			loadIndicator.add(loadIndicatorLabel, BorderLayout.CENTER);
			loadIndicator.setUndecorated(true);
			loadIndicator.setSize(320, 240);
			loadIndicator.setLocationRelativeTo(null);
			loadIndicator.addWindowListener(new WindowListener() {
				@Override
				public void windowOpened(WindowEvent arg0) {
					loadIndicatorLabel.repaint();
					loadResources();
				}
				
				@Override
				public void windowIconified(WindowEvent arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void windowDeiconified(WindowEvent arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void windowDeactivated(WindowEvent arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void windowClosing(WindowEvent arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void windowClosed(WindowEvent arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void windowActivated(WindowEvent arg0) {
					// TODO Auto-generated method stub
					
				}
			});
			loadIndicator.setVisible(true);
			
//			loadResources();
			
			ArrayList<Factory> factoryBuilder = new ArrayList<Factory>();
			
			factoryBuilder.add(new TextModelFactory());
			factoryBuilder.add(new CanvasModelFactory());
			factoryBuilder.add(new ButtonModelFactory());
			factoryBuilder.add(new FloatingTextModelFactory());
			
			for(Primitive.Implementation implementationSingleton: Primitive.getImplementationSingletons())
				factoryBuilder.add(new PrimitiveSingletonFactory(implementationSingleton));
			
			final Factory[] factories = new Factory[factoryBuilder.size()];
			factoryBuilder.toArray(factories);
			
			RootModel rootModel = new RootModel(new LiveModel(new CanvasModel()));
			final Prevayler<Model> pModel = PrevaylerFactory.createPrevayler((Model)rootModel);
			
			final PrevaylerService<Model> prevaylerService = new SnapshottingPrevaylerService<Model>(pModel);
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
				
				@Override
				public void wasCreated(ModelComponent view) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public Tool[] getTools() {
					return new Tool[] {
						new EditTool(),
						new PlotTool(),
						new BindTool(),
						new DragTool(),
						new ConsTool(),
						new TellTool(),
						new ViewTool()
					};
				}

				@Override
				public void unFocus(ModelComponent view) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void becameVisible(ModelComponent view) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void becameInvisible(ModelComponent view) {
					// TODO Auto-generated method stub
					
				}
			};
			TransactionFactory rootTransactionFactory = new TransactionFactory(prevaylerService, new RootLocator());
			final Binding<ModelComponent> rootView = pModel.prevalentSystem().createView(rootViewManager, rootTransactionFactory);
			JFrame frame = (JFrame)rootView.getBindingTarget();
			
			// Can be used for intercepting mouse events?
//			frame.setUndecorated(true);
			
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
					e.getWindow().repaint();
				}
				
				@Override
				public void windowClosing(WindowEvent e) {
					try {
						rootView.releaseBinding();
						prevaylerService.close();
						ResourceManager.INSTANCE.dispose();
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

	private static void loadResources() {
		ResourceManager.INSTANCE.setResourceAccessor("Set " + Model.PROPERTY_BACKGROUND, new Callable<JColorChooser>() {
			@Override
			public JColorChooser call() throws Exception {
				return new JColorChooser();
			}
		});
		ResourceManager.INSTANCE.setResourceAccessor("Set " + Model.PROPERTY_FOREGROUND, new Callable<JColorChooser>() {
			@Override
			public JColorChooser call() throws Exception {
				return new JColorChooser();
			}
		});
		ResourceManager.INSTANCE.setResourceAccessor("Set " + TextModel.PROPERTY_CARET_COLOR, new Callable<JColorChooser>() {
			@Override
			public JColorChooser call() throws Exception {
				return new JColorChooser();
			}
		});
		final String fontFamily = Font.MONOSPACED;
//		final String fontFamily = "Consolas";
		ResourceManager.INSTANCE.setResourceAccessor("Primitive Font", new Callable<Font>() {
			@Override
			public Font call() throws Exception {
				Font font = new Font(fontFamily, Font.BOLD | Font.ITALIC, 12);
				font.toString();
				return font;
			}
		});
	}
}
