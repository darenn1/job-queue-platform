public class GCDemo {
  public static void main(String[] args) {
        System.out.println("Starting heavy memory loop...");
        for (int i = 0; i < 10_000_000; i++) {
            String s = "Garbage data " + i;
            if (i % 1_000_000 == 0) {
                System.out.println("Created " + i + " strings...");
            }
        }
        System.out.println("Done!");
    }
  
}
