package pbuf;

import java.net.*;
import java.io.*;
import org.json.*;
import javax.imageio.*;
import java.awt.image.*;
import java.util.Base64;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;
// Pbuf location corresponds to (in proto files): <java_package>.<java_outer_classname>.<message>
import buffers.RequestProtos.Request;
import buffers.ResponseProtos.Response;

/* PROTOCOL INFO
*
* Requests:
*
* Find a meme
* {"type": "find meme", "name": <string meme name>}
*
* Get all memes
* {"type": "get memes"}
*
* Create a meme
* {"type": "create meme", "template_id": <string id>, "box_count": <int number of text boxes>,
* "text0": <string text>, "text1": <string text>}
* OR FOR box_count > 2
* {"type": "create meme", "template_id": <string id>, "box_count": <int number of text boxes>,
* "boxes": <string JSONArray of text>}
*
* Responses:
*
* Meme
* {"type": "meme", "success": <boolean true if meme was found>, "data": <string meme JSON>}
*
* Meme array
* {"type": "memes array", "success": true, "data": <string memes JSON array>}
*
* Captioned meme
* {"type": "captioned meme", "success": <boolean true if meme was created>,
* "image": <string base64 encoded image>, "message": <string error message>}
*
* Error
* {"type": "error", "success": false, "message": <string an error message>}
*/

public class Client {
  public static InputStream in;
  public static OutputStream out;


  public static Response send(Request req) throws Exception {
    // // send request - JSON version (using ObjectInputStream and ObjectOutputStream)
    // out.writeObject(req.toString());
    // // receive a response
    // String serverResponse = (String)in.readObject();
    // JSONObject res = new JSONObject(serverResponse);

    // send a request - ProtoBuf version (using InputStream and OutputStream)
    req.writeDelimitedTo(out);
    // receive a response
    Response res = Response.parseDelimitedFrom(in);
    return res;
  }

  //************************
  // main program code
  public static void main (String args[]) {
    // get command line arguments (host, port)
    try {
      if (args.length != 2) {
        System.out.println("Argument count doesn't match...");
        System.out.println("Usage: gradle runClient -Phost=<host ip> -Pport=<port number>");
        System.exit(0);
      }
      int port = -1;
      try {
        port = Integer.parseInt(args[1]);
      } catch (NumberFormatException ex) {
        System.out.println("Error parsing port number: Port should be an int");
        System.exit(0);
      }
      String host = args[0];

      // connect to server (create socket, connect)
      Socket server = new Socket(host, port);
      System.out.println("Connected to server at " + host + ":" + port);
      // create InputStream and OutputStream to send ProtoBuf
      in = server.getInputStream();
      out = server.getOutputStream();
      BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

      // begin "infinite" while loop
      JSONObject selectedMeme = null;
      System.out.println("Welcome to the TCP Meme-maker Client!");
      while (true) {
        // print available options
        System.out.println("\nChoose an option...");
        System.out.println("1: Find Meme");
        System.out.println("2: View Memes");
        System.out.println("3: Create Meme");
        System.out.println("0: Quit");

        // get user input
        String userInput = stdin.readLine();

        // quit the client program
        if (userInput.equalsIgnoreCase("quit") || userInput.equals("0")) {
          // close connection
          server.close();
          break;
        }

        // Select a meme by name
        else if (userInput.equalsIgnoreCase("Find Meme") || userInput.equals("1")) {
          System.out.println("\nEnter the name of the meme you would like to find:");
          userInput = stdin.readLine();
          // create a Request Builder
          Request.Builder reqBuilder = Request.newBuilder();
          reqBuilder.setType(Request.RequestType.FIND_MEME)
            .setName(userInput);
          // Build the request, send it, receive the response (see send() above ^)
          Request req = reqBuilder.build();
          Response res = send(req);

          // check the response
          if (res.getSuccess() == true) { // success
            JSONObject resJson = new JSONObject(res.getData());
            System.out.println("\nFound meme \"" + resJson.getString("name") + "\"");
            System.out.println("Would you like to select this meme? (y/n)");
            userInput = stdin.readLine();
            if (userInput.equalsIgnoreCase("y") || userInput.equalsIgnoreCase("yes")) {
              selectedMeme = resJson;
              System.out.println("\nSelected meme: " + selectedMeme.toString(1));
            }
          } else { // fail
            System.out.println("\nSorry, couldn't find that meme.");
          }
        }

        // View all Memes
        else if (userInput.equalsIgnoreCase("View Memes") || userInput.equals("2")) {
          // create a Request Builder
          Request.Builder reqBuilder = Request.newBuilder();
          reqBuilder.setType(Request.RequestType.GET_MEMES);
          // Build the request, send it, receive the response (see send() above ^)
          Request req = reqBuilder.build();
          Response res = send(req);

          // check the response
          if (res.getSuccess() == true) { // success
            JSONArray memes = new JSONArray(res.getData());
            System.out.println();
            for (int i = 0; i < memes.length(); i++) {
              JSONObject currMeme = memes.getJSONObject(i);
              System.out.println(currMeme.getString("id") + ": " + currMeme.getString("name"));
            }
          } else { // fail
            System.out.println("\nThere was an error getting the list of memes.");
          }
        }

        // create meme
        else if (userInput.equalsIgnoreCase("Create Meme") || userInput.equals("3")) {
          // // TODO - SWITCH TO PROTOBUF!
          // if (selectedMeme == null) {
          //   System.out.println("\nPlease select a meme first using option 1: Find Meme");
          // } else {
          //   System.out.println("\nCreating a \"" + selectedMeme.getString("name") + "\" meme...");
          //   JSONObject req = new JSONObject();
          //   req.put("type", "create meme");
          //   req.put("template_id", selectedMeme.getString("id"));
          //   int textBoxCount = selectedMeme.getInt("box_count");
          //   req.put("box_count", textBoxCount);
          //   if (textBoxCount <= 2) {
          //     for (int i = 0; i < textBoxCount; i++) {
          //       System.out.print("Box " + Integer.toString(i + 1) +": ");
          //       userInput = stdin.readLine();
          //       req.put("text" + Integer.toString(i), userInput);
          //     }
          //   } else {
          //     JSONArray boxes = new JSONArray();
          //     for (int i = 0; i < textBoxCount; i++) {
          //       System.out.print("Box " + Integer.toString(i + 1) +": ");
          //       userInput = stdin.readLine();
          //       boxes.put(i, userInput);
          //     }
          //     req.put("boxes", boxes);
          //   }
          //
          //   // send request
          //   out.writeObject(req.toString());
          //   // receive a response
          //   String serverResponse = (String)in.readObject();
          //   JSONObject res = new JSONObject(serverResponse);
          //   if (res.getBoolean("success") == true) {
          //     BufferedImage image = decodeImage(res.getString("image"));
          //     displayImage(image);
          //     System.out.println("\nWould you like to save this meme? (y/n)");
          //     userInput = stdin.readLine();
          //     if (userInput.equalsIgnoreCase("y") || userInput.equalsIgnoreCase("yes")) {
          //       System.out.print("Name the png (without .png extension): ");
          //       userInput = stdin.readLine();
          //       try {
          //         saveImage(userInput, image);
          //       } catch (Exception ex) {
          //         System.out.println("There was an error saving the image...");
          //       }
          //     }
          //   } else {
          //     System.out.println(res.getString("message"));
          //   }
          // }
        }
        // bad user input
        else {
          System.out.println("\nSorry, I didn't understand that input...");
        }
      }

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  //************************
  // IMAGE FUNCTIONS

  // decode an image from Base64 string
  public static BufferedImage decodeImage(String base64) throws Exception {
    Base64.Decoder decoder = Base64.getDecoder();
    byte[] bytesRecv = decoder.decode(base64);
    ByteArrayInputStream byteInStream = new ByteArrayInputStream(bytesRecv);
    BufferedImage image = ImageIO.read(byteInStream);
    return image;
  }

  // save a bufferedImage to a new file
  public static void saveImage(String fileName, BufferedImage image) throws Exception {
    File file = new File(fileName + ".png");
    file.createNewFile();
    ImageIO.write(image, "png", file);
  }

  public static void displayImage(BufferedImage image) {
    ImageIcon icon = new ImageIcon(image);
    if (icon != null) {
      JFrame frame = new JFrame();
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      JLabel label = new JLabel();
      label.setIcon(icon);
      frame.add(label);
      frame.setSize(icon.getIconWidth(), icon.getIconHeight() + 40);
      frame.setVisible(true);
    }
  }

}
