public class ThreadDemo {
  static class MyThread implements Runnable {
    @Override
    public void run() {
      String name = Thread.currentThread().getName();
      for (int i = 0; i < 5; i++) { 
        System.out.println(name + " : " + i);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
       Thread.currentThread().interrupt();
      }
    }
  }

}
public static void main(String[] args) {
  for(int i = 0; i < 5; i++) {
    Thread t = new Thread(new MyThread(), "Thread-" + i);
    t.start();
  }
}
  
}
