package dynamake.commands;

public interface BaseValue<T> {
	BaseValue<T> forForwarding();
	BaseValue<T> forUpwarding();
	BaseValue<T> mapToReferenceLocation(T source, T target);
	
	public static class Util {
		@SuppressWarnings("unchecked")
		public static <T> T forForwarding(T obj) {
			if(obj instanceof BaseValue)
				return (T)((BaseValue<T>)obj).forForwarding();
			return obj;
		}
		@SuppressWarnings("unchecked")
		public static <T> T forUpwarding(T obj) {
			if(obj instanceof BaseValue)
				return (T)((BaseValue<T>)obj).forUpwarding();
			return obj;
		}

		@SuppressWarnings("unchecked")
		public static <T, R> T mapToReferenceLocation(T obj, R source, R target) {
			if(obj instanceof BaseValue)
				return (T)((BaseValue<R>)obj).mapToReferenceLocation(source, target);
			return obj;
		}
	}
}
