import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
public class ProducerConsumerDemo {
  static BlockingQueue<String> queue = new LinkedBlockingQueue<>();
  static class Producer implements Runnable {
    @Override
    public void run() {
      for (int i = 0; i < 20; i++) {
        String job = "Job-" + i;
        try {
          queue.put(job);
          System.out.println("Produced: " + job);
          Thread.sleep(500);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
  static class Consumer implements Runnable {
    private final String name;

    Consumer(String name) {
      this.name = name;
    }

    @Override
    public void run() {
      try {
        while (true) {
          String job = queue.take();
          System.out.println(name + " Consumed: " + job);
          Thread.sleep(800);
        }
      } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
      }
    }
  }

  public static void main(String[] args) {
    Thread producerThread = new Thread(new Producer());
    producerThread.start();
    new Thread(new Consumer("Consumer-1")).start();
    new Thread(new Consumer("Consumer-2")).start();
    new Thread(new Consumer("Consumer-3")).start();
  }



  
}
