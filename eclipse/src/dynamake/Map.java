//package dynamake;
//
//import java.awt.BorderLayout;
//import java.awt.Color;
//import java.awt.Point;
//import java.awt.Rectangle;
//import java.util.Date;
//import java.util.Hashtable;
//
//import javax.swing.BorderFactory;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//
//import org.prevayler.Transaction;
//
//public class Map extends Model {
//	/**
//	 * 
//	 */
//	private static final long serialVersionUID = 1L;
//
//	public static class SetProperty {
//		public final String name;
//		public final Object value;
//
//		public SetProperty(String name, Object value) {
//			this.name = name;
//			this.value = value;
//		}
//	}
//	
//	public static class PropertyChanged {
//		public final String name;
//		public final Object value;
//
//		public PropertyChanged(String name, Object value) {
//			this.name = name;
//			this.value = value;
//		}
//	}
//	
//	public static class SetPropertyTransaction implements Transaction<Map> {
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//		private String name;
//		private Object value;
//
//		public SetPropertyTransaction(String name, Object value) {
//			this.name = name;
//			this.value = value;
//		}
//		
//		@Override
//		public void executeOn(Map prevalentSystem, Date executionTime) {
//			prevalentSystem.set(name, value, new PropogationContext(), 0);
//		}
//	}
//	
//	private Hashtable<String, Object> map = new Hashtable<String, Object>();
//	
//	public void set(String name, Object value, PropogationContext propCtx, int propDistance) {
//		map.put(name, value);
//		sendChanged(new PropertyChanged(name, value), propCtx, propDistance);
//	}
//	
//	public Object get(String name) {
//		return map.get(name);
//	}
//	
//	@Override
//	public void changed(Model sender, Object change, PropogationContext propCtx, int propDistance) {
//		if(change instanceof SetProperty) {
//			SetProperty setProperty = (SetProperty)change;
//			set(setProperty.name, setProperty.value, propCtx, propDistance);
//		}
//	}
//	
//	private static class MapView extends JPanel implements ModelComponent {
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//		private Map model;
//		private TransactionFactory transactionFactory;
//
//		public MapView(Map model, TransactionFactory transactionFactory) {
//			this.model = model;
//			this.transactionFactory = transactionFactory;
//			
//			setBorder(BorderFactory.createLineBorder(Color.BLACK));
//			setLayout(new BorderLayout());
//			add(new JLabel("Map", JLabel.CENTER), BorderLayout.CENTER);
//		}
//
//		@Override
//		public Model getModel() {
//			return model;
//		}
//
//		@Override
//		public TransactionFactory getTransactionFactory() {
//			return transactionFactory;
//		}
//		
//		@Override
//		public TransactionPublisher getObjectTransactionPublisher() {
//			return new TransactionPublisher() {
//				@Override
//				public void appendContainerTransactions(
//						TransactionMapBuilder transactions, ModelComponent child) {
//					// TODO Auto-generated method stub
//					
//				}
//
//				@Override
//				public void appendTransactions(TransactionMapBuilder transactions) {
//					Model.appendComponentPropertyChangeTransactions(model, transactionFactory, transactions);
//				}
//
//				@Override
//				public void appendDroppedTransactions(TransactionMapBuilder transactions) {
//					// TODO Auto-generated method stub
//					
//				}
//
//				@Override
//				public void appendDropTargetTransactions(ModelComponent dropped,
//						Rectangle droppedBounds, Point dropPoint, TransactionMapBuilder transactions) {
//					// TODO Auto-generated method stub
//					
//				}
//			};
//		}
//
//		@Override
//		public Transaction<Model> getImplicitDropAction(ModelComponent target) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//	}
//
//	@Override
//	public Binding<ModelComponent> createView(final ViewManager viewManager,
//			TransactionFactory transactionFactory) {
//		final MapView view = new MapView(this, transactionFactory);
//		
//		final Binding<Model> removableListener = RemovableListener.addAll(this, 
//			bindProperty(this, "Background", new Action1<Color>() {
//				public void run(Color value) {
//					view.setBackground(value);
//					viewManager.refresh(view);
//				}
//			}),
//			bindProperty(this, "Foreground", new Action1<Color>() {
//				public void run(Color value) {
//					view.setForeground(value);
//					viewManager.refresh(view);
//				}
//			})
//		);
//		
//		final RemovableListener removableListenerForBoundChanges = Model.wrapForBoundsChanges(this, view, viewManager);
//		
//		return new Binding<ModelComponent>() {
//			@Override
//			public void releaseBinding() {
//				removableListenerForBoundChanges.releaseBinding();
//				
//				removableListener.releaseBinding();
//			}
//			
//			@Override
//			public ModelComponent getBindingTarget() {
//				return view;
//			}
//		};
//	}
//}
