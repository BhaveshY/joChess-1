/*
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Authors:
 * Mateusz Sławomir Lach ( matlak, msl )
 * Damian Marciniak
 */
package jchess;

import java.awt.*;
import java.net.*;
import java.io.*;
// import java.io.InputStreamReader;
// import javax.swing.*;
// import javax.swing.JPanel;
import java.io.IOException;
import java.util.Properties;
import java.io.FileOutputStream;
// import java.util.logging.Logger;

/** Class representing the game interface which is seen by a player and
 * where are lockated available for player opptions, current games and where
 * can he start a new game (load it or save it)
 */
public class GUI
{
    // The main game instance associated with this GUI
    public Game game;
    
    // Static configuration file loaded at startup
    static final public Properties configFile = GUI.getConfigFile();

    /**
     * Constructor - initializes a new game instance
     */
    public GUI()
    {
        this.game = new Game();
        //this.drawGUI();
    }

    /**
     * Method to load image by a given name with extension
     * @param name : string of image to load, e.g. "chessboard.jpg"
     * @return : image or null if cannot load
     */
    static Image loadImage(String name)
    {
        // Return null if config file wasn't loaded successfully
        if (configFile == null)
        {
            return null;
        }
        Image img = null;
        URL url = null;
        Toolkit tk = Toolkit.getDefaultToolkit();
        try
        {
            // Construct path to image based on theme from config
            String imageLink = "theme/" + configFile.getProperty("THEME", "default") + "/images/" + name;
            System.out.println(configFile.getProperty("THEME"));
            url = JChessApp.class.getResource(imageLink);
            if (url != null) {
                img = tk.getImage(url);
            } else {
                System.out.println("Resource not found: " + imageLink);
            }
        }
        catch (Exception e)
        {
            System.out.println("Error loading image: " + e.getMessage());
            e.printStackTrace();
        }
        return img;
    }

    /**
     * Validates if a theme name is valid
     * TODO: Implement actual theme validation logic
     */
    static boolean themeIsValid(String name)
    {
        return true;
    }

    /**
     * Gets the path to the JAR file containing this application
     * Handles spaces in path and removes trailing separators
     */
    static String getJarPath()
    {
        String path = GUI.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        path = path.replaceAll("[a-zA-Z0-9%!@#$%^&*\\(\\)\\[\\]\\{\\}\\.\\,\\s]+\\.jar", "");
        int lastSlash = path.lastIndexOf(File.separator); 
        if(path.length()-1 == lastSlash)
        {
            path = path.substring(0, lastSlash);
        }
        path = path.replace("%20", " ");
        return path;
    }

    /**
     * Loads or creates the configuration file
     * First tries to load default config from resources,
     * then creates config file if it doesn't exist,
     * finally loads the user's config file
     */
    static Properties getConfigFile()
    {
        Properties defConfFile = new Properties();
        Properties confFile = new Properties();
        File outFile = new File(GUI.getJarPath() + File.separator + "config.txt");
        try
        {
            // Load default config from resources
            defConfFile.load(GUI.class.getResourceAsStream("config.txt"));
        }
        catch (IOException exc)
        {
            System.out.println("Error loading default config: " + exc.getMessage());
            exc.printStackTrace();
        }
        if (!outFile.exists())
        {
            try
            {
                // Create new config file from defaults if it doesn't exist
                defConfFile.store(new FileOutputStream(outFile), null);
            }
            catch (IOException exc)
            {
                System.out.println("Error creating config file: " + exc.getMessage());
                exc.printStackTrace();
            }
        }
        try
        {   
            // Load the user's config file
            confFile.load(new FileInputStream(outFile));
        }
        catch (IOException exc)
        {
            System.out.println("Error loading config file: " + exc.getMessage());
            exc.printStackTrace();
        }
        return confFile;
    }
}
