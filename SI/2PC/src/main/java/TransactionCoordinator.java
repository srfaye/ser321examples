import java.net.*;
import java.io.*;
import buffers.TransactionProtos.TCRequest;
import buffers.TransactionProtos.TCResponse;
import buffers.TransactionProtos.BankRequest;
import buffers.TransactionProtos.BankResponse;
import buffers.TransactionProtos.TCConfirmTransaction;

public class TransactionCoordinator {


  public static void main(String[] args) {
    if (args.length != 5) {
      System.out.println("Expected arguments: <port(int)> <bankAHost(String)> <bankAPort(int)> <bankBHost(String)> <bankBPort(int)>");
      System.exit(1);
    }
    String bankAHost = args[1];
    String bankBHost = args[3];
    int port = 8007; // default ports from build.gradle
    int bankAPort = 8008;
    int bankBPort = 8009;
    try {
      port = Integer.parseInt(args[0]);
      bankAPort = Integer.parseInt(args[2]);
      bankBPort = Integer.parseInt(args[4]);
    } catch (NumberFormatException nfe) {
      System.out.println("Port arguments must be integer");
      System.exit(2);
    }
    System.out.println("Transaction Coorinator connecting to:");
    System.out.println("Bank A at " + bankAHost + ":" + bankAPort);
    System.out.println("Bank B at " + bankBHost + ":" + bankBPort);
    System.out.println("Transaction Coordinator listening on port " + port);

    try {
      ServerSocket serv = new ServerSocket(port);
      Socket bankASock = new Socket(bankAHost, bankAPort);
      Socket bankBSock = new Socket(bankBHost, bankBPort);
      InputStream bankAIn = bankASock.getInputStream();
      OutputStream bankAOut = bankASock.getOutputStream();
      InputStream bankBIn = bankBSock.getInputStream();
      OutputStream bankBOut = bankBSock.getOutputStream();


      // Just one client!
      System.out.println("Transaction Coordinator waiting for connection...");
      Socket clientSocket = serv.accept();
      InputStream in = clientSocket.getInputStream();
      OutputStream out = clientSocket.getOutputStream();
      System.out.println("Connected to a client!");

      while (serv.isBound() && !serv.isClosed()) {
        try {



          // receive a transaction request
          TCRequest tcReq = TCRequest.parseDelimitedFrom(in);
          System.out.println("Received request to transfer " + tcReq.getAmount() + " from " + tcReq.getFrom() + " to " + tcReq.getTo());

          // check with both banks
          BankRequest.Builder bankAReq = BankRequest.newBuilder();
          if (tcReq.getFrom().equals("Bank A")) {
            bankAReq.setAmount(tcReq.getAmount());
            bankAReq.setWithdraw(true);
          } else {
            bankAReq.setAmount(tcReq.getAmount());
            bankAReq.setWithdraw(false);
          }
          bankAReq.build().writeDelimitedTo(bankAOut);
          BankResponse bankARes = BankResponse.parseDelimitedFrom(bankAIn);

          BankRequest.Builder bankBReq = BankRequest.newBuilder();
          if (tcReq.getFrom().equals("Bank B")) {
            bankBReq.setAmount(tcReq.getAmount());
            bankBReq.setWithdraw(true);
          } else {
            bankBReq.setAmount(tcReq.getAmount());
            bankBReq.setWithdraw(false);
          }
          bankBReq.build().writeDelimitedTo(bankBOut);
          BankResponse bankBRes = BankResponse.parseDelimitedFrom(bankBIn);

          // if both yes
          if (bankARes.getCanPerform() == true && bankBRes.getCanPerform() == true) {
            System.out.println("We can perform the transaction!");

            TCConfirmTransaction.Builder bankAConfirm = TCConfirmTransaction.newBuilder();
            bankAConfirm.setPerform(true);
            bankAConfirm.build().writeDelimitedTo(bankAOut);
            BankResponse bankAConfirmRes = BankResponse.parseDelimitedFrom(bankAIn);

            TCConfirmTransaction.Builder bankBConfirm = TCConfirmTransaction.newBuilder();
            bankBConfirm.setPerform(true);
            bankBConfirm.build().writeDelimitedTo(bankBOut);
            BankResponse bankBConfirmRes = BankResponse.parseDelimitedFrom(bankBIn);

            if (bankAConfirmRes.getHasPerformed() == true && bankBConfirmRes.getHasPerformed() == true) {
              System.out.println("Transaction has been performed!");

              // notify the client that the transaction was successful
              TCResponse.Builder tcRes = TCResponse.newBuilder();
              tcRes.setSuccess(true).build().writeDelimitedTo(out);

            }

          } else {
            System.out.println("Transaction can't be performed, notify banks and client");

            if (bankBRes.getCanPerform() == false) {
              if (bankARes.getCanPerform() == true) {
              TCConfirmTransaction.Builder bankAConfirm = TCConfirmTransaction.newBuilder();
              bankAConfirm.setPerform(false);
              bankAConfirm.build().writeDelimitedTo(bankAOut);
              BankResponse bankAConfirmRes = BankResponse.parseDelimitedFrom(bankAIn);
            }
          }


          if (bankARes.getCanPerform() == false) {
            if (bankBRes.getCanPerform() == true) {
              TCConfirmTransaction.Builder bankBConfirm = TCConfirmTransaction.newBuilder();
              bankBConfirm.setPerform(false);
              bankBConfirm.build().writeDelimitedTo(bankBOut);
              BankResponse bankBConfirmRes = BankResponse.parseDelimitedFrom(bankBIn);
            }
          }

          // notify the client that the transaction was not successful
          TCResponse.Builder tcRes = TCResponse.newBuilder();
          tcRes.setSuccess(false).build().writeDelimitedTo(out);
        }


      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }


  } catch (Exception ex) {
    ex.printStackTrace();
  }
}


}
