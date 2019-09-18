package flip.goalone;
import java.util.List;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.lang.Math;
import java.util.HashMap;
import javafx.util.Pair; 
import java.util.ArrayList;

import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;

public class Player implements flip.sim.Player
{
	private int seed = 42;
	private Random random;
	private boolean isplayer1;
	private Integer n;
	private Double diameter_piece;
	private boolean goalone;
	private ArrayList<Integer> runner;

	private HashMap<Integer, Point> myPieces;
	private HashMap<Integer, Point> oPieces;

	public Player()
	{
		random = new Random(seed);
		goalone = false;
	}

	// Initialization function.
    // pieces: Location of the pieces for the player.
    // n: Number of pieces available.
    // t: Total turns available.
	public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isplayer1, double diameter_piece)
	{
		System.out.println("N = " + n);
		this.n = n;
		this.isplayer1 = isplayer1;
		this.diameter_piece = diameter_piece;
		this.runner = new ArrayList<Integer>();
	}

	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		List<Pair<Integer, Point>> output = new ArrayList<Pair<Integer, Point>> ();

		int[][] density;
		if(!goalone) {
			density = getDensity(player_pieces, opponent_pieces, 42, 14);
			//printArray(density);
			//System.out.println();
			checkGoalOne(density, player_pieces);
		}
		
		

		Pair<Integer, Point> temp;
		if(!goalone) {
			temp = getBestMove(num_moves, player_pieces, opponent_pieces, isplayer1);
			if(temp != null) output.add(temp);
			else {
				temp = getNextMove(num_moves, player_pieces, opponent_pieces, isplayer1);
				output.add(temp);
			}

			//System.out.println(temp);
			if(num_moves == 1) {
				return output;
			}

			player_pieces.put(player_pieces.size(), output.get(0).getValue());
			temp = getBestMove(num_moves, player_pieces, opponent_pieces, isplayer1);
			if(temp != null) output.add(temp);
			else {
				temp = getNextMove(num_moves, player_pieces, opponent_pieces, isplayer1);
				output.add(temp);
			}
		}
		else {
			temp = getRunnerBestMove(num_moves, player_pieces, opponent_pieces, isplayer1);
			if(temp != null) output.add(temp);
			else {
				temp = getRunnerNextMove(num_moves, player_pieces, opponent_pieces, isplayer1);
				output.add(temp);
			}

			//System.out.println(temp);
			if(num_moves == 1) {
				return output;
			}

			player_pieces.put(player_pieces.size(), output.get(0).getValue());
			temp = getRunnerBestMove(num_moves, player_pieces, opponent_pieces, isplayer1);
			if(temp != null) output.add(temp);
			else {
				temp = getRunnerNextMove(num_moves, player_pieces, opponent_pieces, isplayer1);
				output.add(temp);
			}     
		}
		return output;
	}

	private void checkGoalOne(int[][] matrix, HashMap<Integer, Point> pieces) {
		if(goalone) return;
		int index = 15;
		if(isplayer1) index = 41-15;
		int sum;
		for(int i = 0; i < matrix.length; i++) {
			if(addRow(matrix, i) == 0 ) return;
		}
		goalone = true;
		findRunner(matrix, pieces);
	}

	private int addRow(int[][] matrix, int y) {
		int sum = 0;
		if(isplayer1) {
			for(int i = 26; i >= 0; i--) {
				sum += matrix[y][i];
			}
		}
		else {
			for(int i = 15; i < matrix[0].length; i++) {
				sum += matrix[y][i];
			}
		}
		//System.out.println(sum);
		return sum;
	}

	private void findRunner(int[][] matrix, HashMap<Integer, Point> pieces) {
		List<Integer> list = new ArrayList<Integer>();
		int index = 15;
		if(isplayer1) index = 41-15;
		//get a list of row index that has more than 1 in its block.
		for(int i = 0; i < matrix.length; i++) {
			if(addRow(matrix, i) > 1) {
				System.out.println("!! " + i);
				list.add(i);
			}
		}

		double xMin = 15.0 * (120.0 / 42.0) - 60.0;
		double xMax = 16.0 * (120.0 / 42.0) - 60.0;

		if(isplayer1) {
			xMin = 25.0 * (120.0 / 42.0) - 60.0;
			xMax = 26.0 * (120.0 / 42.0) - 60.0;
		}

		//System.out.println("xMin: " + xMin + " xMax: " + xMax);

		for(int y : list) {
			List<Integer> points = new ArrayList<Integer>();
			double yMin = (double) (y + 0) * (40.0 / 14.0) - 20.0;
			double yMax = (double) (y + 1) * (40.0 / 14.0) - 20.0;
			System.out.println("yMin: " + yMin + " yMax: " + yMax);
			double min = xMax;

			for(int i = 0; i < pieces.size(); i++) {
				Point temp = pieces.get(i);
				if(temp.x >= xMin && temp.y >= yMin && temp.y <= yMax) {
					System.out.println("it happens");
					points.add(i);
					if(temp.x < min) { 
						min = temp.x;
					}
				}
			}

			for(int i : points) {
				Point temp = pieces.get(i);
				if(temp.x != min) runner.add(i);
			}
		}

		for(int i : runner) {
			Point temp = pieces.get(i);
			System.out.println(i + ": " + "x: " + temp.x + " y: " + temp.y);
		}
	}

	private void printArray(int[][] temp) {
		for(int i = 0; i < temp.length; i++) {
			System.out.print("[");
			for(int j = 0; j < temp[0].length; j++) {
				System.out.print(" " + temp[i][j] + " ");
			}
			System.out.println("]");
		}
	}

	private int[] getYDensity(HashMap<Integer, Point> player_pieces, int size) {
		int[] output = new int[size];
		Point temp;
		int index;

		for(int i = 0; i < player_pieces.size(); i++) {
			temp = player_pieces.get(i);
			index = ((int) Math.round(temp.y) + 20) / (40/size);
			System.out.println(temp.y + " : " + index);
			output[index]++;
		}

		return output;
	}

	private int[] getXDensity(HashMap<Integer, Point> player_pieces, int size) {
		int[] output = new int[size];
		Point temp;
		int index;

		for(int i = 0; i < player_pieces.size(); i++) {
			temp = player_pieces.get(i);
			index = ((int) Math.round(temp.x) + 60) / (120 / size);
			//System.out.println(temp.x + " : " + index);
			output[index]++;
		}

		return output;
	}

	private int[][] getDensity(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, int x, int y) {
		int[][] output = new int[y][x];
		Point pTemp;
		Point oTemp;
		int px;
		int py;
		int ox;
		int oy;

		for(int i = 0; i < player_pieces.size(); i++) {
			pTemp = player_pieces.get(i);
			//oTemp = opponent_pieces.get(i);
			//px = ((int) Math.round(pTemp.x) + 60) / (120 / x); 
			//py = ((int) Math.round(pTemp.y) + 20) / (40 / y);
			px = (int)((pTemp.x + 60.0) / (120.0 / (double) x));
			py = (int)((pTemp.y + 20.0) / (40.0 / (double) y));
			//System.out.println(px + " " + py);

			//ox = ((int) Math.round(oTemp.x) + 60) / (120 / x);
			//oy = ((int) Math.round(oTemp.y) + 20) / (40.0 / y);
			//ox = (int)((oTemp.x + 60.0) / (120.0 / (double) x));
			//oy = (int)((oTemp.y + 20.0) / (40.0 / (double) y));
			output[py][px]++;
			//output[oy][ox]++;
		}

		return output;
	}

	private Pair<Integer, Point> getBestMove(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

		//my code here
		for (int i = 0; i < n; i++) {
			Point curr_position = player_pieces.get(i);

			Point new_position = new Point(curr_position);
			new_position.x = isplayer1 ? new_position.x - this.diameter_piece : new_position.x + this.diameter_piece;
			// Want delta x > 0
			Pair<Integer, Point> move = new Pair<Integer, Point>(i, new_position);
			if(check_validity(move, player_pieces, opponent_pieces)) {
				//System.out.println("IDEAL: " + move.getValue().x);
				if(isplayer1) {
					if(!goalone){
						if(move.getValue().x < 14) continue;
					}
					else {
						if(move.getValue().x < -30) continue;
					}
				} else {
					if(!goalone) {
						if(move.getValue().x > -14) continue;
					} else {
						if(move.getValue().x > 30) continue;
					}
				}
				moves.add(move);
				continue;
			}
		 }
		 if(moves.size() > 0)
		 	return pick_move(num_moves, moves, isplayer1, player_pieces); 
		 return null;
	}

	private Pair<Integer, Point> getRunnerBestMove(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

		//my code here
		for (int i : runner) {
			Point curr_position = player_pieces.get(i);

			Point new_position = new Point(curr_position);
			new_position.x = isplayer1 ? new_position.x - this.diameter_piece : new_position.x + this.diameter_piece;
			// Want delta x > 0
			Pair<Integer, Point> move = new Pair<Integer, Point>(i, new_position);
			if(check_validity(move, player_pieces, opponent_pieces)) {
				//System.out.println("IDEAL: " + move.getValue().x);
				if(isplayer1) {
					if(!goalone){
						if(move.getValue().x < 14) continue;
					}
					else {
						if(move.getValue().x < -30) continue;
					}
				} else {
					if(!goalone) {
						if(move.getValue().x > -14) continue;
					} else {
						if(move.getValue().x > 30) continue;
					}
				}
				moves.add(move);
				continue;
			}
		 }
		 if(moves.size() > 0)
		 	return pick_move(num_moves, moves, isplayer1, player_pieces); 
		 return null;
	}

	private Pair<Integer, Point> getNextMove(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();

		//my code here
		for (int i = 0; i < n; i++) {
			Point curr_position = player_pieces.get(i);
			Point new_position = new Point(curr_position);
			Pair<Integer, Point> move = new Pair<Integer, Point>(i, new_position);
			double theta = 0.1;
			do {
				new_position.x = curr_position.x;
				new_position.y = curr_position.y;

				//TODO: could create a table w/ these values to make runtime faster
				double delta_x1 = diameter_piece * Math.cos(theta);
				double delta_y1 = diameter_piece * Math.sin(theta);

				new_position.x = isplayer1 ? new_position.x - delta_x1 : new_position.x + delta_x1;
				new_position.y += delta_y1;
				move = new Pair<Integer, Point>(i, new_position);
				if(check_validity(move,	player_pieces, opponent_pieces)) {
					//System.out.println("VALID");
					break;
				}
				//check same move reflected over x axis
				new_position.x = curr_position.x;
				new_position.y = curr_position.y;
				double delta_x2 = diameter_piece * Math.cos(-1 * theta);
				double delta_y2 = diameter_piece * Math.sin(-1 * theta);
				new_position.x = isplayer1 ? new_position.x - delta_x2 : new_position.x + delta_x2;
				new_position.y += delta_y2;
				move = new Pair<Integer, Point>(i, new_position);
				if(check_validity(move,	player_pieces, opponent_pieces)) {
					//System.out.println("VALID");
					break;
				}
				theta += .1;
			} while (theta < Math.PI / 2 - .05); //slightly under 90 degree angle

		 	if(check_validity(move, player_pieces, opponent_pieces)) {
		 		if(isplayer1) {
					if(!goalone){
						if(move.getValue().x < 14) continue;
					}
					else {
						if(move.getValue().x < -30) continue;
					}
				} else {
					if(!goalone) {
						if(move.getValue().x > -14) continue;
					} else {
						if(move.getValue().x > 30) continue;
					}
				}
				possible_moves.add(move);
			}
		 }

		 return pick_move(num_moves, possible_moves, isplayer1, player_pieces);
	}

	private Pair<Integer, Point> getRunnerNextMove(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		List<Pair<Integer, Point>> possible_moves = new ArrayList<Pair<Integer, Point>>();

		//my code here
		for (int i : runner) {
			Point curr_position = player_pieces.get(i);
			Point new_position = new Point(curr_position);
			Pair<Integer, Point> move = new Pair<Integer, Point>(i, new_position);
			double theta = 0.1;
			do {
				new_position.x = curr_position.x;
				new_position.y = curr_position.y;

				//TODO: could create a table w/ these values to make runtime faster
				double delta_x1 = diameter_piece * Math.cos(theta);
				double delta_y1 = diameter_piece * Math.sin(theta);

				new_position.x = isplayer1 ? new_position.x - delta_x1 : new_position.x + delta_x1;
				new_position.y += delta_y1;
				move = new Pair<Integer, Point>(i, new_position);
				if(check_validity(move,	player_pieces, opponent_pieces)) {
					//System.out.println("VALID");
					break;
				}
				//check same move reflected over x axis
				new_position.x = curr_position.x;
				new_position.y = curr_position.y;
				double delta_x2 = diameter_piece * Math.cos(-1 * theta);
				double delta_y2 = diameter_piece * Math.sin(-1 * theta);
				new_position.x = isplayer1 ? new_position.x - delta_x2 : new_position.x + delta_x2;
				new_position.y += delta_y2;
				move = new Pair<Integer, Point>(i, new_position);
				if(check_validity(move,	player_pieces, opponent_pieces)) {
					//System.out.println("VALID");
					break;
				}
				theta += .1;
			} while (theta < Math.PI / 2 - .05); //slightly under 90 degree angle

		 	if(check_validity(move, player_pieces, opponent_pieces)) {
		 		if(isplayer1) {
					if(!goalone){
						if(move.getValue().x < 14) continue;
					}
					else {
						if(move.getValue().x < -30) continue;
					}
				} else {
					if(!goalone) {
						if(move.getValue().x > -14) continue;
					} else {
						if(move.getValue().x > 30) continue;
					}
				}
				possible_moves.add(move);
			}
		 }

		 return pick_move(num_moves, possible_moves, isplayer1, player_pieces); 
	}

	public Pair<Integer, Point> pick_move(Integer num_moves, List<Pair<Integer, Point>> possible_moves, boolean isplayer1, HashMap<Integer, Point> player_pieces) {
		Pair<Integer, Point> move = possible_moves.get(0);

		//int index = 0;

		for(int i = 1; i<possible_moves.size(); i++) {
			if(isBetterThan(move, possible_moves.get(i), isplayer1)) {
				move = possible_moves.get(i);
				//index = i;
			}
		}

		//player_pieces.put(index, move.getValue());
		return move;
	}

	private boolean isBetterThan(Pair<Integer, Point> current_move, Pair<Integer, Point> new_move, boolean isplayer1) {
		if(isplayer1) {
			return current_move.getValue().x > new_move.getValue().x;
		} else {
			return current_move.getValue().x < new_move.getValue().x;
		}
	}

	private int getScore(HashMap<Integer, Point> pieces) {
		int score = 0;

		for(int i = 0; i < pieces.size(); i++) {
			Point temp = pieces.get(i);
			
			if(isplayer1)
				if(temp.x >= -60.0 - diameter_piece/2 && temp.x <= -20.0 - diameter_piece/2) score++;
			else
				if(temp.x >= 20.0 + diameter_piece/2 && temp.x <= 60.0 - diameter_piece/2) score++;
		}
		return score;
	}

	public boolean check_validity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces)
    {
        boolean valid = true;
       
        // check if move is adjacent to previous position.
        if(!Board.almostEqual(Board.getdist(player_pieces.get(move.getKey()), move.getValue()), diameter_piece))
            {
                return false;
            }
        // check for collisions
        valid = valid && !Board.check_collision(player_pieces, move);
        valid = valid && !Board.check_collision(opponent_pieces, move);

        // check within bounds
        valid = valid && Board.check_within_bounds(move);
        return valid;

    }
}

