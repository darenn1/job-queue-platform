import java.util.concurrent.*;

public class FutureDemo {

    public static void main(String[] args) throws Exception {

        ExecutorService executor = Executors.newSingleThreadExecutor();

        Callable<Integer> task = () -> {
            System.out.println("Task started in " + Thread.currentThread().getName());
            Thread.sleep(2000);
            return 42;
        };

        Future<Integer> future = executor.submit(task);

        System.out.println("Doing other work in main thread...");

        Integer result = future.get();

        System.out.println("Result = " + result);

        executor.shutdown();
    }
}