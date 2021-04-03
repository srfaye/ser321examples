import java.net.*;
import java.io.*;
import buffers.TransactionProtos.TCRequest;
import buffers.TransactionProtos.TCResponse;

public class Client {

  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Expected arguments: <host(String)> <port(int)>");
      System.exit(1);
    }
    String tcHost = args[0];
    int tcPort = 8007; // Default from build.gradle
    try {
      tcPort = Integer.parseInt(args[1]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port] must be integer");
      System.exit(2);
    }
    System.out.println("Client connecting to Transaction Coordinator at " + tcHost + ":" + tcPort);

    try {
      Socket sock = new Socket(tcHost, tcPort);
      InputStream in = sock.getInputStream();
      OutputStream out = sock.getOutputStream();
      BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));


      while (true) {

        System.out.println("Enter transaction value or 'quit' to exit...");
        String userInput = stdin.readLine();
        double transactionValue = Double.parseDouble(userInput);
        if (userInput.equalsIgnoreCase("quit")) {
          // close connection
          sock.close();
          break;
        }
        System.out.println("Enter from Bank name...");
        String fromBankName = stdin.readLine();
        System.out.println("Enter to Bank name...");
        String toBankName = stdin.readLine();

        // get/send the transaction request
        TCRequest.Builder tcRequest = TCRequest.newBuilder();
        tcRequest.setAmount(transactionValue);
        tcRequest.setFrom(fromBankName);
        tcRequest.setTo(toBankName);
        tcRequest.build().writeDelimitedTo(out);

        // receive a response
        TCResponse tcRes = TCResponse.parseDelimitedFrom(in);
        System.out.println("Transaction request success?: " + tcRes.getSuccess());

      }


    } catch (Exception ex) {
      ex.printStackTrace();
    }

  }


}
