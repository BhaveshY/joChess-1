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

// [Added] To use ListSelectionModel
import javax.swing.*;
 import java.awt.event.ActionListener;
 import java.awt.event.ActionEvent;
 import javax.swing.event.ListSelectionListener;
 import javax.swing.event.ListSelectionEvent;
 import java.io.File;
 import java.util.Properties;
 import java.io.FileOutputStream;
 import java.io.IOException; // [Added] Needed for handling IOExceptions

import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

 public class ThemeChooseWindow extends JDialog implements ActionListener, ListSelectionListener
 {

     JList<String> themesList; // [Changed] Specified JList with generic type <String>
     ImageIcon themePreview;
     GridBagLayout gbl;
     public String result;
     GridBagConstraints gbc;
     JButton themePreviewButton;
     JButton okButton;

     ThemeChooseWindow(Frame parent) throws Exception
    {
        super(parent);

        // Updated themePath to include package name if necessary
        String themePath = "/jchess/theme"; // [Changed] Updated resource path
        java.net.URL themeURL = getClass().getResource(themePath);
        if (themeURL == null) {
            throw new Exception(Settings.lang("error_when_creating_theme_config_window") + ": Theme directory not found in resources at path " + themePath);
        }

        System.out.println("Theme URL: " + themeURL);

        String[] themeNames = null;
        if (themeURL.getProtocol().equals("file")) {
            File dir = new File(themeURL.toURI());
            if (!dir.exists() || !dir.isDirectory()) {
                throw new Exception(Settings.lang("error_when_creating_theme_config_window") + ": Theme directory does not exist or is not a directory at " + dir.getPath());
            }

            File[] files = dir.listFiles(File::isDirectory);
            if (files != null && files.length > 0) {
                themeNames = new String[files.length];
                for (int i = 0; i < files.length; i++) {
                    themeNames[i] = files[i].getName();
                }
            } else {
                throw new Exception(Settings.lang("error_when_creating_theme_config_window") + ": No theme directories found in " + dir.getPath());
            }
        } else if (themeURL.getProtocol().equals("jar")) {
            themeNames = getResourceListing(themePath);
            if (themeNames == null || themeNames.length == 0) {
                throw new Exception(Settings.lang("error_when_creating_theme_config_window") + ": No theme directories found inside JAR at " + themePath);
            }
        } else {
            throw new Exception(Settings.lang("error_when_creating_theme_config_window") + ": Unsupported resource protocol " + themeURL.getProtocol());
        }

        System.out.println("Found themes: " + String.join(", ", themeNames));

        this.setTitle(Settings.lang("choose_theme_window_title"));
        Dimension winDim = new Dimension(550, 230);
        this.setMinimumSize(winDim);
        this.setMaximumSize(winDim);
        this.setSize(winDim);
        this.setResizable(false);
        this.setLayout(null);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);            

        // Initialize themesList with themeNames
        this.themesList = new JList<>(themeNames);
        if (this.themesList == null) {
            throw new Exception(Settings.lang("error_when_creating_theme_config_window") + ": Failed to create themes list");
        }
        this.themesList.setLocation(new Point(10, 10));
        this.themesList.setSize(new Dimension(150, 150)); // [Adjusted] Increased size for better visibility
        this.themesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.themesList.addListSelectionListener(this);
        this.add(this.themesList);

        Properties prp = GUI.getConfigFile();

        this.gbl = new GridBagLayout();
        this.gbc = new GridBagConstraints();
        
        String currentTheme = prp.getProperty("THEME", "default");
        this.themePreview = loadThemePreview(currentTheme);

        this.result = "";
        this.themePreviewButton = new JButton();
        if (this.themePreview != null) {
            this.themePreviewButton.setIcon(this.themePreview);
        }
        this.themePreviewButton.setLocation(new Point(170, 10)); // [Adjusted] Positioning next to themesList
        this.themePreviewButton.setSize(new Dimension(350, 150));
        this.add(this.themePreviewButton);

        this.okButton = new JButton("OK");
        this.okButton.setLocation(new Point(200, 170));
        this.okButton.setSize(new Dimension(150, 40));
        this.add(this.okButton);
        this.okButton.addActionListener(this);
        this.setModal(true);
    }

    /**
     * Retrieves a list of resources within the specified path.
     * Supports both file system and JAR protocols.
     * 
     * @param path The resource path.
     * @return An array of resource names.
     * @throws Exception If an error occurs while listing resources.
     */
    private String[] getResourceListing(String path) throws Exception {
        java.net.URL dirURL = getClass().getResource(path);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            return new File(dirURL.toURI()).list();
        } 

        if (dirURL == null) {
            String me = getClass().getName().replace(".", "/")+".class";
            dirURL = getClass().getResource(me);
        }
        
        if (dirURL.getProtocol().equals("jar")) {
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); 
            java.util.jar.JarFile jar = new java.util.jar.JarFile(java.net.URLDecoder.decode(jarPath, "UTF-8"));
            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
            java.util.Set<String> result = new java.util.HashSet<>();

            while(entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path.substring(1))) { // remove leading "/"
                    String entry = name.substring(path.length());
                    int checkSubdir = entry.indexOf("/");
                    if (checkSubdir >= 0) {
                        entry = entry.substring(0, checkSubdir);
                    }
                    if (entry.length() > 0) {
                        result.add(entry);
                    }
                }
            }
            jar.close();
            return result.toArray(new String[result.size()]);
        } 
        
        throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
    }

    /**
     * Loads the preview image for the specified theme.
     * 
     * @param themeName The name of the theme.
     * @return An ImageIcon of the preview, or null if not found.
     */
    private ImageIcon loadThemePreview(String themeName) {
        String previewPath = "/jchess/theme/" + themeName + "/images/Preview.png"; // [Changed] Updated path to include package
        java.net.URL previewURL = getClass().getResource(previewPath);
        if (previewURL != null) {
            return new ImageIcon(previewURL);
        } else {
            System.out.println("Cannot find preview image for theme: " + themeName + ", at path: " + previewPath);
            java.net.URL noPreviewURL = getClass().getResource("/jchess/theme/noPreview.png"); // [Changed] Updated path to include package
            return noPreviewURL != null ? new ImageIcon(noPreviewURL) : null;
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent event)
    {
        if (!event.getValueIsAdjusting() && themesList.getSelectedIndex() != -1) {
            String element = themesList.getSelectedValue();
            ImageIcon preview = loadThemePreview(element);
            if (preview != null) {
                this.themePreview = preview;
                this.themePreviewButton.setIcon(this.themePreview);
            } else {
                this.themePreviewButton.setIcon(null);
            }
        }
    }

     /** Handles action events such as button clicks
      * @param evt Contains information about the performed action
      */
     public void actionPerformed(ActionEvent evt)
     {
         if (evt.getSource() == this.okButton)
         {
             Properties prp = GUI.getConfigFile();
             int element = this.themesList.getSelectedIndex();
             if (element == -1) { // [Added] Check if no theme is selected
                 JOptionPane.showMessageDialog(this, Settings.lang("no_theme_selected"));
                 return;
             }
             String name = this.themesList.getModel().getElementAt(element).toString();
             if (GUI.themeIsValid(name))
             {
                 prp.setProperty("THEME", name);
                 try (FileOutputStream fOutStr = new FileOutputStream("config.txt")) { // [Changed] Use try-with-resources to ensure stream is closed
                     prp.store(fOutStr, null);
                 }
                 catch (IOException exc) // [Changed] Catch specific IOException
                 {
                     JOptionPane.showMessageDialog(this, Settings.lang("error_saving_config") + ": " + exc.getMessage()); // [Added] Notify user of save error
                 }
                 JOptionPane.showMessageDialog(this, Settings.lang("changes_visible_after_restart"));
                 this.setVisible(false);

             }
             else
             {
                 JOptionPane.showMessageDialog(this, Settings.lang("invalid_theme_selected")); // [Added] Notify user of invalid theme selection
             }
             System.out.print(prp.getProperty("THEME"));
         }
     }
 }
