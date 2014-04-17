package dynamake;

import java.util.Date;
import java.util.Hashtable;

import org.prevayler.Prevayler;
import org.prevayler.Transaction;
import org.prevayler.TransactionWithQuery;

public class Root {
	private Model rootModel;
	
	public Root() {
		
	}
	
	private Hashtable<Integer, Object> idToObjMap = new Hashtable<Integer, Object>();
	
	public Object get(int id) {
		return idToObjMap.get(id);
	}
	
	public void add(int id, Object obj) {
		idToObjMap.put(id, obj);
	}
	
	public void remove(int id) {
		idToObjMap.remove(id);
	}
	
	public static class Add<T> implements TransactionWithQuery<Root, T> {
		private int id;
		private Factory factory;
		
		public Add(int id, Factory factory) {
			this.factory = factory;
		}
		
		
		
//		@Override
//		public void executeOn(Root prevalentSystem, Date executionTime) {
//			Object obj = factory.create();
//			prevalentSystem.add(id, obj);
//		}



		@Override
		public T executeAndQuery(Root prevalentSystem, Date executionTime) throws Exception {
			Object obj = factory.create(new Hashtable<String, Object>());
			prevalentSystem.add(id, obj);
			return (T)obj;
		}
	}
	
	public static class Remove implements Transaction<Root> {
		private int id;
		
		public Remove(int id) {
			this.id = id;
		}
		
		@Override
		public void executeOn(Root prevalentSystem, Date executionTime) {
			prevalentSystem.remove(id);
		}
	}
	
	public static <T> void execute(Prevayler<Root> pRoot, final int id, final Transaction<T> transaction) {
		pRoot.execute(new Transaction<Root>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@SuppressWarnings("unchecked")
			@Override
			public void executeOn(Root prevalentSystem, Date executionTime) {
				T obj = (T)prevalentSystem.get(id);
				transaction.executeOn(obj, executionTime);
			}
		});
	}
}
