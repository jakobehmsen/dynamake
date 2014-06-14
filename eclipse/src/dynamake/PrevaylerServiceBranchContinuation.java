package dynamake;

public interface PrevaylerServiceBranchContinuation<T> {
	void doContinue(PropogationContext propCtx, PrevaylerServiceBranch<T> branch);
	
	public static class Util {
		public static <T> PrevaylerServiceBranchContinuation<T> absorb() {
			return new PrevaylerServiceBranchContinuation<T>() {
				@Override
				public void doContinue(PropogationContext propCtx, PrevaylerServiceBranch<T> branch) {
//					branch.absorb();
				}
			};
		}
	}
}
