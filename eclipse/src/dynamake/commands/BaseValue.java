package dynamake.commands;

public interface BaseValue<T> {
	BaseValue<T> forForwarding();
	BaseValue<T> forUpwarding();
	BaseValue<T> mapToReferenceLocation(T source, T target);
	
	public static class Util {
		@SuppressWarnings("unchecked")
		public static <T> Object forForwarding(T obj) {
			if(obj instanceof BaseValue)
				return ((BaseValue<T>)obj).forForwarding();
			return obj;
		}
		@SuppressWarnings("unchecked")
		public static <T> Object forUpwarding(T obj) {
			if(obj instanceof BaseValue)
				return ((BaseValue<T>)obj).forUpwarding();
			return obj;
		}

		@SuppressWarnings("unchecked")
		public static <T> Object mapToReferenceLocation(Object obj, T source, T target) {
			if(obj instanceof BaseValue)
				return ((BaseValue<T>)obj).mapToReferenceLocation(source, target);
			return obj;
		}
	}
}
