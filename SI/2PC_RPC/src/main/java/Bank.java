import java.net.*;
import java.io.*;

import java.lang.Math;
import java.util.concurrent.TimeUnit;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import rpc.BankRequest;
import rpc.BankResponse;
import rpc.TCConfirmTransaction;
import rpc.BankServiceGrpc;

public class Bank {
  private Server server;
  int port;

  static String bankName;
  static double accountValue;

  Bank(int port) {
    this.port = port;
  }

  private void start() throws IOException {
    /* The port on which the server should run */
    server = ServerBuilder.forPort(port)
        .addService(new BankServiceImpl())
        .build()
        .start();

    System.out.println(bankName + " running...");
    System.out.println("Inital account value: " + accountValue + "\n");
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        try {
          Bank.this.stop();
        } catch (InterruptedException e) {
          e.printStackTrace(System.err);
        }
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /* Our implementation of the service */
  static class BankServiceImpl extends BankServiceGrpc.BankServiceImplBase {

    @Override
    public void checkTransaction(BankRequest req, StreamObserver<BankResponse> responseObserver) {
      System.out.println("Received from TC...\nAmount: " + req.getAmount() + "\nWithdraw: " + req.getWithdraw());
      System.out.println("Current balance: " + accountValue + "\n");
      boolean canPerform = true;
      if (req.getWithdraw() == true) {
        if (!(accountValue - Math.abs(req.getAmount()) >= 0.0)) {
          canPerform = false;
          System.out.println("I can't perform that transaction, I'm broke!\n");
        }
      }
      BankResponse response = BankResponse.newBuilder().setCanPerform(canPerform).setHasPerformed(false).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    }

    @Override
    public void performTransaction(TCConfirmTransaction req, StreamObserver<BankResponse> responseObserver) {
      System.out.println("Received from TC...\nPerform transaction?: " + req.getPerform() + "\n");
      if (req.getWithdraw()) {
        accountValue -= Math.abs(req.getAmount());
      } else {
        accountValue += Math.abs(req.getAmount());
      }
      System.out.println("New account value: " + accountValue + "\n");
      BankResponse response = BankResponse.newBuilder().setCanPerform(true).setHasPerformed(true).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    }
  }

  /**
   * Main launches the server from the command line.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    double accountValue = 0;
    String bankName = "";
    int port = 8008; // default *BANK A* port from build.gradle (shouldn't matter if ran properly)
    ServerSocket serv = null;
    if (args.length != 3) {
      System.out.println("Expected arguments: <port(int)> <bankName(String)>");
      System.exit(1);
    }
    try {
      port = Integer.parseInt(args[0]);
      bankName = args[1];
      accountValue = Double.parseDouble(args[2]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port] must be integer");
      System.exit(2);
    }
    final Bank bank = new Bank(port);
    Bank.bankName = bankName;
    Bank.accountValue = accountValue;
    bank.start();
    bank.blockUntilShutdown();
  }


  // public static void main(String[] args) {
    // double accountValue = 0;
    // ServerSocket serv = null;
    // if (args.length != 3) {
    //   System.out.println("Expected arguments: <port(int)> <bankName(String)>");
    //   System.exit(1);
    // }
    // int port = 8008; // default *BANK A* port from build.gradle (shouldn't matter if ran properly)
    // try {
    //   port = Integer.parseInt(args[0]);
    //   serv = new ServerSocket(port);
    //   accountValue = Double.parseDouble(args[2]);
    // } catch (NumberFormatException nfe) {
    //   System.out.println("[Port] must be integer");
    //   System.exit(2);
    // } catch (IOException ioe) {
    //   System.out.println("Problem creating ServerSocket. Exiting...");
    //   System.exit(2);
    // }
    // String bankName = args[1];
  //
  //
  //   System.out.println("Initial " + bankName + " account value: " + accountValue);
  //
  //   while (true) {
  //     try {
  //       System.out.println(bankName + " listening on port " + port);
  //       Socket tcSock = null;
  //       tcSock = serv.accept(); // move this
  //       InputStream in = tcSock.getInputStream();
  //       OutputStream out = tcSock.getOutputStream();
  //       System.out.println("A TC has connected!");
  //
  //
  //       while (serv.isBound() && !serv.isClosed()) {
  //         // get a transaction request
          // BankRequest req = BankRequest.parseDelimitedFrom(in);
          // System.out.println("Received a request for " + req.getAmount() + "; Withdraw: " + req.getWithdraw());
          //
          // // check the account!
          // boolean canPerform = true;
          // if (req.getWithdraw() == true) {
          //   if (!(accountValue - Math.abs(req.getAmount()) >= 0.0)) {
          //     canPerform = false;
          //   }
          // }
          //
          // // send back yes or no
          // if (canPerform) {
  //           BankResponse.Builder bankRes = BankResponse.newBuilder();
  //           bankRes.setCanPerform(true).build().writeDelimitedTo(out);
  //
  //           // wait for confirmation, if yes perform transaction
  //
  //           TCConfirmTransaction confirm = TCConfirmTransaction.parseDelimitedFrom(in);
  //           if (confirm.getPerform() == true) {
  //             System.out.println("Transaction confirmed! Performing...");
  //             // perform transaction
  //             if (req.getWithdraw() == true) {
  //               accountValue -= Math.abs(req.getAmount());
  //             } else {
  //               accountValue += Math.abs(req.getAmount());
  //             }
  //             System.out.println("Transaction peformed - New account value: " + accountValue);
  //             bankRes.setHasPerformed(true).build().writeDelimitedTo(out);
  //           } else {
  //             System.out.println("Transaction cancelled!");
  //             bankRes.setHasPerformed(false).build().writeDelimitedTo(out);
  //           }
  //
  //         } else {
  //           System.out.println("That account doesn't have enough money! Cancelling transaction!");
  //           BankResponse.Builder bankRes = BankResponse.newBuilder();
  //           bankRes.setHasPerformed(false).build().writeDelimitedTo(out);
  //         }
  //
  //       }
  //
  //     } catch (Exception ex) {
  //       ex.printStackTrace();
  //     }
  //   }
  // }


}
