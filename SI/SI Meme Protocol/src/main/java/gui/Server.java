package gui;

import java.net.*;
import java.io.*;
import org.json.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.*;
import java.awt.image.*;
import java.util.Base64;

public class Server {

  //************************
  // main program code
  public static void main(String[] args) throws Exception {
    // get command line arguments (port)
    try {
      if (args.length != 1) {
        System.out.println("Argument count doesn't match...");
        System.out.println("Usage: gradle runServer -Pport=<port number>");
        System.exit(0);
      }
      int port = -1;
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException ex) {
        System.out.println("Error parsing port number: Port should be an int");
        System.exit(0);
      }

      // populate a JSONArray with fun stuff to work with
      JSONArray memes = getMemes();

      // open the port/create a socket
      ServerSocket sock = new ServerSocket(port);
      Socket clientSock;
      System.out.println("TCP Server is ready for connection...");

      // begin infinite while loop
      while (true) {
        // wait for client to connect
        try {
          clientSock = sock.accept();
          ObjectOutputStream out = new ObjectOutputStream(clientSock.getOutputStream());
          ObjectInputStream in = new ObjectInputStream(clientSock.getInputStream());
          System.out.println("A client has connected!");

          // wait for requests
          while (true) {

            // receive a new request
            String received = (String) in.readObject();
            // System.out.println("Received from client: " + received);
            JSONObject req = new JSONObject(received);
            JSONObject res = new JSONObject();

            // findMemeByName request
            if (req.getString("type").equals("find meme")) {
              System.out.println("Getting a meme...");
              JSONObject meme = findMemeByName(memes, req.getString("name"));
              res.put("type", "meme");
              if (meme == null) {
                res.put("success", false);
                System.out.println("Couldn't find the client's meme!");
              } else {
                res.put("type", "find meme");
                res.put("success", true);
                res.put("data", meme);
                BufferedImage image = getImage(meme.getString("url")); // ** NOT IN SI videos: let's send the meme template image too
                String encodedImage = encodeImage(image);
                res.put("image", encodedImage);
                System.out.println("Found the meme \"" + meme.getString("name") + "\"");
                System.out.println("Sending the meme to client...");
              }
            }

            // getMemes request
            else if (req.getString("type").equals("get memes")) {
              System.out.println("Sending the array of memes to the client...");
              res.put("type", "memes array");
              res.put("success", true);
              res.put("data", memes);
            }

            // createMeme request
            else if (req.getString("type").equals("create meme")) {
              System.out.println("Creating a meme...");
              res.put("type", "captioned meme");
              LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
              params.put("template_id", req.getString("template_id"));
              int textBoxCount = req.getInt("box_count");
              if (textBoxCount <=2) {
                for (int i = 0; i < textBoxCount; i++) {
                  params.put("text" + Integer.toString(i), req.getString("text" + Integer.toString(i)));
                }
              } else {
                for (int i = 0; i < textBoxCount; i++) {
                  params.put("boxes["+Integer.toString(i)+"][text]", req.getJSONArray("boxes").getString(i));
                }
              }
              JSONObject result = createMeme(params);
              if (result.getBoolean("success") == true) {
                res.put("success", true);
                BufferedImage image = getImage(result.getJSONObject("data").getString("url"));
                String encodedImage = encodeImage(image);
                res.put("image", encodedImage);
              } else {
                res.put("success", false);
                res.put("message", "There was an error creating your meme. Sorry!");
              }
              res.put("data", result);
            }

            // error - unrecognized request
            else {
              System.out.println("Error: didn't understand client's request...");
              res.put("type", "error");
              res.put("success", false);
              res.put("message", "Server didn't understand the request!");
            }

            // send the server's response
            out.writeObject(res.toString());
          }

          // keep listening for more clients
        } catch (Exception ex) {
          System.out.println(ex);
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }


  //************************
  // HTTP FUNCTIONS

  // decode an image from Base64 string
  // Performs HTTP GET request to https://api.imgflip.com/get_memes
  // Returns a JSONArray of memes to work with
  private static JSONArray getMemes() throws Exception {
    // do a HTTP GET request to https://api.imgflip.com/get_memes
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.imgflip.com/get_memes")).build();
    HttpResponse<String> response = client.send(request,
    HttpResponse.BodyHandlers.ofString());
    // return a JSONArray of memes
    return new JSONObject(response.body()).getJSONObject("data").getJSONArray("memes");
  }

  // Searches a JSONArray for a JSONObject by the value of "name" key
  private static JSONObject findMemeByName(JSONArray memes, String name) {
    // search the JSON meme array by name
    JSONObject meme = null;
    for (int i = 0; i < memes.length(); i++) {
      if (memes.getJSONObject(i).getString("name").equalsIgnoreCase(name)) {
        meme = memes.getJSONObject(i);
        break;
      }
    }
    return meme;
  }

  // Performs HTTP POST request to https://api.imgflip.com/caption_image
  // Takes a linked hash map of params and converts them to query string
  // All requests use the account SER321 with password 321memes
  // Returns a response JSONObject
  private static JSONObject createMeme(LinkedHashMap<String, String> values) throws Exception {
    // add username and password to values
    values.put("username", "SER321");
    values.put("password", "321memes");

    // convert values HashMap into http params string (name=steve&job=SI+Leader)
    StringBuilder params = new StringBuilder();
    for (Map.Entry<String,String> entry : values.entrySet()) {
      params.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
      params.append("=");
      params.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
      params.append("&");
    }

    // do a POST request to https://api.imgflip.com/caption_image
    HttpClient postClient = HttpClient.newHttpClient();
    HttpRequest postReq = HttpRequest.newBuilder()
    .uri(URI.create("https://api.imgflip.com/caption_image"))
    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
    .POST(HttpRequest.BodyPublishers.ofString(params.toString()))
    .build();
    HttpResponse<String> postRes = postClient.send(postReq,
    HttpResponse.BodyHandlers.ofString());

    // convert the response body to a JSONObject
    JSONObject json = new JSONObject(postRes.body());
    return json;
  }

  //************************
  // IMAGE FUNCTIONS

  // get a BufferedImage from a URL
  public static BufferedImage getImage(String imageURL) throws Exception {
    URL url = new URL(imageURL);
    return ImageIO.read(url);
  }

  // encode an image to a Base64 string
  public static String encodeImage(BufferedImage image) throws Exception {
    ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", byteOutStream);
    byte[] bytes = byteOutStream.toByteArray();
    Base64.Encoder encoder = Base64.getEncoder();
    return encoder.encodeToString(bytes);
  }

}
