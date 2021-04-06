import java.net.*;
import java.io.*;
import java.util.concurrent.TimeUnit;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import rpc.TCRequest;
import rpc.TCResponse;
import rpc.BankRequest;
import rpc.BankResponse;
import rpc.TCConfirmTransaction;
import rpc.BankServiceGrpc;


public class TransactionCoordinator {
  private final BankServiceGrpc.BankServiceBlockingStub blockingStub;
  public String bankName;

  /** Construct client for accessing server using the existing channel. */
  public TransactionCoordinator(Channel channel, String bankName) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
    // shut it down.
    // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
    blockingStub = BankServiceGrpc.newBlockingStub(channel);
    this.bankName = bankName;
  }

  public BankResponse checkTransaction(Double amount, boolean withdraw) {
    BankRequest request = BankRequest.newBuilder().setAmount(amount).setWithdraw(withdraw).build();
    BankResponse response;
    try {
      response = blockingStub.checkTransaction(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return null;
    }
    System.out.println("Received from " + bankName + "...");
    System.out.println("Can perform: " + response.getCanPerform());
    System.out.println("Has performed: " + response.getHasPerformed());
    System.out.println();
    return response;
  }

  public BankResponse confirmTransaction(boolean perform, Double amount, boolean withdraw) {
    TCConfirmTransaction request = TCConfirmTransaction.newBuilder().setPerform(perform).setAmount(amount).setWithdraw(withdraw).build();
    BankResponse response;
    try {
      response = blockingStub.performTransaction(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return null;
    }
    System.out.println("Received from " + bankName + "...");
    System.out.println("Can perform: " + response.getCanPerform());
    System.out.println("Has performed: " + response.getHasPerformed());
    System.out.println();
    return response;
  }

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

    // Create a communication channel to the server, known as a Channel. Channels are thread-safe
    // and reusable. It is common to create channels at the beginning of your application and reuse
    // them until the application shuts down.
    String targetA = bankAHost + ":" + bankAPort;
    ManagedChannel channelA = ManagedChannelBuilder.forTarget(targetA)
    // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
    // needing certificates.
    .usePlaintext()
    .build();
    String targetB = bankBHost + ":" + bankBPort;
    ManagedChannel channelB = ManagedChannelBuilder.forTarget(targetB)
    .usePlaintext()
    .build();


    try {
      ServerSocket serv = new ServerSocket(port);
      TransactionCoordinator tcA = new TransactionCoordinator(channelA, "Bank A");
      TransactionCoordinator tcB = new TransactionCoordinator(channelB, "Bank B");
      // Socket bankASock = new Socket(bankAHost, bankAPort);
      // Socket bankBSock = new Socket(bankBHost, bankBPort);
      // InputStream bankAIn = bankASock.getInputStream();
      // OutputStream bankAOut = bankASock.getOutputStream();
      // InputStream bankBIn = bankBSock.getInputStream();
      // OutputStream bankBOut = bankBSock.getOutputStream();

      // Just one client!
      while (true) {
        try {
          System.out.println("Transaction Coordinator waiting for connection from client...");
          Socket clientSocket = serv.accept();
          InputStream in = clientSocket.getInputStream();
          OutputStream out = clientSocket.getOutputStream();
          System.out.println("Connected to a client!");

          while (serv.isBound() && !serv.isClosed()) {

            // receive a transaction request
            TCRequest tcReq = TCRequest.parseDelimitedFrom(in);
            System.out.println("Received request to transfer " + tcReq.getAmount() + " from " + tcReq.getFrom() + " to " + tcReq.getTo());

            // check with both banks
            BankResponse bankARes = tcA.checkTransaction(tcReq.getAmount(), (tcReq.getFrom().equals("Bank A")));
            BankResponse bankBRes = tcB.checkTransaction(tcReq.getAmount(), (tcReq.getFrom().equals("Bank B")));
            // BankRequest.Builder bankAReq = BankRequest.newBuilder();
            // if (tcReq.getFrom().equals("Bank A")) {
            //   bankAReq.setAmount(tcReq.getAmount());
            //   bankAReq.setWithdraw(true);
            // } else {
            //   bankAReq.setAmount(tcReq.getAmount());
            //   bankAReq.setWithdraw(false);
            // }
            // bankAReq.build().writeDelimitedTo(bankAOut);
            // BankResponse bankARes = BankResponse.parseDelimitedFrom(bankAIn);
            //
            // BankRequest.Builder bankBReq = BankRequest.newBuilder();
            // if (tcReq.getFrom().equals("Bank B")) {
            //   bankBReq.setAmount(tcReq.getAmount());
            //   bankBReq.setWithdraw(true);
            // } else {
            //   bankBReq.setAmount(tcReq.getAmount());
            //   bankBReq.setWithdraw(false);
            // }
            // bankBReq.build().writeDelimitedTo(bankBOut);
            // BankResponse bankBRes = BankResponse.parseDelimitedFrom(bankBIn);

            // if both yes
            if (bankARes.getCanPerform() == true && bankBRes.getCanPerform() == true) {
              System.out.println("We can perform the transaction!");

              BankResponse bankAConfirmRes = tcA.confirmTransaction(true, tcReq.getAmount(), (tcReq.getFrom().equals("Bank A")));
              BankResponse bankBConfirmRes = tcB.confirmTransaction(true, tcReq.getAmount(), (tcReq.getFrom().equals("Bank B")));

              // TCConfirmTransaction.Builder bankAConfirm = TCConfirmTransaction.newBuilder();
              // bankAConfirm.setPerform(true);
              // bankAConfirm.build().writeDelimitedTo(bankAOut);
              // BankResponse bankAConfirmRes = BankResponse.parseDelimitedFrom(bankAIn);
              //
              // TCConfirmTransaction.Builder bankBConfirm = TCConfirmTransaction.newBuilder();
              // bankBConfirm.setPerform(true);
              // bankBConfirm.build().writeDelimitedTo(bankBOut);
              // BankResponse bankBConfirmRes = BankResponse.parseDelimitedFrom(bankBIn);

              if (bankAConfirmRes.getHasPerformed() == true && bankBConfirmRes.getHasPerformed() == true) {
                System.out.println("Transaction has been performed!");

                // notify the client that the transaction was successful
                TCResponse.Builder tcRes = TCResponse.newBuilder();
                tcRes.setSuccess(true).build().writeDelimitedTo(out);
              }

            } else {
              System.out.println("Transaction can't be performed, notify banks and client");

              // if (bankBRes.getCanPerform() == false) {
              //   if (bankARes.getCanPerform() == true) {
              //     TCConfirmTransaction.Builder bankAConfirm = TCConfirmTransaction.newBuilder();
              //     bankAConfirm.setPerform(false);
              //     bankAConfirm.build().writeDelimitedTo(bankAOut);
              //     BankResponse bankAConfirmRes = BankResponse.parseDelimitedFrom(bankAIn);
              //   }
              // }
              //
              // if (bankARes.getCanPerform() == false) {
              //   if (bankBRes.getCanPerform() == true) {
              //     TCConfirmTransaction.Builder bankBConfirm = TCConfirmTransaction.newBuilder();
              //     bankBConfirm.setPerform(false);
              //     bankBConfirm.build().writeDelimitedTo(bankBOut);
              //     BankResponse bankBConfirmRes = BankResponse.parseDelimitedFrom(bankBIn);
              //   }
              // }

              // notify the client that the transaction was not successful
              TCResponse.Builder tcRes = TCResponse.newBuilder();
              tcRes.setSuccess(false).build().writeDelimitedTo(out);
            }

          }

        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }


    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      try {
        channelA.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        channelB.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }


}
