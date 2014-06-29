package dynamake.transcription;

public interface TranscriberBranchCreator<T> {
	void create(TranscriberBranchCreation<T> branchCreation);
	
	public static class Util {
		public static <T> TranscriberBranchCreator<T> empty() {
			return new TranscriberBranchCreator<T>() {
				@Override
				public void create(TranscriberBranchCreation<T> branchCreation) { }
			};
		}
	}
}
