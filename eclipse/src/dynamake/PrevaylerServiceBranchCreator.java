package dynamake;

public interface PrevaylerServiceBranchCreator<T> {
	void create(PrevaylerServiceBranchCreation<T> branchCreation);
	
	public static class Util {
		public static <T> PrevaylerServiceBranchCreator<T> empty() {
			return new PrevaylerServiceBranchCreator<T>() {
				@Override
				public void create(PrevaylerServiceBranchCreation<T> branchCreation) { }
			};
		}
	}
}
