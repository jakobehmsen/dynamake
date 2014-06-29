package dynamake;

import dynamake.models.PropogationContext;

public interface TranscriberBranchContinuation<T> {
	void doContinue(PropogationContext propCtx, TranscriberBranch<T> branch);
	
	public static class Util {
		public static <T> TranscriberBranchContinuation<T> absorb() {
			return new TranscriberBranchContinuation<T>() {
				@Override
				public void doContinue(PropogationContext propCtx, TranscriberBranch<T> branch) {
//					branch.absorb();
				}
			};
		}
	}
}
