package dynamake;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;

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
//			final Prevayler<Model> pModel = PrevaylerFactory.createPrevayler((Model)new LiveModel(new ContainerModel()));
			Factory[] factories = new Factory[]{new TextModelFactory(), new CanvasModelFactory()};
			final Prevayler<Model> pModel = PrevaylerFactory.createPrevayler((Model)new LiveModel(new CanvasModel(), factories));
			
			TransactionFactory rootTransactionFactory = new TransactionFactory(pModel, new RootLocator());
			JFrame frame = pModel.prevalentSystem().toFrame(null, rootTransactionFactory);
			
			frame.setTitle("Dynamake 0.0.1");
			
			frame.setSize(1280, 768);
			frame.setLocationRelativeTo(null);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					try {
						pModel.close();
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
