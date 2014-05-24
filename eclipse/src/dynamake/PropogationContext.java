package dynamake;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class PropogationContext {
	private PropogationContext parent;
	private ArrayList<PropogationContext> children = new ArrayList<PropogationContext>();
	private HashSet<Model> visitedByModels = new HashSet<Model>();
	private Map<String, Object> definitions = new Hashtable<String, Object>();
	
	private HashSet<Integer> tags = new HashSet<Integer>();
	private List<Command<Model>> backwardTransactions = new ArrayList<Command<Model>>();
	
	public PropogationContext() { }
	
	public PropogationContext(Map<String, Object> definitions) { 
		this.definitions = definitions;
	}
	
	public PropogationContext(int tag) {
		tag(tag);
	}
	
	public boolean isMarkedVisitedBy(Model model) {
		if(visitedByModels.contains(model))
			return true;
		if(parent != null)
			return parent.isMarkedVisitedBy(model);
		return false;
	}
	
	public boolean isTagged(int tag) {
		if(this.tags.contains(tag))
			return true;
		if(parent != null)
			return parent.isTagged(tag);
		return false;
	}

	public void markVisitedBy(Model model) {
		visitedByModels.add(model);
	}

	public void tag(int tag) {
		tags.add(tag);
	}
	
	public Object lookup(String term) {
		if(definitions != null) {
			Object meaning = definitions.get(term);
			if(meaning != null)
				return meaning;
		}
		if(parent != null)
			return parent.lookup(term);
		return null;
	}

	public void define(String term, Object meaning) {
		definitions.put(term, meaning);
	}

	public boolean isOrDerivesFrom(PropogationContext propCtx) {
		if(this == propCtx)
			return true;
		if(parent != null)
			return parent.isOrDerivesFrom(propCtx);
		return false;
	}
	
	public void collectBackwardTransaction(Command<Model> backwardTransaction) {
		backwardTransactions.add(backwardTransaction);
	}
	
	public List<Command<Model>> getBackwardTransactions() {
		return backwardTransactions;
	}
	
	public PropogationContext getParent() {
		return parent;
	}

	public PropogationContext branch() {
		PropogationContext propCtxBranch = new PropogationContext();
		propCtxBranch.parent = this;
		this.children.add(propCtxBranch);
		return propCtxBranch;
	}
}
