package gui;

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
* {"type": "meme", "success": <boolean true if meme was found>, "data": <string meme JSON>, "img": <string Base64 encoded image>}
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
  public int port;
  public String host;
  Socket server;
  ObjectInputStream in;
  ObjectOutputStream out;

  public Client(String host, int port) {
    this.host = host;
    this.port = port;
    try {
      this.server = new Socket(host, port);
      in = new ObjectInputStream(server.getInputStream());
      out = new ObjectOutputStream(server.getOutputStream());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public JSONObject sendRequest(JSONObject req) throws Exception {
    out.writeObject(req.toString());
    // receive a response
    String serverResponse = (String)in.readObject();
    return new JSONObject(serverResponse);
  }



  //************************
  // main program code - don't need this for the GUI!
  // public static void main (String args[]) {
  //   // get command line arguments (host, port)
  //   try {
  //     if (args.length != 2) {
  //       System.out.println("Argument count doesn't match...");
  //       System.out.println("Usage: gradle runClient -Phost=<host ip> -Pport=<port number>");
  //       System.exit(0);
  //     }
  //     int port = -1;
  //     try {
  //       port = Integer.parseInt(args[1]);
  //     } catch (NumberFormatException ex) {
  //       System.out.println("Error parsing port number: Port should be an int");
  //       System.exit(0);
  //     }
  //     String host = args[0];
  //
  //     // connect to server (create socket, connect)
  //     Socket server = new Socket(host, port);
  //     System.out.println("Connected to server at " + host + ":" + port);
  //     ObjectInputStream in = new ObjectInputStream(server.getInputStream());
  //     ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
  //     BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
  //
  //     // begin "infinite" while loop
  //     JSONObject selectedMeme = null;
  //     System.out.println("Welcome to the TCP Meme-maker Client!");
  //     while (true) {
  //       // print available options
  //       System.out.println("\nChoose an option...");
  //       System.out.println("1: Find Meme");
  //       System.out.println("2: View Memes");
  //       System.out.println("3: Create Meme");
  //       System.out.println("0: Quit");
  //
  //       // get user input
  //       String userInput = stdin.readLine();
  //
  //       // quit the client program
  //       if (userInput.equalsIgnoreCase("quit") || userInput.equals("0")) {
  //         // close connection
  //         server.close();
  //         break;
  //       }
  //
  //       // Select a meme by name
  //       else if (userInput.equalsIgnoreCase("Find Meme") || userInput.equals("1")) {
  //         JSONObject req = new JSONObject();
  //         System.out.println("\nEnter the name of the meme you would like to find:");
  //         userInput = stdin.readLine();
  //         req.put("type", "find meme");
  //         req.put("name", userInput);
  //         // send request
  //         out.writeObject(req.toString());
  //         // receive a response
  //         String serverResponse = (String)in.readObject();
  //         JSONObject res = new JSONObject(serverResponse);
  //         // check the response
  //         if (res.getBoolean("success") == true) { // success
  //           System.out.println("\nFound meme \"" + res.getJSONObject("data").getString("name") + "\"");
  //           System.out.println("Would you like to select this meme? (y/n)");
  //           userInput = stdin.readLine();
  //           if (userInput.equalsIgnoreCase("y") || userInput.equalsIgnoreCase("yes")) {
  //             selectedMeme = new JSONObject(res.toString()).getJSONObject("data");
  //             System.out.println("\nSelected meme: " + selectedMeme.toString());
  //           }
  //         } else { // fail
  //           System.out.println("\nSorry, couldn't find that meme.");
  //         }
  //       }
  //
  //       // View all Memes
  //       else if (userInput.equalsIgnoreCase("View Memes") || userInput.equals("2")) {
  //         JSONObject req = new JSONObject();
  //         req.put("type", "get memes");
  //         // send request
  //         out.writeObject(req.toString());
  //         // receive a response
  //         String serverResponse = (String)in.readObject();
  //         JSONObject res = new JSONObject(serverResponse);
  //         // check the response
  //         if (res.getBoolean("success") == true) { // success
  //           JSONArray memes = res.getJSONArray("data");
  //           System.out.println();
  //           for (int i = 0; i < memes.length(); i++) {
  //             JSONObject currMeme = memes.getJSONObject(i);
  //             System.out.println(currMeme.getString("id") + ": " + currMeme.getString("name"));
  //           }
  //         } else { // fail
  //           System.out.println("\nThere was an error getting the list of memes.");
  //         }
  //       }
  //
  //       // create meme
  //       else if (userInput.equalsIgnoreCase("Create Meme") || userInput.equals("3")) {
  //         if (selectedMeme == null) {
  //           System.out.println("\nPlease select a meme first using option 1: Find Meme");
  //         } else {
  //           System.out.println("\nCreating a \"" + selectedMeme.getString("name") + "\" meme...");
  //           JSONObject req = new JSONObject();
  //           req.put("type", "create meme");
  //           req.put("template_id", selectedMeme.getString("id"));
  //           int textBoxCount = selectedMeme.getInt("box_count");
  //           req.put("box_count", textBoxCount);
  //           if (textBoxCount <= 2) {
  //             for (int i = 0; i < textBoxCount; i++) {
  //               System.out.print("Box " + Integer.toString(i + 1) +": ");
  //               userInput = stdin.readLine();
  //               req.put("text" + Integer.toString(i), userInput);
  //             }
  //           } else {
  //             JSONArray boxes = new JSONArray();
  //             for (int i = 0; i < textBoxCount; i++) {
  //               System.out.print("Box " + Integer.toString(i + 1) +": ");
  //               userInput = stdin.readLine();
  //               boxes.put(i, userInput);
  //             }
  //             req.put("boxes", boxes);
  //           }
  //
  //           // send request
  //           out.writeObject(req.toString());
  //           // receive a response
  //           String serverResponse = (String)in.readObject();
  //           JSONObject res = new JSONObject(serverResponse);
  //           if (res.getBoolean("success") == true) {
  //             BufferedImage image = decodeImage(res.getString("image"));
  //             displayImage(image);
  //             System.out.println("\nWould you like to save this meme? (y/n)");
  //             userInput = stdin.readLine();
  //             if (userInput.equalsIgnoreCase("y") || userInput.equalsIgnoreCase("yes")) {
  //               System.out.print("Name the png (without .png extension): ");
  //               userInput = stdin.readLine();
  //               try {
  //                 saveImage(userInput, image);
  //               } catch (Exception ex) {
  //                 System.out.println("There was an error saving the image...");
  //               }
  //             }
  //           } else {
  //             System.out.println(res.getString("message"));
  //           }
  //         }
  //       }
  //       // bad user input
  //       else {
  //         System.out.println("\nSorry, I didn't understand that input...");
  //       }
  //     }
  //
  //   } catch (Exception ex) {
  //     ex.printStackTrace();
  //   }
  // }

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
