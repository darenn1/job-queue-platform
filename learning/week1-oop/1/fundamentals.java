import java.util.HashMap;
import java.util.Map;

public class fundamentals {

    public static String reverse(String s) {
      StringBuilder sb = new StringBuilder();
      for (int i = s.length() - 1; i >= 0; i--){
        sb.append(s.charAt(i));
      }
      return sb.toString();
    }

    public static Integer WordWithHighestFrequency(String word){
      String[] words = word.toLowerCase().split(" ");
      Map<String, Integer> frequencyMap = new HashMap<>();
      for (String w : words) {
        frequencyMap.put(w, frequencyMap.getOrDefault(w, 0) + 1);
      }
      int maxFrequency = 0;
      for (Integer freq : frequencyMap.values()) {
        if (freq > maxFrequency) {
          maxFrequency = freq;
    }
      }
      return maxFrequency;
    }

    public static Boolean isPrime(Integer n) {
      if (n <= 1) return false;
      for (int i = 2; i <= Math.sqrt(n); i++) {
        if (n % i == 0) return false;
      }
      return true;
    }

    public static void main(String[] args) {
        String inputString = "hello world hello";
        System.out.println("Reversed String: " + reverse(inputString));
        System.out.println("Highest Frequency: " + WordWithHighestFrequency(inputString));
        int number = 29;
        System.out.println(number + " is prime: " + isPrime(number));
    }
}