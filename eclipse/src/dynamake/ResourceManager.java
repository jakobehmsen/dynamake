package dynamake;

import java.util.Hashtable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ResourceManager {
	public static final ResourceManager INSTANCE = new ResourceManager();
	
	private ExecutorService exeService;
	private Hashtable<String, Future<?>> resourceFutures;
	
	private ResourceManager() {
//		int cores = Runtime.getRuntime().availableProcessors();
//		exeService = Executors.newFixedThreadPool(cores);
		exeService = Executors.newSingleThreadExecutor();
		resourceFutures = new Hashtable<String, Future<?>>();
	}
	
	public <T> void setResourceAccessor(String name, Callable<T> resourceGetter) {
		Future<T> future = exeService.submit(resourceGetter);
		resourceFutures.put(name, future);
	}
	
	public <T> T getResource(String name, Class<T> c) throws InterruptedException, ExecutionException {
		@SuppressWarnings("unchecked")
		Future<T> future = (Future<T>) resourceFutures.get(name);
		return future != null ? future.get() : null;
	}
	
	public void dispose() {
		exeService.shutdown();
	}
}
