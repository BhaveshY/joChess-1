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

import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.util.Calendar;
import java.awt.event.ComponentListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Class responsible for the starts of new games, loading games,
 * saving it, and for ending it.
 * This class is also responsible for appoing player with have
 * a move at the moment
 */
public class Game extends JPanel implements MouseListener, ComponentListener
{

    public Settings settings; // Holds the game settings
    public boolean blockedChessboard; // Indicates if the chessboard is blocked for user interaction
    public Chessboard chessboard; // The chessboard component
    private Player activePlayer; // The player who is currently active
    public GameClock gameClock; // The game clock component
    public Client client; // The client for network play
    public Moves moves; // The moves history component
    public Chat chat; // The chat component

    Game()
    {
        this.setLayout(null); // Set layout to null for absolute positioning
        this.moves = new Moves(this); // Initialize moves history
        settings = new Settings(); // Load game settings
        chessboard = new Chessboard(this.settings, this.moves); // Create chessboard with settings and moves
        chessboard.setVisible(true); // Make chessboard visible
        chessboard.setSize(Chessboard.img_height, Chessboard.img_width); // Set chessboard size
        chessboard.addMouseListener(this); // Add mouse listener for chessboard interactions
        chessboard.setLocation(new Point(0, 0)); // Set chessboard location
        this.add(chessboard); // Add chessboard to the panel
        gameClock = new GameClock(this); // Initialize game clock
        gameClock.setSize(new Dimension(200, 100)); // Set game clock size
        gameClock.setLocation(new Point(500, 0)); // Set game clock location
        this.add(gameClock); // Add game clock to the panel

        JScrollPane movesHistory = this.moves.getScrollPane(); // Get scroll pane for moves history
        movesHistory.setSize(new Dimension(180, 350)); // Set size for moves history
        movesHistory.setLocation(new Point(500, 121)); // Set location for moves history
        this.add(movesHistory); // Add moves history to the panel

        this.chat = new Chat(); // Initialize chat component
        this.chat.setSize(new Dimension(380, 100)); // Set chat size
        this.chat.setLocation(new Point(0, 500)); // Set chat location
        this.chat.setMinimumSize(new Dimension(400, 100)); // Set minimum size for chat

        this.blockedChessboard = false; // Initialize chessboard as not blocked
        this.setLayout(null); // Set layout to null for absolute positioning
        this.addComponentListener(this); // Add component listener for resizing
        this.setDoubleBuffered(true); // Enable double buffering for smoother rendering
    }

    /** Method to save actual state of game
     * @param path address of place where game will be saved
     */
    public void saveGame(File path)
    {
        File file = path; // Set the file to save the game
        FileWriter fileW = null; // Initialize FileWriter
        try
        {
            fileW = new FileWriter(file); // Create FileWriter for the specified file
        }
        catch (java.io.IOException exc)
        {
            System.err.println("error creating fileWriter: " + exc); // Log error if FileWriter creation fails
            JOptionPane.showMessageDialog(this, Settings.lang("error_writing_to_file")+": " + exc); // Show error message
            return; // Exit method on error
        }
        Calendar cal = Calendar.getInstance(); // Get current date and time
        String str = new String(""); // Initialize string to hold game data
        String info = new String("[Event \"Game\"]\n[Date \"" + cal.get(cal.YEAR) + "." + (cal.get(cal.MONTH) + 1) + "." + cal.get(cal.DAY_OF_MONTH) + "\"]\n"
                + "[White \"" + this.settings.playerWhite.name + "\"]\n[Black \"" + this.settings.playerBlack.name + "\"]\n\n"); // Prepare game info
        str += info; // Append game info to string
        str += this.moves.getMovesInString(); // Append moves history to string
        try
        {
            fileW.write(str); // Write game data to file
            fileW.flush(); // Flush the writer
            fileW.close(); // Close the writer
        }
        catch (java.io.IOException exc)
        {
            System.out.println("error writing to file: " + exc); // Log error if writing fails
            JOptionPane.showMessageDialog(this, Settings.lang("error_writing_to_file")+": " + exc); // Show error message
            return; // Exit method on error
        }
        JOptionPane.showMessageDialog(this, Settings.lang("game_saved_properly")); // Notify user that game was saved successfully
    }

    /** Loading game method(loading game state from the earlier saved file)
     *  @param file File where is saved game
     */

    /*@Override
    public void setSize(int width, int height) {
    Dimension min = this.getMinimumSize();
    if(min.getHeight() < height && min.getWidth() < width) {
    super.setSize(width, height);
    } else if(min.getHeight() < height) {
    super.setSize(width, (int)min.getHeight());
    } else if(min.getWidth() < width) {
    super.setSize((int)min.getWidth(), height);
    } else {
    super.setSize(width, height);
    }
    }*/
    static public void loadGame(File file)
    {
        FileReader fileR = null; // Initialize FileReader
        try
        {
            fileR = new FileReader(file); // Create FileReader for the specified file
        }
        catch (java.io.IOException exc)
        {
            System.out.println("Something wrong reading file: " + exc); // Log error if reading fails
            return; // Exit method on error
        }
        BufferedReader br = new BufferedReader(fileR); // Create BufferedReader for reading file
        String tempStr = new String(); // Initialize temporary string for
        String blackName, whiteName; // Initialize player names
        try
        {
            tempStr = getLineWithVar(br, new String("[White")); // Get white player's name
            whiteName = getValue(tempStr);
            tempStr = getLineWithVar(br, new String("[Black")); // Get black player's name
            blackName = getValue(tempStr);
            tempStr = getLineWithVar(br, new String("1.")); // Get game type
        }
        catch (ReadGameError err)
        {
            System.out.println("Error reading file: " + err); // Log error if reading fails
            return; // Exit method on error
        }
        Game newGUI = JChessApp.jcv.addNewTab(whiteName + " vs. " + blackName); // Create new game tab
        Settings locSetts = newGUI.settings; // Get settings for the new game
        locSetts.playerBlack.name = blackName; // Set black player's name
        locSetts.playerWhite.name = whiteName; // Set white player's name
        locSetts.playerBlack.setType(Player.playerTypes.localUser); // Set black player type
        locSetts.playerWhite.setType(Player.playerTypes.localUser); // Set white player type
        locSetts.gameMode = Settings.gameModes.loadGame; // Set game mode
        locSetts.gameType = Settings.gameTypes.local; // Set game type

        newGUI.newGame(); // Start new game
        newGUI.blockedChessboard = true; // Block chessboard for user interaction
        newGUI.moves.setMoves(tempStr); // Set moves history
        newGUI.blockedChessboard = false; // Unblock chessboard
        newGUI.chessboard.repaint(); // Repaint chessboard
        //newGUI.chessboard.draw();
    }

    /** Method checking in with of line there is an error
     *  @param  br BufferedReader class object to operate on
     *  @param  srcStr String class object with text which variable you want to get in file
     *  @return String with searched variable in file (whole line)
     *  @throws ReadGameError class object when something goes wrong when reading file
     */
    static public String getLineWithVar(BufferedReader br, String srcStr) throws ReadGameError
    {
        String str = new String();
        while (true)
        {
            try
            {
                str = br.readLine();
            }
            catch (java.io.IOException exc)
            {
                System.out.println("Something wrong reading file: " + exc);
            }
            if (str == null)
            {
                throw new ReadGameError();
            }
            if (str.startsWith(srcStr))
            {
                return str;
            }
        }
    }

    /** Method to get value from loaded txt line
     *  @param line Line which is readed
     *  @return result String with loaded value
     *  @throws ReadGameError object class when something goes wrong
     */
    static public String getValue(String line) throws ReadGameError
    {
        //System.out.println("getValue called with: "+line);
        int from = line.indexOf("\"");
        int to = line.lastIndexOf("\"");
        int size = line.length() - 1;
        String result = new String();
        if (to < from || from > size || to > size || to < 0 || from < 0)
        {
            throw new ReadGameError();
        }
        try
        {
            result = line.substring(from + 1, to);
        }
        catch (java.lang.StringIndexOutOfBoundsException exc)
        {
            System.out.println("error getting value: " + exc);
            return "none";
        }
        return result;
    }

    /** Method to Start new game
     *
     */
    public void newGame()
    {
        chessboard.setPieces("", settings.playerWhite, settings.playerBlack);

        //System.out.println("new game, game type: "+settings.gameType.name());

        activePlayer = settings.playerWhite;
        if (activePlayer.playerType != Player.playerTypes.localUser)
        {
            this.blockedChessboard = true;
        }
        SwingUtilities.invokeLater(() -> {
            chessboard.resizeChessboard(chessboard.get_height(true));
            chessboard.repaint();
            this.repaint();  // fixed the marker issue
        });
        //dirty hacks starts over here :) 
        //to fix rendering artefacts on first run
        // Game activeGame = JChessApp.jcv.getActiveTabGame();
        // if( activeGame != null && JChessApp.jcv.getNumberOfOpenedTabs() == 0 )
        // {
        //     activeGame.chessboard.resizeChessboard(activeGame.chessboard.get_height(true));
        //     activeGame.chessboard.repaint();
        //     activeGame.repaint();
        // }
        // chessboard.repaint();
        // this.repaint();
        //dirty hacks ends over here :)
    }

    /** Method to end game
     *  @param message what to show player(s) at end of the game (for example "draw", "black wins" etc.)
     */
    public void endGame(String massage)
    {
        this.blockedChessboard = true;
        System.out.println(massage);
        JOptionPane.showMessageDialog(null, massage);
    }

    /** Method to swich active players after move
     */
    public void switchActive()
    {
        if (activePlayer == settings.playerWhite)
        {
            activePlayer = settings.playerBlack;
        }
        else
        {
            activePlayer = settings.playerWhite;
        }

        this.gameClock.switch_clocks();
    }

    /** Method of getting accualy active player
     *  @return  player The player which have a move
     */
    public Player getActivePlayer()
    {
        return this.activePlayer;
    }

    /** Method to go to next move (checks if game is local/network etc.)
     */
    public void nextMove()
    {
        switchActive();

        System.out.println("next move, active player: " + activePlayer.name + ", color: " + activePlayer.color.name() + ", type: " + activePlayer.playerType.name());
        if (activePlayer.playerType == Player.playerTypes.localUser)
        {
            this.blockedChessboard = false;
        }
        else if (activePlayer.playerType == Player.playerTypes.networkUser)
        {
            this.blockedChessboard = true;
        }
        else if (activePlayer.playerType == Player.playerTypes.computer)
        {
        }
    }

    /** Method to simulate Move to check if it's correct etc. (usable for network game).
     * @param beginX from which X (on chessboard) move starts
     * @param beginY from which Y (on chessboard) move starts
     * @param endX   to   which X (on chessboard) move go
     * @param endY   to   which Y (on chessboard) move go
     * */
    public boolean simulateMove(int beginX, int beginY, int endX, int endY)
    {
        try 
        {
            chessboard.select(chessboard.squares[beginX][beginY]);
            if (chessboard.activeSquare.piece.allMoves().indexOf(chessboard.squares[endX][endY]) != -1) //move
            {
                chessboard.move(chessboard.squares[beginX][beginY], chessboard.squares[endX][endY]);
            }
            else
            {
                System.out.println("Bad move");
                return false;
            }
            chessboard.unselect();
            nextMove();

            return true;
            
        } 
        catch(StringIndexOutOfBoundsException exc) 
        {
            return false;
        }    
        catch(ArrayIndexOutOfBoundsException exc) 
        {
            return false;
        }
        catch(NullPointerException exc)
        {
            return false;
        }
        finally
        {
            Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, "ERROR");
        }
    }

    // MouseListener:
    public void mouseClicked(MouseEvent arg0)
    {
    }
    
    public boolean undo()
    {
        boolean status = false;
        
        if( this.settings.gameType == Settings.gameTypes.local )
        {
            status = chessboard.undo();
            if( status )
            {
                this.switchActive();
            }
            else
            {
                chessboard.repaint();//repaint for sure
            }
        }
        else if( this.settings.gameType == Settings.gameTypes.network )
        {
            this.client.sendUndoAsk();
            status = true;
        }
        return status;
    }
    
    public boolean rewindToBegin()
    {
        boolean result = false;
        
        if( this.settings.gameType == Settings.gameTypes.local )
        {
            while( chessboard.undo() )
            {
                result = true;
            }
        }
        else
        {
            throw new UnsupportedOperationException( Settings.lang("operation_supported_only_in_local_game") );
        }
        
        return result;
    }
    
    public boolean rewindToEnd() throws UnsupportedOperationException
    {
        boolean result = false;
        
        if( this.settings.gameType == Settings.gameTypes.local )
        {
            while( chessboard.redo() )
            {
                result = true;
            }
        }
        else
        {
            throw new UnsupportedOperationException( Settings.lang("operation_supported_only_in_local_game") );
        }
        
        return result;
    }
    
    public boolean redo()
    {
        boolean status = chessboard.redo();
        if( this.settings.gameType == Settings.gameTypes.local )
        {
            if ( status )
            {
                this.nextMove();
            }
            else
            {
                chessboard.repaint();//repaint for sure
            }
        }
        else
        {
            throw new UnsupportedOperationException( Settings.lang("operation_supported_only_in_local_game") );
        }
        return status;
    }
    
    

    public void mousePressed(MouseEvent event)
    {
        if (event.getButton() == MouseEvent.BUTTON3) //right button
        {
            this.undo();
        }
        else if (event.getButton() == MouseEvent.BUTTON2 && settings.gameType == Settings.gameTypes.local)
        {
            this.redo();
        }
        else if (event.getButton() == MouseEvent.BUTTON1) //left button
        {

            if (!blockedChessboard)
            {
                try 
                {
                    int x = event.getX();//get X position of mouse
                    int y = event.getY();//get Y position of mouse

                    Square sq = chessboard.getSquare(x, y);
                    if (sq.piece != null && sq.piece.player == this.activePlayer && sq != chessboard.activeSquare)
                    {
                        chessboard.unselect();
                        chessboard.select(sq);
                    } 
                    else if (chessboard.activeSquare == sq) //unselect
                    {
                        chessboard.unselect();
                    }
                    else if (chessboard.activeSquare != null && chessboard.activeSquare.piece != null
                            && chessboard.activeSquare.piece.allMoves().indexOf(sq) != -1) //move
                    {
                        if (settings.gameType == Settings.gameTypes.local)
                        {
                            chessboard.move(chessboard.activeSquare, sq);
                        }
                        else if (settings.gameType == Settings.gameTypes.network)
                        {
                            client.sendMove(chessboard.activeSquare.pozX, chessboard.activeSquare.pozY, sq.pozX, sq.pozY);
                            chessboard.move(chessboard.activeSquare, sq);
                        }
                        chessboard.unselect();
                        //switch player
                        this.nextMove();
                        //checkmate or stalemate
                        King king;
                        if (this.activePlayer == settings.playerWhite)
                        {
                            king = chessboard.kingWhite;
                        }
                        else
                        {
                            king = chessboard.kingBlack;
                        }

                        switch (king.isCheckmatedOrStalemated())
                        {
                            case 1:
                                this.endGame("Checkmate! " + king.player.color.toString() + " player lose!");
                                break;
                            case 2:
                                this.endGame("Stalemate! Draw!");
                                break;
                        }
                    }
                } 
                catch(NullPointerException exc) {
                    System.err.println(exc.getMessage());
                    chessboard.repaint();
                }
            }
            else if (blockedChessboard)
            {
                System.out.println("Chessboard is blocked");
            }
        }
        //chessboard.repaint();
    }

    public void mouseReleased(MouseEvent arg0)
    {
    }

    public void mouseEntered(MouseEvent arg0)
    {
    }

    public void mouseExited(MouseEvent arg0)
    {
    }

    public void componentResized(ComponentEvent e)
    {
        int height = this.getHeight() >= this.getWidth() ? this.getWidth() : this.getHeight();
        int chess_height = (int)Math.round( (height * 0.8)/8 )*8;
        this.chessboard.resizeChessboard((int)chess_height);
        chess_height = this.chessboard.getHeight();
        this.moves.getScrollPane().setLocation(new Point(chess_height + 5, 100));
        this.moves.getScrollPane().setSize(this.moves.getScrollPane().getWidth(), chess_height - 100);
        this.gameClock.setLocation(new Point(chess_height + 5, 0));
        if (this.chat != null)
        {
            this.chat.setLocation(new Point(0, chess_height + 5));
            this.chat.setSize(new Dimension(chess_height, this.getHeight() - (chess_height + 5))); 
        }
    }

    public void componentMoved(ComponentEvent e)
    {
    }

    public void componentShown(ComponentEvent e)
    {
    }

    public void componentHidden(ComponentEvent e)
    {
    }
}

class ReadGameError extends Exception
{
}
