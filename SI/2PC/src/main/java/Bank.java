import java.net.*;
import java.io.*;
import buffers.TransactionProtos.BankRequest;
import buffers.TransactionProtos.BankResponse;
import buffers.TransactionProtos.TCConfirmTransaction;
import java.lang.Math;


public class Bank {


  public static void main(String[] args) {
    double accountValue = 0;
    if (args.length != 3) {
      System.out.println("Expected arguments: <port(int)> <bankName(String)>");
      System.exit(1);
    }
    int port = 8008; // default *BANK A* port from build.gradle (shouldn't matter if ran properly)
    try {
      port = Integer.parseInt(args[0]);
      accountValue = Double.parseDouble(args[2]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port] must be integer");
      System.exit(2);
    }
    String bankName = args[1];

    System.out.println("Initial " + bankName + " account value: " + accountValue);
    System.out.println(bankName + " listening on port " + port);



    try {
      ServerSocket serv = new ServerSocket(port);
      Socket tcSock = null;
      tcSock = serv.accept(); // move this
      InputStream in = tcSock.getInputStream();
      OutputStream out = tcSock.getOutputStream();
      System.out.println("A TC has connected!");


      while (serv.isBound() && !serv.isClosed()) {
        // get a transaction request
        BankRequest req = BankRequest.parseDelimitedFrom(in);
        System.out.println("Received a request for " + req.getAmount() + "; Withdraw: " + req.getWithdraw());

        // check the account!
        boolean canPerform = true;
        if (req.getWithdraw() == true) {
          if (!(accountValue - Math.abs(req.getAmount()) >= 0.0)) {
            canPerform = false;
          }
        }

        // send back yes or no
        if (canPerform) {
          BankResponse.Builder bankRes = BankResponse.newBuilder();
          bankRes.setCanPerform(true).build().writeDelimitedTo(out);

          // wait for confirmation, if yes perform transaction
          TCConfirmTransaction confirm = TCConfirmTransaction.parseDelimitedFrom(in);
          if (confirm.getPerform() == true) {
            System.out.println("Transaction confirmed! Performing...");
            bankRes.setHasPerformed(true).build().writeDelimitedTo(out);
          } else {
            System.out.println("Transaction cancelled!");
            bankRes.setHasPerformed(false).build().writeDelimitedTo(out);
          }



        } else {
          System.out.println("That account doesn't have enough money! Cancelling transaction!");
          BankResponse.Builder bankRes = BankResponse.newBuilder();
          bankRes.setHasPerformed(false).build().writeDelimitedTo(out);
        }



      }


    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }


}
