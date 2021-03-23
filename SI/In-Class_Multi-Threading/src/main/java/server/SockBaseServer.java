package server;

import java.net.*;
import java.io.*;
import java.util.Random;
import java.util.concurrent.locks.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import buffers.OperationProtos.Request;
import buffers.OperationProtos.Response;

class SockBaseServer implements Runnable {

  // Properties for each runnable
  protected Socket clientSocket = null;
  protected int id;
  protected ReadWriteLock mutex;

  // A static string that all threads can access
  public static String sharedString = "";

  // Constructor takes and sets a client socket, an ID, and a lock
  public SockBaseServer(Socket clientSocket, int id, ReadWriteLock mutex) {
    this.clientSocket = clientSocket;
    this.id = id;
    this.mutex = mutex;
  }

  // Concatenates a string 5 times (with a random short delay) to the static sharedString
  // Meant to simulate race conditions and shared memory access. Connect two clients to this server
  // and send a string close to the same time. Note this is NOT synchronized (potential r/w problems if ran without locks or monitors)
  public void addToSharedString(String str) throws Exception {
    Random rand = new Random();
    for (int i = 0; i < 5; i++) {
      int randNum = rand.nextInt(2 - 1 + 1) + 1;
      Thread.sleep(1000 * randNum);
      sharedString += str;
    }
  }

  // This is a synchronized static method version of the above function. It is synchronized on the class object
  // that this synchronized method belongs to (SockBaseServer.class). Behaves similarly to synchronized block in
  // run() below.
  public synchronized static void staticSyncAddToSharedString(String str) throws Exception {
    Random rand = new Random();
    for (int i = 0; i < 5; i++) {
      int randNum = rand.nextInt(2 - 1 + 1) + 1;
      Thread.sleep(1000 * randNum);
      sharedString += str;
    }
  }

  // This is what gets called when we do thread.start()
  public void run() {
    try {
      InputStream in = clientSocket.getInputStream(); // setup our in/out streams
      OutputStream out = clientSocket.getOutputStream();
      while (true) {
        Request req = Request.parseDelimitedFrom(in); // get a request
        System.out.println("String from client " + id + ":\n" + req.getStr());

        /* IF WE WERE TO SIMPLY CALL addToSharedString(), we may have a race condition */
        // addToSharedString(req.getStr());

        /* This shows how you could used a synchronized block within an instance
        method, synchronized on the SockBaseServer class
        There are other ways to handle monitors! Check out:
        http://tutorials.jenkov.com/java-concurrency/synchronized.html
        for a good summary */
        // synchronized(SockBaseServer.class) {
        //   addToSharedString(req.getStr());
        // }

        /* Using a static synchronized method. Behaves similarly to the above block */
        staticSyncAddToSharedString(req.getStr());

        /* Showing a ReadWriteLock */
        // mutex.writeLock().lock();
        // mutex.readLock().lock();
        // try {
        //   addToSharedString(req.getStr());
        // } catch (Exception ex){
        //   System.out.println(ex);
        // } finally { // make sure to unlock in a finally block or you may encounter deadlocks or starvation!
        //   mutex.writeLock().unlock();
        //   mutex.readLock().unlock();
        // }

        // Build our response and send it back to the client
        Response.Builder resBuilder = Response.newBuilder();
        resBuilder.setSuccess(true);
        /* note that we are still reading sharedString outside of a lock/synchronized block! might get a "dirty" read!
        to fix, we could make a static sychronized getSharedString() method, move this into a synchronized block that
        is synchronized on SockBaseServer.class, or move this call in between a mutex.readLock().Lock() and mutex.readLock.Unlock() */
        resBuilder.setStr(sharedString);
        Response res = resBuilder.build();
        res.writeDelimitedTo(out);
      }
    } catch (Exception ex) {
      System.out.println(ex);
    }
  }

  // Main server code
  public static void main (String args[]) throws Exception {
    // Parse command line arguments
    ServerSocket serv = null;
    int port = 8007; // default port
    int sleepDelay = 10000; // default delay
    if (args.length != 2) {
      System.out.println("Expected arguments: <port(int)> <delay(int)>");
      System.exit(1);
    }
    try {
      port = Integer.parseInt(args[0]);
      sleepDelay = Integer.parseInt(args[1]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port|sleepDelay] must be an integer");
      System.exit(2);
    }
    try {
      serv = new ServerSocket(port);
    } catch(Exception e) {
      e.printStackTrace();
      System.exit(2);
    }

    // Creating a ReadWriteLock to pass to threads
    ReadWriteLock mutex = new ReentrantReadWriteLock(); // you can instead use ReentrantLock if you don't care about separate read/write locks/unlocks

    // Creat a count for keeping track of client IDs
    int count = 0;

    // If using a thread pool, we would set it up before accepting connections
    int threadPoolSize = 2; // Max number of threads to run at a given time
    Executor pool = Executors.newFixedThreadPool(threadPoolSize);

    // Begin listening for connections and spinning up threads
    while (serv.isBound() && !serv.isClosed()) {
      System.out.println("Listening for connections...");
      try {
        Socket clientSocket = serv.accept(); // Accept an incoming connection
        System.out.println("Accepted a connection from client " + (count + 1));

        /* If implementing Runnable interface, starting a thread should look something like this
        I'd recomment using this method because it plays nicely with thread pools */
        // SockBaseServer runnable = new SockBaseServer(clientSocket, ++count, mutex);
        // Thread thread = new Thread(runnable);
        // thread.start();

        /* If extending Thread class, starting a thread would look more like this */
        // SockBaseServer sockBaseServerThread = new SockBaseServer(clientSocket, ++count, mutex);
        // sockBaseServerThread.start();

        /* If using a thread pool, starting a thread would look like this. Don't submit Thread objects to thread pool! */
        pool.execute(new SockBaseServer(clientSocket, ++count, mutex)); // Passing a new runnable to pool.execute

      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }
}
