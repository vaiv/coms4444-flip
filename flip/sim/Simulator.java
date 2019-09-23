/*
    Project: Flip
    Course: Programming & Problem Solving
    Year : 2019
    Instructor: Prof. Kenneth Ross
    URL: http://www.cs.columbia.edu/~kar/4444f19/

    Author: Vaibhav Darbari
    Simulator Version: 1.0
    
*/
package flip.sim;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Random;
import java.util.PriorityQueue;
import java.util.Scanner; 

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import flip.sim.Point;
import flip.sim.Board;
import javafx.util.Pair; 

import java.util.HashMap; 
import java.util.Map; 
import java.util.*;
import java.io.*;

public class Simulator
{
	private static final String root = "flip";
    private static final String statics_root = "statics";
	private static List<String> playerNames;
	private static boolean gui = false;
	private static double fps = 50;
	private static Integer n_pieces = 10;
	private static Integer seed = 42;
	private static Integer runs = 1;
	private static Integer turns = 200;
	private static boolean swap_players = false;
    private static long timeout = 1000;
    private static String version = "1.0";

    // state variables for gui

	private static PlayerWrapper player1;
    private static PlayerWrapper player2;

    private static Integer player1_score;
    private static Integer player2_score;

    private static List<Point> player1_pieces_list = new ArrayList<Point>();
    private static List<Point> player2_pieces_list = new ArrayList<Point>();

    private static Integer numTurns;
    private static Integer round;





	 public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException
	 {
	 	parseArgs(args);
       
        Log.log("parsing done");
	 	List<Pair<String, String>> pairs = new ArrayList<Pair<String, String>>();
	 	HashMap<String, Pair<Integer, Integer>> player_wins = new HashMap<String, Pair<Integer, Integer>>();

        HTTPServer server = null;
        if (gui) {
            server = new HTTPServer();
            Log.record("Hosting HTTP Server on " + server.addr());
            if (!Desktop.isDesktopSupported())
                Log.record("Desktop operations not supported");
            else if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                Log.record("Desktop browse operation not supported");
            else {
                try {
                    Desktop.getDesktop().browse(new URI("http://localhost:" + server.port()));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            
            // gui(server, state(fps));
        }

        for(Integer i=0;i<playerNames.size();i++)
        {
            System.out.println(playerNames.get(i));
            if (player_wins.containsKey(playerNames.get(i)))
            {
                Log.log("player at position " + i.toString() + " is named similarly to another player.");

                String mod_name = " ";
                for(Integer j=0;j<100;j++)
                {
                    mod_name = playerNames.get(i) + "_" + j.toString();
                    if (player_wins.containsKey(mod_name))
                        continue;
                    else
                        {
                            playerNames.set(i, mod_name);
                            break;
                        }
                }

                Log.log("player at position " + i.toString() + " is being renamed to " + mod_name );
            }

            player_wins.put(playerNames.get(i), new Pair<Integer, Integer>(0, 0));

        }

	 	for(Integer i=0;i< playerNames.size(); i++)
	 	{
            System.out.println(playerNames.get(i));
	 		for(int j = i+1; j<playerNames.size(); j++)
	 		{
	 			Pair<String, String> players = new Pair(playerNames.get(i),playerNames.get(j));
	 			pairs.add(players);
	 		}
	 	}

        
	 	for(Pair<String, String> pair : pairs)
	 	{
	 		swap_players = false;
	 		Log.log("###############################################################################################################");
	 		Log.log("games beginning for " + pair.getKey() + " and " + pair.getValue());

            round = 0;
	 		for(int i=0; i < runs; i++)
	 		{
                round++;
	 			try 
	 			{
            		player1 = loadPlayerWrapper(cleanName(pair.getKey()), pair.getKey());
            		player2 = loadPlayerWrapper(cleanName(pair.getValue()), pair.getValue());
       			} 
       			catch (Exception ex) 
       			{
            		Log.log("Unable to load players. " + ex.getMessage());
            		System.exit(0);
            	}

            	if(swap_players)
            	{
            		PlayerWrapper tmp = player1;
            		player1 = player2;
            		player2 = tmp;
            	}

            	Log.log("player 1 is" + player1.getName());
            	Log.log("player 2 is" + player2.getName());


            	Board game = new Board(n_pieces, seed+round);

                Log.log("Board setup complete.");

            	HashMap<Integer, Point> player1_pieces = game.get_pieces(true);
            	HashMap<Integer, Point> player2_pieces = game.get_pieces(false);



                numTurns = new Integer(turns);
                player1_score = 0;
                player2_score = 0;
                update_lists(player1_pieces, player2_pieces);

                 if (gui) 
                 {
                    gui(server, state(fps));
                 }

                try
                {
                    player1.init(deepClone(player1_pieces), n_pieces, turns, true, Board.get_diameter_piece());
                    player2.init(deepClone(player2_pieces), n_pieces, turns, false, Board.get_diameter_piece());
                }
                catch(Exception ex)
                {
                    Log.log("Unable to begin game, players failed to initialize. " + ex.getMessage());
                    continue;
                }

            	

            	Integer curr_score_player_1 = 0;
            	Integer curr_score_player_2 = 0;
            	PlayerWrapper winner;
            	Integer winner_score;

            	for(int j=0;j<turns;j++)
            	{
                    numTurns--;
            		if(j == 0)
            		{
            			play_turn(player1, player2, game, 1, true);
                        player1_score = curr_score_player_1;

                        if (gui) 
                         {
                            gui(server, state(fps));
                         }

            			play_turn(player1, player2, game, 2, false);
                        player2_score = curr_score_player_2;

                        if (gui) 
                         {
                            gui(server, state(fps));
                         }
            		}
            		else
            		{
            			curr_score_player_1  = Math.max(play_turn(player1, player2, game, 2, true), curr_score_player_1);
                        player1_score = curr_score_player_1;

            			if(curr_score_player_1 == n_pieces)
            			{
            				winner = player1;
            				break;
            			}

                         if (gui) 
                         {
                            gui(server, state(fps));
                         }

            			curr_score_player_2  = Math.max(play_turn(player1, player2, game, 2, false), curr_score_player_2);
                        player2_score = curr_score_player_2;

            			if(curr_score_player_2 == n_pieces)
            			{
            				winner = player2;
            				break;
            			}

                        if (gui) 
                         {
                            gui(server, state(fps));
                         }
            		}
            	}

            	if (curr_score_player_1 > curr_score_player_2) 
                    {
                        winner = player1;
                    }
                else if (curr_score_player_1 != curr_score_player_2)
                    {
                        winner = player2;
                    }
                else
                    winner = null;

            	Log.log("--------------------------------------------------------------------------------------------------------------");
            	Log.log("Player 1: "+player1.getName()+" scored " + curr_score_player_1.toString() + " points at the end of this round");
            	Log.log("Player 2: "+player2.getName()+" scored " + curr_score_player_2.toString() + " points at the end of this round");

            	if(winner == null)
            	{
            		Log.log("This round of the game ended in a tie.");
            	}
            	else
            	{
            		Log.log(winner.getName() + " won the round.");
            		if (winner == player1)
            		{
            			Pair<Integer,Integer> wins = player_wins.get(winner.getName());
            			player_wins.put(winner.getName(), new Pair<Integer, Integer>(wins.getKey()+1, wins.getValue()));
            		}
            		else
            		{
            			Pair<Integer,Integer> wins = player_wins.get(winner.getName());
            			player_wins.put(winner.getName(), new Pair<Integer, Integer>(wins.getKey(), wins.getValue()+1));
            		}
            	}

                if (gui) 
                 {
                    gui(server, state(fps));
                 }

            	swap_players = !swap_players;

                Log.log("--------------------------------------------------------------------------------------------------------------");
        	}


	 	}

	 	Log.log("All games concluded!");
	 	Log.log("----------------------------------------------Summary of results------------------------------------------");
	 	Log.log("player name \t wins as first \t wins as second");
	 	for (HashMap.Entry<String, Pair<Integer, Integer>> entry : player_wins.entrySet()) 
        {
           Log.log(entry.getKey() + "\t\t" + entry.getValue().getKey().toString() + "\t\t" + entry.getValue().getValue().toString());
        }

        Log.log("----------------------------------------------End of log.-------------------------------------------------");

        Log.end();

         if (gui) 
          {
             gui(server, state(fps));
             Scanner in = new Scanner(System.in); 
             String s = in.nextLine(); 
          }
        System.exit(0);

	 }

     private static String cleanName(String s)
     {
        String res = " ";
        if(s.contains("_"))
        {
            Integer idx = s.lastIndexOf("_");
            res = s.substring(0,idx);
        }
        else
            return s;

        return res;
     }
	 private static Integer play_turn(PlayerWrapper player1, PlayerWrapper player2, Board game, Integer num_moves, boolean isplayer1 )
	 {
	 	Integer score = 0;
	 	List<Pair<Integer, Point>> moves;
	 	HashMap<Integer, Point> player1_pieces = game.get_pieces(true);
        HashMap<Integer, Point> player2_pieces = game.get_pieces(false);

        try
        {
            if(isplayer1)
            {
                moves = player1.getMoves(num_moves,deepClone(player1_pieces), deepClone(player2_pieces), true);
            }
            else
            {
                moves = player2.getMoves(num_moves, deepClone(player2_pieces), deepClone(player1_pieces), false);
            }

            if(moves.size()>num_moves)
                    Log.record("More moves than requested returned. Only first "+ num_moves.toString()+ " were considered.");

            Integer i=0;
            for(Pair<Integer, Point> move : moves)
                {
                    i++;
                    if(i>num_moves) break;

                    if(game.check_valid_move(move, isplayer1))
                    {
                        Log.record("Player " + (isplayer1? "1":"2") + " moved piece "+ move.getKey() + " to "+ move.getValue());
                        game.move_piece(move, isplayer1);
                    }
                    else
                    {
                        Log.record("Player "+ (isplayer1? "1":"2") +" could not move piece "+ move.getKey() + " to "+ move.getValue() + ". Invalid move.");
                    }
                }

            score = game.get_score(isplayer1);

        }
        catch(Exception ex)
        {
            Log.record("Player " + (isplayer1? "1":"2") + "turn ended with an exception. "  + ex.getMessage());
        }
	 	
        update_lists(player1_pieces, player2_pieces);
	 	return score;

	 }

     private static void update_lists(HashMap<Integer, Point> player1_pieces, HashMap<Integer, Point> player2_pieces)
     {
        player1_pieces_list.clear();
        player2_pieces_list.clear();

         for (HashMap.Entry<Integer, Point> entry : player1_pieces.entrySet()) 
        {
            player1_pieces_list.add(entry.getValue());    
        }

         for (HashMap.Entry<Integer, Point> entry : player2_pieces.entrySet()) 
        {
            player2_pieces_list.add(entry.getValue());    
        }
     }

	 private static void parseArgs(String[] args) 
	 {
        int i = 0;
        playerNames = new ArrayList<String>();
        for (; i < args.length; ++i) {
            switch (args[i].charAt(0)) {
                case '-':
                    if (args[i].equals("-p") || args[i].equals("--players")) 
                    {
                        while (i + 1 < args.length && args[i + 1].charAt(0) != '-') 
                        {
                            ++i;
                            playerNames.add(args[i]);
                        }

                        if (playerNames.size() < 2) 
                        {
                            throw new IllegalArgumentException("Invalid number of players, you need atleast 2 players to start a game.");
                        }

                    } 
                    else if (args[i].equals("-g") || args[i].equals("--gui")) 
                    {
                        gui = true;
                    } 
                    else if (args[i].equals("-l") || args[i].equals("--logfile")) 
                    {
                        if (++i == args.length) 
                        {
                            throw new IllegalArgumentException("Missing logfile name");
                        }
                        Log.setLogFile(args[i]);
                        Log.activate();
                    } 
                     else if (args[i].equals("-v") || args[i].equals("--verbose")) 
                    {
                        Log.verbose();
                    } 
                    else if (args[i].equals("--fps")) 
                    {
                        if (++i == args.length) 
                        {
                            throw new IllegalArgumentException("Missing frames per second.");
                        }
                        fps = Double.parseDouble(args[i]);
                    } 
                    else if (args[i].equals("-n") || args[i].equals("--num_pieces")) 
                    {
                        if (++i == args.length) 
                        {
                            throw new IllegalArgumentException("Missing number of pieces.");
                        }
                        n_pieces = Integer.parseInt(args[i]);

                    } 
                    else if (args[i].equals("-s") || args[i].equals("--seed")) 
                    {
                        if (++i == args.length) 
                        {
                            throw new IllegalArgumentException("Missing seed number.");
                        }
                        seed = Integer.parseInt(args[i]);
                    }
                    else if (args[i].equals("-r") || args[i].equals("--runs")) 
                    {
                        if (++i == args.length) 
                        {
                            throw new IllegalArgumentException("Missing number of runs.");
                        }
                        runs = Integer.parseInt(args[i]);
                    }
                    else if (args[i].equals("-t") || args[i].equals("--turns")) 
                    {
                        if (++i == args.length) 
                        {
                            throw new IllegalArgumentException("Missing number of turns.");
                        }			
                        turns = Integer.parseInt(args[i]);
                    }
                    else 
                    {
                        throw new IllegalArgumentException("Unknown argument '" + args[i] + "'");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument '" + args[i] + "'");
            }
        }

        Log.log("Project: Flip. \n Simulator Version:" + version);
        Log.log("Players: " + playerNames.toString());
        Log.log("GUI " + (gui ? "enabled" : "disabled"));

        if (gui)
            Log.log("FPS: " + fps);
    }

    private static PlayerWrapper loadPlayerWrapper(String name, String mod_name) throws Exception {
        Log.log("Loading player " + name);
        Player p = loadPlayer(name);
        if (p == null) {
            Log.log("Cannot load player " + name);
            System.exit(1);
        }

        return new PlayerWrapper(p, mod_name, timeout);
    }

    public static Player loadPlayer(String name) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException 
    {
        String sep = File.separator;
        Set<File> player_files = directory(root + sep + name, ".java");
        File class_file = new File(root + sep + name + sep + "Player.class");
        long class_modified = class_file.exists() ? class_file.lastModified() : -1;
        if (class_modified < 0 || class_modified < last_modified(player_files) ||
                class_modified < last_modified(directory(root + sep + "sim", ".java"))) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null)
                throw new IOException("Cannot find Java compiler");
            StandardJavaFileManager manager = compiler.
                    getStandardFileManager(null, null, null);
//            long files = player_files.size();
            Log.log("Compiling for player " + name);
            if (!compiler.getTask(null, manager, null, null, null,
                    manager.getJavaFileObjectsFromFiles(player_files)).call())
                throw new IOException("Compilation failed");
            class_file = new File(root + sep + name + sep + "Player.class");
            if (!class_file.exists())
                throw new FileNotFoundException("Missing class file");
        }
        ClassLoader loader = Simulator.class.getClassLoader();
        if (loader == null)
            throw new IOException("Cannot find Java class loader");
        @SuppressWarnings("rawtypes")
        Class raw_class = loader.loadClass(root + "." + name + ".Player");
        return (Player)raw_class.newInstance();
    }

    private static long last_modified(Iterable<File> files) 
    {
        long last_date = 0;
        for (File file : files) 
        {
            long date = file.lastModified();
            if (last_date < date)
                last_date = date;
        }
        return last_date;
    }

    private static Set<File> directory(String path, String extension) {
        Set<File> files = new HashSet<File>();
        Set<File> prev_dirs = new HashSet<File>();
        prev_dirs.add(new File(path));
        do {
            Set<File> next_dirs = new HashSet<File>();
            for (File dir : prev_dirs)
                for (File file : dir.listFiles())
                    if (!file.canRead()) ;
                    else if (file.isDirectory())
                        next_dirs.add(file);
                    else if (file.getPath().endsWith(extension))
                        files.add(file);
            prev_dirs = next_dirs;
        } while (!prev_dirs.isEmpty());
        return files;
    }

    private static <T extends Object> T deepClone(T object) {
        if (object == null) {
            return null;
        }

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(object);
            ByteArrayInputStream bais = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            ObjectInputStream objectInputStream = new ObjectInputStream(bais);
            return (T) objectInputStream.readObject();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void gui(HTTPServer server, String content) {
        if (server == null) return;
        String path = null;
        for (;;) {
            for (;;) {
                try {
                    path = server.request();
                    break;
                } catch (IOException e) {
                    Log.record("HTTP request error " + e.getMessage());
                }
            }
            if (path.equals("data.txt")) {
                try {
                    server.reply(content);
                } catch (IOException e) {
                    Log.record("HTTP dynamic reply error " + e.getMessage());
                }
                return;
            }
            if (path.equals("")) path = "webpage.html";
            else if (!Character.isLetter(path.charAt(0))) {
                Log.record("Potentially malicious HTTP request \"" + path + "\"");
                break;
            }

            File file = new File(statics_root + File.separator + path);
            if (file == null) {
                Log.record("Unknown HTTP request \"" + path + "\"");
            } else {
                try {
                    server.reply(file);
                } catch (IOException e) {
                    Log.record("HTTP static reply error " + e.getMessage());
                }
            }
        }
    }


    // The state that is sent to the GUI. (JSON)
    private static String state(double fps) {
        String json = "{ \"refresh\":" + (1000.0/fps) + ",\"curr_round\":" + (int)round + ",\"remaining_turns\":" + (int)numTurns + ",";

        json+= "\"player1\":" + "\"" + player1.getName() + "\"" + ",\"player2\":" + "\"" + player2.getName() + "\"" + ",\"player1_score\":" + (int)player1_score + ",\"player2_score\":" + (int)player2_score + ",";  
        
        json += "\"player1_pieces\":[";
        for (int i = 0; i < player1_pieces_list.size(); i++)
        {
            Point p =  player1_pieces_list.get(i);
            json += "{\"x\" : " + p.x + ",\"y\" : " + p.y + "}";
            if (i !=  player1_pieces_list.size() - 1)
            {
                json += ",";
            }
        }
        json += "],";
        
         json += "\"player2_pieces\":[";
        for (int i = 0; i < player2_pieces_list.size(); i++)
        {
            Point p =  player2_pieces_list.get(i);
            json += "{\"x\" : " + p.x + ",\"y\" : " + p.y + "}";
            if (i !=  player2_pieces_list.size() - 1)
            {
                json += ",";
            }
        }
        // json += "],";
        
        json += "]}";
        return json;
    }


}
