package dynamake.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class Categorizer<T, U> {
	private Hashtable<T, List<U>> categories = new Hashtable<T, List<U>>();
	
	public void add(T category, U item) {
		List<U> itemsInCategory = categories.get(category);
		if(itemsInCategory == null) {
			itemsInCategory = new ArrayList<U>();
			categories.put(category, itemsInCategory);
		}
		itemsInCategory.add(item);
	}
	
	public void remove(T category, U item) {
		List<U> itemsInCategory = categories.get(category);
		if(itemsInCategory != null)
			itemsInCategory.remove(item);
	}
	
	public List<U> getItems(T category) {
		List<U> itemsInCategory = categories.get(category);
		if(itemsInCategory != null)
			return new ArrayList<U>(itemsInCategory);
		return Collections.emptyList();
	}
	
	public boolean containsItem(T category, U item) {
		List<U> itemsInCategory = categories.get(category);
		if(itemsInCategory != null)
			itemsInCategory.contains(item);
		return false;
	}

	public void clear() {
		categories.clear();
	}
}
