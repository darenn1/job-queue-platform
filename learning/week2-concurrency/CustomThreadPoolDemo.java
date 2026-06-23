import java.util.concurrent.*;

public class CustomThreadPoolDemo {

    public static void main(String[] args) {

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,                         
                6,                        
                10, TimeUnit.SECONDS,       
                new ArrayBlockingQueue<>(10), 
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy() 
        );

        for (int i = 1; i <= 20; i++) {
            int taskId = i;

            executor.execute(() -> {
                System.out.println(
                    "Task " + taskId +
                    " executed by " + Thread.currentThread().getName()
                );

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
    }
}