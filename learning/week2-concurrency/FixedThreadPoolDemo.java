import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FixedThreadPoolDemo {

    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(4);

        for (int i = 1; i <= 20; i++) {
            int taskId = i;

            executor.submit(() -> {
                System.out.println(
                    "Task " + taskId +
                    " executed by " + Thread.currentThread().getName()
                );

                try {
                    Thread.sleep(500); // simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
    }
}