package gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.WindowConstants;

import org.json.*;

import javax.imageio.*;
import java.awt.image.*;
import java.util.Base64;

/**
* The ClientGui class is a GUI frontend that displays an image grid, an input text box,
* a button, and a text area for status.
*
* Methods of Interest
* ----------------------
* show(boolean modal) - Shows the GUI frame with the current state
*     -> modal means that it opens the GUI and suspends background processes. Processing
*        still happens in the GUI. If it is desired to continue processing in the
*        background, set modal to false.
* newGame(int dimension) - Start a new game with a grid of dimension x dimension size
* insertImage(String filename, int row, int col) - Inserts an image into the grid
* appendOutput(String message) - Appends text to the output panel
* submitClicked() - Button handler for the submit button in the output panel
*
* Notes
* -----------
* > Does not show when created. show() must be called to show he GUI.
*
*/
public class ClientGui implements gui.OutputPanel.EventHandlers {
  JDialog frame;
  PicturePanel picturePanel;
  OutputPanel outputPanel;
  public Client client;
  private States state;
  public JSONObject selectedMeme;
  public int currentBox;
  public JSONObject createMemeReq;

  private enum States {
    // User selecting from menu of choices
    MENU_CHOICE,
    // Choosing the name of the meme
    SELECT_MEME_NAME,
    // Providing text to meme boxes
    ADDING_MEME_TEXT
  }

  /**
  * Construct dialog
  */
  public ClientGui() {
    createMemeReq = null;
    currentBox = 0;
    selectedMeme = null;
    state = States.MENU_CHOICE;
    frame = new JDialog();
    frame.setLayout(new GridBagLayout());
    frame.setMinimumSize(new Dimension(500, 500));
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    // setup the top picture frame
    picturePanel = new PicturePanel();
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.weighty = 0.25;
    frame.add(picturePanel, c);

    // setup the input, button, and output area
    c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 1;
    c.weighty = 0.75;
    c.weightx = 1;
    c.fill = GridBagConstraints.BOTH;
    outputPanel = new OutputPanel();
    outputPanel.addEventHandlers(this);
    frame.add(outputPanel, c);
  }

  /**
  * Shows the current state in the GUI
  * @param makeModal - true to make a modal window, false disables modal behavior
  */
  public void show(boolean makeModal) {
    frame.pack();
    frame.setModal(makeModal);
    frame.setVisible(true);
  }

  public void refreshPicturePanel() {
    picturePanel.invalidate();
    picturePanel.validate();
    picturePanel.repaint();
  }

  public void printMenu() {
    outputPanel.appendOutput("\nChoose an option...");
    outputPanel.appendOutput("1: Find Meme");
    outputPanel.appendOutput("2: View Memes");
    outputPanel.appendOutput("3: Create Meme");
    outputPanel.appendOutput("0: Quit");
  }

  /**
  * Creates a new game and set the size of the grid
  * @param dimension - the size of the grid will be dimension x dimension
  */
  public void newGame(int dimension) {
    picturePanel.newGame(dimension);
    // outputPanel.appendOutput("Started new game with a " + dimension + "x" + dimension + " board.");
    outputPanel.appendOutput("Welcome to the TCP Meme-maker Client!");
    printMenu();
  }

  /**
  * Insert an image into the grid at position (col, row)
  *
  * @param filename - filename relative to the root directory
  * @param row - the row to insert into
  * @param col - the column to insert into
  * @return true if successful, false if an invalid coordinate was provided
  * @throws IOException An error occured with your image file
  */
  public boolean insertImage(String filename, int row, int col) throws IOException {
    String error = "";
    try {
      // insert the image
      if (picturePanel.insertImage(filename, row, col)) {
        // put status in output
        outputPanel.appendOutput("Inserting " + filename + " in position (" + row + ", " + col + ")");
        return true;
      }
      error = "File(\"" + filename + "\") not found.";
    } catch(PicturePanel.InvalidCoordinateException e) {
      // put error in output
      error = e.toString();
    }
    outputPanel.appendOutput(error);
    return false;
  }

  /**
  * Insert an image into the grid at position (col, row)
  *
  * @param img - BufferedImage to insert
  * @param row - the row to insert into
  * @param col - the column to insert into
  * @return true if successful, false if an invalid coordinate was provided
  * @throws IOException An error occured with your image file
  */
  public boolean insertImage(BufferedImage img, int row, int col) throws IOException {
    String error = "";
    try {
      // insert the image
      if (picturePanel.insertImage(img, row, col)) {
        // put status in output
        // outputPanel.appendOutput("Inserting " + filename + " in position (" + row + ", " + col + ")");
        return true;
      }
      error = "Problem inserting the image";
    } catch(PicturePanel.InvalidCoordinateException e) {
      // put error in output
      error = e.toString();
    }
    outputPanel.appendOutput(error);
    return false;
  }

  /**
  * Submit button handling
  *
  * Change this to whatever you need
  */
  @Override
  public void submitClicked() {
    try {
      // Pulls the input box text
      String userInput = outputPanel.getInputText();
      // if has input
      if (userInput.length() > 0) {

        if (this.state == States.MENU_CHOICE) {
          if (userInput.equalsIgnoreCase("quit") || userInput.equals("0")) {
            outputPanel.appendOutput("\nQuit selected");
            this.state = States.MENU_CHOICE;
            printMenu();
          }
          else if (userInput.equalsIgnoreCase("Find Meme") || userInput.equals("1")) {
            outputPanel.appendOutput("\nFind Meme selected");
            outputPanel.appendOutput("Enter the name of the meme you would like to find:");
            this.state = States.SELECT_MEME_NAME;
          }
          else if (userInput.equalsIgnoreCase("View Memes") || userInput.equals("2")) {
            outputPanel.appendOutput("\nView Memes selected");
            JSONObject req = new JSONObject();
            req.put("type", "get memes");
            JSONObject res = client.sendRequest(req);
            // outputPanel.appendOutput(res.toString(1));
            if (res.getBoolean("success") == true) { // success
              JSONArray memes = res.getJSONArray("data");
              for (int i = 0; i < memes.length(); i++) {
                JSONObject currMeme = memes.getJSONObject(i);
                outputPanel.appendOutput(currMeme.getString("id") + ": " + currMeme.getString("name"));
              }
            } else { // fail
              System.out.println("\nThere was an error getting the list of memes.");
            }
            this.state = States.MENU_CHOICE;
            printMenu();
          }
          else if (userInput.equalsIgnoreCase("Create Meme") || userInput.equals("3")) {
            outputPanel.appendOutput("\nCreate Meme selected");
            if (selectedMeme == null) {
              outputPanel.appendOutput("\nPlease select a meme first using option 1: Find Meme");
              this.state = States.MENU_CHOICE;
              printMenu();
            } else {
              this.state = States.ADDING_MEME_TEXT;
              this.currentBox = 0;
              this.createMemeReq = new JSONObject();
              outputPanel.appendOutput("Box " + Integer.toString(currentBox + 1) +": ");
            }

          }
          else {
            outputPanel.appendOutput("\nSorry, I didn't understand that input...");
            this.state = States.MENU_CHOICE;
            printMenu();
          }
        }
        else if (this.state == States.SELECT_MEME_NAME) {
          JSONObject req = new JSONObject();
          req.put("type", "find meme");
          req.put("name", userInput);
          JSONObject res = client.sendRequest(req);
          // check the response
          if (res.getBoolean("success") == true) { // success
            outputPanel.appendOutput("Found and selected the meme \"" + res.getJSONObject("data").getString("name") + "\"");
            BufferedImage image = Client.decodeImage(res.getString("image")); // ** NOT IN SI videos: let's insert the meme template image
            insertImage(image, 0, 0);
            selectedMeme = new JSONObject(res.toString()).getJSONObject("data");
          } else { // fail
            outputPanel.appendOutput("Sorry, couldn't find that meme.");
          }
          this.state = States.MENU_CHOICE;
          printMenu();
        }
        else if (this.state == States.ADDING_MEME_TEXT) {

          if (currentBox == 0) {
            outputPanel.appendOutput("\nCreating a \"" + selectedMeme.getString("name") + "\" meme...");
            createMemeReq.put("type", "create meme");
            createMemeReq.put("template_id", selectedMeme.getString("id"));
            int textBoxCount = selectedMeme.getInt("box_count");
            createMemeReq.put("box_count", textBoxCount);
            createMemeReq.put("text" + Integer.toString(currentBox), userInput);
            currentBox++;
            outputPanel.appendOutput("Box " + Integer.toString(currentBox + 1) +": ");
          } else {
            createMemeReq.put("text" + Integer.toString(currentBox), userInput);
            JSONObject res = client.sendRequest(createMemeReq);
            outputPanel.appendOutput(res.toString(1));

            if (res.getBoolean("success") == true) {
              BufferedImage image = Client.decodeImage(res.getString("image"));
              insertImage(image, 0, 0);
            }

            this.state = States.MENU_CHOICE;
            printMenu();
          }

        }

        // clear input text box
        outputPanel.setInputText("");
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
  * Key listener for the input text box
  *
  * Change the behavior to whatever you need
  */
  @Override
  public void inputUpdated(String input) {
    if (input.equals("surprise")) {
      outputPanel.appendOutput("You found me!");
    }
  }

  public static void main(String[] args) throws IOException {
    // create the frame
    ClientGui main = new ClientGui();
    main.client = new Client("localhost", 8001);

    // prepare the GUI for display
    main.newGame(1); // we only need grid 1 for this assignment!

    // add images to the grid
    // main.insertImage("img/Pineapple-Upside-down-cake.jpg", 0, 0);

    // show the GUI dialog as modal
    main.show(true);

  }
}
