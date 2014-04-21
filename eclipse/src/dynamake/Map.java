package dynamake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.util.Date;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.prevayler.Transaction;

public class Map extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static class SetProperty {
		public final String name;
		public final Object value;

		public SetProperty(String name, Object value) {
			this.name = name;
			this.value = value;
		}
	}
	
	public static class PropertyChanged {
		public final String name;
		public final Object value;

		public PropertyChanged(String name, Object value) {
			this.name = name;
			this.value = value;
		}
	}
	
	public static class SetPropertyTransaction implements Transaction<Map> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String name;
		private Object value;

		public SetPropertyTransaction(String name, Object value) {
			this.name = name;
			this.value = value;
		}
		
		@Override
		public void executeOn(Map prevalentSystem, Date executionTime) {
			prevalentSystem.set(name, value, new PropogationContext());
		}
	}
	
	private Hashtable<String, Object> map = new Hashtable<String, Object>();
	
	public void set(String name, Object value, PropogationContext propCtx) {
		map.put(name, value);
		sendChanged(new PropertyChanged(name, value), propCtx);
	}
	
	public Object get(String name) {
		return map.get(name);
	}
	
	@Override
	public void changed(Model sender, Object change, PropogationContext propCtx) {
		if(change instanceof SetProperty) {
			SetProperty setProperty = (SetProperty)change;
			set(setProperty.name, setProperty.value, propCtx);
		}
	}
	
	private static class MapView extends JPanel implements ModelComponent {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Map model;
		private TransactionFactory transactionFactory;

		public MapView(Map model, TransactionFactory transactionFactory) {
			this.model = model;
			this.transactionFactory = transactionFactory;
			
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
			setLayout(new BorderLayout());
			add(new JLabel("Map", JLabel.CENTER), BorderLayout.CENTER);
		}

		@Override
		public Model getModel() {
			return model;
		}

		@Override
		public void appendContainerTransactions(
				TransactionMapBuilder transactions, ModelComponent child) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void appendTransactions(TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public TransactionFactory getTransactionFactory() {
			return transactionFactory;
		}

		@Override
		public Transaction<Model> getDefaultDropTransaction(
				ModelComponent dropped, Point dropPoint) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void appendDroppedTransactions(TransactionMapBuilder transactions) {
			// TODO Auto-generated method stub
			
		}
	}

	@Override
	public Binding<ModelComponent> createView(ViewManager viewManager,
			TransactionFactory transactionFactory) {
		final MapView view = new MapView(this, transactionFactory);
		
		final RemovableListener removableListenerForBoundChanges = Model.wrapForBoundsChanges(this, view, viewManager);
		
		return new Binding<ModelComponent>() {
			@Override
			public void releaseBinding() {
				removableListenerForBoundChanges.releaseBinding();
			}
			
			@Override
			public ModelComponent getBindingTarget() {
				return view;
			}
		};
	}
}
