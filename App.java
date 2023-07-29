package track;
import java.util.Scanner;
import java.util.ArrayList;

public class App {
  
  public static void main(String[] args) {
    // このコードは標準入力と標準出力を用いたサンプルコードです。
    // このコードは好きなように編集・削除してもらって構いません。
    // ---
    // This is a sample code to use stdin and stdout.
    // Edit and remove this code as you like.

    String[] lines = new JinDori(getStdin()).outputLines();
    for (int i = 0, l = lines.length; i < l; i++) {
      String output = String.format("%s", lines[i]);
      System.out.println(output);
    }
  }

  private static String[] getStdin() {
    Scanner scanner = new Scanner(System.in);
    ArrayList<String> lines = new ArrayList<>();
    while (scanner.hasNext()) {
      lines.add(scanner.nextLine());
    }
    return lines.toArray(new String[lines.size()]);
  }
}
