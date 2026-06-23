import java.util.concurrent.atomic.AtomicInteger;

public class SharedCounter {
  private static final AtomicInteger counter = new AtomicInteger(0);
  public static void main(String[] args) throws InterruptedException {
    Thread [] threads = new Thread [10];

    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread(() -> {
        for (int j = 0; j < 1000; j++) {
          counter.incrementAndGet();
        }
      });
      threads[i].start();
    }
    for (Thread t : threads) {
      t.join();
    }
    System.out.println(counter.get());

  }
  
}
