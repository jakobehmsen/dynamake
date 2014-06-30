package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.UIManager;

import resources.ResourceManager;

import dynamake.delegates.Func0;
import dynamake.models.Binding;
import dynamake.models.CanvasModel;
import dynamake.models.LiveModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelRootLocator;
import dynamake.models.RootModel;
import dynamake.models.TextModel;
import dynamake.models.ModelTranscriber;
import dynamake.models.ViewManager;
import dynamake.models.factories.CanvasModelFactory;
import dynamake.models.factories.Factory;
import dynamake.models.factories.TextModelFactory;
import dynamake.tools.BindTool;
import dynamake.tools.ConsTool;
import dynamake.tools.DragTool;
import dynamake.tools.EditTool;
import dynamake.tools.PenTool;
import dynamake.tools.PlotTool;
import dynamake.tools.RedoTool;
import dynamake.tools.ScaleTool;
import dynamake.tools.TellTool;
import dynamake.tools.Tool;
import dynamake.tools.TrimTool;
import dynamake.tools.UndoTool;
import dynamake.tools.ViewTool;
import dynamake.transcription.SnapshottingTranscriber;
import dynamake.transcription.Transcriber;

public class Main {
	public static void main(String[] args) {
		try {
//			Fraction f = new Fraction(1, 9);
//			Fraction f2 = new Fraction(6, 1);
//			
//			System.out.println(f.add(f2));
//			
//			if(1 != 2) {
//				return;
//			}
			
			// Can be used for intercepting mouse events?
//			JFrame.setDefaultLookAndFeelDecorated(true);
			
			final Frame loadIndicator = new Frame();
			final JLabel loadIndicatorLabel = new JLabel("Loading Dynamake...", JLabel.CENTER);
			loadIndicatorLabel.setBorder(BorderFactory.createEtchedBorder());
			loadIndicator.add(loadIndicatorLabel, BorderLayout.CENTER);
			loadIndicator.setUndecorated(true);
			loadIndicator.setSize(320, 240);
			loadIndicator.setLocationRelativeTo(null);
			loadIndicator.addWindowListener(new WindowAdapter() {
				@Override
				public void windowOpened(WindowEvent arg0) {
					loadIndicatorLabel.repaint();
					loadResources();
				}
			});
			loadIndicator.setVisible(true);
			
//			loadResources();
			
			ArrayList<Factory> factoryBuilder = new ArrayList<Factory>();
			
//			factoryBuilder.add(new TextModelFactory());
			factoryBuilder.add(new CanvasModelFactory());
//			factoryBuilder.add(new ButtonModelFactory());
			factoryBuilder.add(new TextModelFactory());
			
//			for(Primitive.Implementation implementationSingleton: Primitive.getImplementationSingletons())
//				factoryBuilder.add(new PrimitiveSingletonFactory(implementationSingleton));
			
			final Factory[] factories = new Factory[factoryBuilder.size()];
			factoryBuilder.toArray(factories);
			
//			RootModel rootModel = new RootModel(new LiveModel(new CanvasModel()));
//			final Prevayler<Model> pModel = PrevaylerFactory.createPrevayler((Model)rootModel);
//			
//			final PrevaylerService<Model> prevaylerService = new SnapshottingPrevaylerService<Model>(pModel);
			final Transcriber<Model> transcriber = new SnapshottingTranscriber<Model>(new Func0<Model>() {
				@Override
				public Model call() {
					// TODO Auto-generated method stub
					RootModel rootModel = new RootModel(new LiveModel(new CanvasModel()));
					
//					rootModel.setLocation(new ModelRootLocation());
					
					return rootModel;
				}
			});
			ViewManager rootViewManager = new ViewManager() {
				@Override
				public Factory[] getFactories() {
					return factories;
				}
				
				private Tool[] tools = new Tool[] {
					new EditTool(),
					new PlotTool(),
					new BindTool(),
					new DragTool(),
					new ConsTool(),
					new TellTool(),
					new ViewTool(),
					new ScaleTool(),
//					new TextTool(),
//					new WriteTool(),
					new PenTool(),
					new TrimTool(),
					new UndoTool(),
					new RedoTool()
				}; 
				
				@Override
				public Tool[] getTools() {
					return tools;
				}
			};
			ModelTranscriber rootModelTranscriber = new ModelTranscriber(transcriber, new ModelRootLocator());
			
			UIManager.put("ToggleButton.select", Color.DARK_GRAY);
			
			final Binding<ModelComponent> rootView = transcriber.prevalentSystem().createView(null, rootViewManager, rootModelTranscriber);
			rootView.getBindingTarget().initialize();
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
						transcriber.close();
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
		ResourceManager.INSTANCE.setResourceAccessor("Set " + Model.PROPERTY_COLOR, new Callable<JColorChooser>() {
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
