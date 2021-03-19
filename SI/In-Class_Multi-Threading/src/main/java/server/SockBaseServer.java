package server;

import java.net.*;
import java.io.*;

import buffers.OperationProtos.Request;
import buffers.OperationProtos.Response;


class SockBaseServer {
    public static void main (String args[]) throws Exception {

        int count = 0;
        ServerSocket    serv = null;
        InputStream in = null;
        OutputStream out = null;
        Socket clientSocket = null;
        int port = 9099; // default port
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
        while (serv.isBound() && !serv.isClosed()) {
            System.out.println("Ready...");
            try {
                clientSocket = serv.accept();
                // spin up threak
                in = clientSocket.getInputStream();
                out = clientSocket.getOutputStream();
                Request req = Request.parseDelimitedFrom(in);
                System.out.println(req.toString());
                int add = 0;


                if (req.getOperationType() == Request.OperationType.ADD) {
                  for (int num: req.getNumsList()){
                    add += num;
                  }
                } else if (req.getOperationType() == Request.OperationType.SUB){

                }

                Response.Builder resBuilder = Response.newBuilder();
                resBuilder.setSuccess(true);
                resBuilder.setResult(add);
                Response res = resBuilder.build();

                res.writeDelimitedTo(out);


                
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (out != null)  out.close();
                if (in != null)   in.close();
                if (clientSocket != null) clientSocket.close();
            }
        }
    }

}

