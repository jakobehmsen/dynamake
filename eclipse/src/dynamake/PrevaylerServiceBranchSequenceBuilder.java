package dynamake;

import java.util.ArrayList;

public class PrevaylerServiceBranchSequenceBuilder<T> {
	private DualCommandFactory<T> transactionFactory;
	
	public PrevaylerServiceBranchSequenceBuilder(DualCommandFactory<T> transactionFactory) {
		this.transactionFactory = transactionFactory;
	}

	public PrevaylerServiceBranchCreator<T> build(final PrevaylerServiceBranchCreator<T> branchCreator) {
		return new PrevaylerServiceBranchCreator<T>() {
			@Override
			public void create(PrevaylerServiceBranchCreation<T> branchCreation) {
				ArrayList<DualCommand<T>> dualCommands = new ArrayList<DualCommand<T>>();
				transactionFactory.createDualCommands(dualCommands);
				
				build(branchCreator, branchCreation, 0, dualCommands);
			}
		};
	}

	public PrevaylerServiceBranch<T> build(PrevaylerServiceBranch<T> branch, PropogationContext propCtx, final PrevaylerServiceBranchCreator<T> branchCreator) {
		return branch.branch(propCtx, build(branchCreator));
	}

	private void build(
		final PrevaylerServiceBranchCreator<T> branchCreator, PrevaylerServiceBranchCreation<T> branchCreation, final int index, final ArrayList<DualCommand<T>> dualCommands) {
		DualCommand<T> transaction = dualCommands.get(index);
		PrevaylerServiceBranchContinuation<T> continuation;
		
		if(index + 1 == dualCommands.size()) {
			continuation = new PrevaylerServiceBranchContinuation<T>() {
				@Override
				public void doContinue(PropogationContext propCtx,
						PrevaylerServiceBranch<T> branch) {
					branch.branch(propCtx, branchCreator);
				}
			};
		} else {
			continuation = new PrevaylerServiceBranchContinuation<T>() {
				@Override
				public void doContinue(PropogationContext propCtx, PrevaylerServiceBranch<T> branch) {
					branch.branch(propCtx, new PrevaylerServiceBranchCreator<T>() {
						@Override
						public void create(PrevaylerServiceBranchCreation<T> branchCreation) {
							build(branchCreator, branchCreation, index + 1, dualCommands);
						}
					});
				}
			};
		}
		
		branchCreation.create(transaction, continuation);
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
