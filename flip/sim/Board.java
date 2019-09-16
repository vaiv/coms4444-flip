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

import java.lang.*; 
import java.util.Random;
import java.util.ArrayList;

import flip.sim.Point;
import javafx.util.Pair; 

import java.util.HashMap; 
import java.util.Map; 
import java.util.*;
import java.util.HashSet;

public class Board 
{

    private Integer num_pieces;
    private Random r;
    private static double diameter_piece;
    private static double eps;
    private static double x_min, x_max, y_min,y_max;
    private double init_offset_x;
    public HashMap<Integer, Point> player1;
    public HashMap<Integer, Point> player2;
    private Set<String> pieces_offsets;

    public Board(Integer n, Integer seed)
    {
        num_pieces = n; 
        r = new Random(seed);
        x_max = 60.0;
        x_min = -60.0;
        y_max = 20.0;
        y_min = -20.0;
        init_offset_x = 20.0; 
        diameter_piece = 2.0;
        eps = 1E-7;
        player1 = new HashMap<Integer, Point>();
        player2 = new HashMap<Integer, Point>();
        pieces_offsets = new HashSet<String>();
        generate_random_pieces();
    }

    public void generate_random_pieces()
    {
        
        while(player1.size()!=num_pieces)
        {
            Double x = init_offset_x + (x_max - init_offset_x) * r.nextDouble();
            Double y = y_min + (y_max - y_min) * r.nextDouble();

            String s = x.toString() + ":" + y.toString();
            if(pieces_offsets.contains(s) || check_collision(player1, new Pair<Integer, Point>(player1.size(),new Point(x, y))))
                continue;

            if(x - diameter_piece/2  + eps < init_offset_x || x + diameter_piece/2 - eps > x_max  || y - diameter_piece/2  + eps < y_min || y + diameter_piece/2 - eps > y_max)
                continue;

            pieces_offsets.add(s);
            player1.put( (Integer) player1.size(), new Point(x, y));
            player2.put( (Integer) player2.size(), new Point(-x, y));

        }

    }

    public static boolean almostEqual(double a, double b)
    {
        return Math.abs(a-b) < eps;
    }

    public static double getdist(Point a, Point b)
    {
        return Math.sqrt(Math.pow(Math.abs(a.x - b.x), 2.0) + Math.pow(Math.abs(a.y - b.y), 2.0));
    }
        
    public static double get_diameter_piece()
    {
        return diameter_piece;
    }
    public static boolean check_collision(HashMap<Integer, Point> m, Pair<Integer, Point> move)
    {
        for (HashMap.Entry<Integer, Point> entry : m.entrySet()) 
        {
            if ( getdist(move.getValue(), entry.getValue()) + eps < diameter_piece)
            {
                // Double dist = getdist(move.getValue(), entry.getValue()) + eps;
                // Log.record("collision detected between pieces " + move.getKey().toString() + " and "+ entry.getKey().toString()+ "distance was "+ dist.toString());
                return true;
            }
                
        }
        return false;
    }

    public static boolean check_within_bounds(Pair<Integer, Point> move)
    {
        return !(move.getValue().x - diameter_piece/2  + eps < x_min || move.getValue().x + diameter_piece/2 - eps > x_max  || 
                move.getValue().y - diameter_piece/2  + eps < y_min || move.getValue().y + diameter_piece/2 - eps > y_max);
    }

    public boolean check_valid_move(Pair<Integer, Point> move, boolean isplayer1)
    {
        boolean valid = true;
        try
        {
            String curr_player = isplayer1 ? "1" : "2";
            // check if null move
            if(move == null)
               {
                    Log.record("Player " + curr_player + " chose not to move.");
                    return false;
               } 
            //check if valid key
            if(move.getKey()<0 || move.getKey()>=num_pieces)
                {
                    Log.record("Ivalid key in move from player " + curr_player);
                    return false;
                }
            // check if move is adjacent to previous position.
            if(isplayer1 && !almostEqual(getdist(player1.get(move.getKey()), move.getValue()), diameter_piece))
                {
                    Double dist = getdist(player1.get(move.getKey()), move.getValue());
                    Log.record("new move not adjacent to previous position for player 1." + " dist was " + dist.toString());
                    return false;
                }
            else if (!isplayer1 && !almostEqual(getdist(player2.get(move.getKey()), move.getValue()), diameter_piece))
                {
                     Double dist = getdist(player2.get(move.getKey()), move.getValue());
                    Log.record("new move not adjacent to previous position for player 2." + " dist was " + dist.toString());
                    return false;
                }

            // check for collisions
            valid = valid && !check_collision(player1, move);
            valid = valid && !check_collision(player2, move);
            if(!valid)
                Log.record(" Collision detected in new move for player "+ curr_player);

            // check within bounds
            if(move.getValue().x - diameter_piece/2  + eps < x_min || move.getValue().x + diameter_piece/2 - eps > x_max  || 
                move.getValue().y - diameter_piece/2  + eps < y_min || move.getValue().y + diameter_piece/2 - eps > y_max)
                {
                    valid = false;
                    Log.record(" piece is being placed out of bounds by player  "+ curr_player);
                }

        }
        catch (Exception ex)
        {
            Log.record("unanticipated error occurred while evaluating move. "+ ex.getMessage());
            return false;
        }

        return valid;

    }

    public void move_piece(Pair<Integer, Point> move, boolean isplayer1)
    {
        if(isplayer1)
            player1.put(move.getKey(), move.getValue());
        else
            player2.put(move.getKey(), move.getValue());
    }

    public Integer get_score(boolean isplayer1)
    {
        double lim_min = 0.0, lim_max= 0.0;
        HashMap<Integer, Point> m;
        Integer score = 0;
        if(isplayer1)
        {
            
            lim_min = -60.0;
            lim_max = -20.0;
            m = player1;
        }
        else
        {
            lim_min = 20.0;
            lim_max = 60.0;
            m = player2;
        }

        for (HashMap.Entry<Integer, Point> entry : m.entrySet()) 
        {
            if(entry.getValue().x - diameter_piece/2 + eps >= lim_min  && entry.getValue().x + diameter_piece/2 - eps <= lim_max)
                score++;
        }

        return score;

    }

    public HashMap<Integer, Point> get_pieces(boolean isplayer1)
    {
        if(isplayer1) return player1;
        else return player2;
    }
    
    
}