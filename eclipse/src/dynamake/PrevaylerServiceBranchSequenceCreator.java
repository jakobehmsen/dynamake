package dynamake;

import java.util.ArrayList;

public class PrevaylerServiceBranchSequenceCreator<T> implements PrevaylerServiceBranchCreator<T> {
	private DualCommandFactory<T> transactionFactory;
	private PrevaylerServiceBranchContinuation<T> continuation;
	
	public PrevaylerServiceBranchSequenceCreator(DualCommandFactory<T> transactionFactory, PrevaylerServiceBranchContinuation<T> continuation) {
		this.transactionFactory = transactionFactory;
		this.continuation = continuation;
	}

	private void connectParts(
		PrevaylerServiceBranchCreation<T> branchCreation, final int index, final ArrayList<DualCommand<T>> dualCommands) {
		DualCommand<T> transaction = dualCommands.get(index);
		PrevaylerServiceBranchContinuation<T> nextContinuation;
		
		if(index + 1 == dualCommands.size()) {
			nextContinuation = continuation;
		} else {
			nextContinuation = new PrevaylerServiceBranchContinuation<T>() {
				@Override
				public void doContinue(PropogationContext propCtx, PrevaylerServiceBranch<T> branch) {
//					branch.branch(propCtx, new PrevaylerServiceBranchCreator<T>() {
//						@Override
//						public void create(PrevaylerServiceBranchCreation<T> branchCreation) {
//							connectParts(branchCreation, index + 1, dualCommands);
//						}
//					});
				}
			};
		}
		
		branchCreation.create(transaction, nextContinuation);
	}

	@Override
	public void create(PrevaylerServiceBranchCreation<T> branchCreation) {
		ArrayList<DualCommand<T>> dualCommands = new ArrayList<DualCommand<T>>();
		transactionFactory.createDualCommands(dualCommands);
		
		connectParts(branchCreation, 0, dualCommands);
	}
	
//	private PrevaylerServiceBranchCreator<T> build(final PrevaylerServiceBranchCreator<T> branchCreator, final int index) {
//		if(index == parts.size())
//			return branchCreator;
//		
//		return new PrevaylerServiceBranchCreator<T>() {
//			@Override
//			public void create(PrevaylerServiceBranchCreation<T> branchCreation) {
//				DualCommand<T> transaction = null;
//				branchCreation.create(transaction, new PrevaylerServiceBranchContinuation<T>() {
//					@Override
//					public void doContinue(PropogationContext propCtx, PrevaylerServiceBranch<T> branch) {
//						branch.branch(propCtx, build(branchCreator, index + 1));
//					}
//				});
//			}
//		};
//	}
	
//	public PrevaylerServiceBranch<T> build(PrevaylerServiceBranch<T> branch, PropogationContext propCtx, PrevaylerServiceBranchCreator<T> branchCreator) {
//		return build(branch, propCtx, branchCreator, 0);
//	}
//	
//	private PrevaylerServiceBranch<T> build(PrevaylerServiceBranch<T> branch, PropogationContext propCtx, PrevaylerServiceBranchCreator<T> branchCreator, int index) {
//		if(index == parts.size())
//			return branch.branch(propCtx, branchCreator);
//		
//		return branch.branch(propCtx, new PrevaylerServiceBranchCreator<T>() {
//			@Override
//			public void create(PrevaylerServiceBranchCreation<T> branchCreation) {
//				// TODO Auto-generated method stub
//				
//			}
//		});
//	}
}
