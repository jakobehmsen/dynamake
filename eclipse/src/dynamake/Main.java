package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.UIManager;


import dynamake.delegates.Func0;
import dynamake.models.Binding;
import dynamake.models.CanvasModel;
import dynamake.models.LiveModel;
import dynamake.models.Model;
import dynamake.models.ModelComponent;
import dynamake.models.ModelRootLocator;
import dynamake.models.RootModel;
import dynamake.models.ModelTranscriber;
import dynamake.models.ViewManager;
import dynamake.resources.ResourceManager;
import dynamake.tools.BindTool;
import dynamake.tools.ConsTool;
import dynamake.tools.DragTool;
import dynamake.tools.EditTool;
import dynamake.tools.PenTool;
import dynamake.tools.PlotTool;
import dynamake.tools.RedoTool;
import dynamake.tools.ScaleTool;
import dynamake.tools.TellTool;
import dynamake.tools.ToolFactory;
import dynamake.tools.TrimTool;
import dynamake.tools.UndoTool;
import dynamake.tools.ViewTool;
import dynamake.tools.UnwrapTool;
import dynamake.tools.ReflectToolFactory;
import dynamake.transcription.SnapshottingTranscriber;
import dynamake.transcription.Transcriber;

public class Main {
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
			loadIndicator.addWindowListener(new WindowAdapter() {
				@Override
				public void windowOpened(WindowEvent arg0) {
					loadIndicatorLabel.repaint();
					loadResources();
				}
			});
			loadIndicator.setVisible(true);
			
			final Transcriber<Model> transcriber = new SnapshottingTranscriber<Model>(new Func0<Model>() {
				@Override
				public Model call() {
					RootModel rootModel = new RootModel(new LiveModel(new CanvasModel()));
					
					return rootModel;
				}
			});
			ViewManager rootViewManager = new ViewManager() {
				private ToolFactory[] toolFactories = new ToolFactory[] {
					new ReflectToolFactory<EditTool>("Edit", EditTool.class),
					new ReflectToolFactory<PlotTool>("Plot", PlotTool.class),
					new ReflectToolFactory<UnwrapTool>("Unwrap", UnwrapTool.class),
					new ReflectToolFactory<BindTool>("Bind", BindTool.class),
					new ReflectToolFactory<DragTool>("Drag", DragTool.class),
					new ReflectToolFactory<ConsTool>("Cons", ConsTool.class),
					new ReflectToolFactory<TellTool>("Tell", TellTool.class),
					new ReflectToolFactory<ViewTool>("View", ViewTool.class),
					new ReflectToolFactory<ScaleTool>("Scale", ScaleTool.class),
					new ReflectToolFactory<PenTool>("Pen", PenTool.class),
					new ReflectToolFactory<TrimTool>("Trim", TrimTool.class),
					new ReflectToolFactory<UndoTool>("Undo", UndoTool.class),
					new ReflectToolFactory<RedoTool>("Redo", RedoTool.class)
				};
				
				@Override
				public ToolFactory[] getToolFactories() {
					return toolFactories;
				}
			};
			ModelTranscriber rootModelTranscriber = new ModelTranscriber(transcriber, new ModelRootLocator());
			
			UIManager.put("ToggleButton.select", Color.DARK_GRAY);
			
			final Binding<ModelComponent> rootView = transcriber.prevalentSystem().createView(null, rootViewManager, rootModelTranscriber);
			rootView.getBindingTarget().initialize();
			JFrame frame = (JFrame)rootView.getBindingTarget();
			
			// Can be used for intercepting mouse events?
//			frame.setUndecorated(true);
			
			frame.setTitle("Dynamake");
			
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
