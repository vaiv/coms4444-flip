package flip.g6;

import java.util.HashMap;
import java.util.Random;

import flip.sim.Board;
import flip.sim.Point;
import javafx.util.Pair;

public class ObstacleCreation extends Move {

	private int seed = 42;
	private HashMap<Integer, Point> player_pieces;
	private HashMap<Integer, Point> opponent_pieces;
	private boolean isplayer1;
	private Integer n;
	private Double diameter_piece;

	public ObstacleCreation(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, Integer n, Double diameter_piece) {
		this.player_pieces = player_pieces;
		this.opponent_pieces = opponent_pieces;
		this.isplayer1 = isplayer1;
		this.n = n;
		this.diameter_piece = diameter_piece;
	}


	@Override
	public boolean isPossible() {
		return false; // TODO: Change this implementation
	}

	@Override
	public Pair<Integer, Point> getMove() {

		//		System.out.println("In getMove of obstacle creation");

		Pair<Integer, Point> move = null; // TODO: Change this implementation

		HashMap<Integer, Point> unfinished_pieces = getUnfinishedPlayerPieces(player_pieces, isplayer1);
		HashMap<Integer, Point> closest_pieces = getClosestPointsToOpponentBoundary(n / 2, unfinished_pieces, isplayer1);
		HashMap<Integer, Point> relevant_pieces = (HashMap<Integer, Point>) player_pieces.clone();

		for(Integer id : closest_pieces.keySet()) {
			relevant_pieces.remove(id);
		}

		Random random = new Random();
		Integer piece_id = random.nextInt(n);
		while(!relevant_pieces.containsKey(piece_id))
			piece_id = random.nextInt(n);
		Point curr_position = player_pieces.get(piece_id);
		Point new_position = new Point(curr_position);

		Point opponentCentroid = getPlayerCentroid(opponent_pieces);
		Point teamCentroid = getPlayerCentroid(player_pieces);
		double angleBetweenPalyerCenteroids = 0;
		if(player_pieces.size() > 5) {
			angleBetweenPalyerCenteroids = getAngle(teamCentroid, opponentCentroid);
		}
		double maxInteriorDistance = player_pieces.size()/10 + 3;
		double maxCentroidDistance = player_pieces.size()/10;

		if((isplayer1 && curr_position.x < -(20 + maxInteriorDistance)) || (!isplayer1 && curr_position.x > (20 + maxInteriorDistance))) {
			// Player 1
			return getMove();
		}

		double theta = -Math.PI/2 + Math.PI * random.nextDouble();
		double delta_x = 0;
		double delta_y = 0;
		if(Math.abs(opponentCentroid.x - teamCentroid.x) > maxCentroidDistance && (opponentCentroid.x > teamCentroid.x) && !isplayer1) {
			delta_x = diameter_piece * Math.cos(angleBetweenPalyerCenteroids);
			delta_y = diameter_piece * Math.sin(angleBetweenPalyerCenteroids);
		}else if(Math.abs(opponentCentroid.x - teamCentroid.x) > maxCentroidDistance && opponentCentroid.x < teamCentroid.x && isplayer1) {
			delta_x = diameter_piece * Math.cos(-angleBetweenPalyerCenteroids);
			delta_y = diameter_piece * Math.sin(-angleBetweenPalyerCenteroids);
		}else {
			delta_x = diameter_piece * Math.cos(theta);
			delta_y = diameter_piece * Math.sin(theta);
		}

		Double val = (Math.pow(delta_x,2) + Math.pow(delta_y, 2));
		// System.out.println("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " + Math.cos(theta) + " " + Math.sin(theta) + " diameter is " + diameter_piece);
		// Log.record("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " + Math.cos(theta) + " " + Math.sin(theta) + " diameter is " + diameter_piece);

		new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
		new_position.y += delta_y;
		move = new Pair<Integer, Point>(piece_id, new_position);

		Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());
		// System.out.println("distance from previous position is " + dist.toString());
		// Log.record("distance from previous position is " + dist.toString());

		// System.out.println(move.getKey() + " " + move.getValue());
		
		System.out.println("USED THETA...Obstacle creation, theta: " + theta);
		return move;
	}

	/**
	 * Get centroid of opponent points
	 * @param opponent_pieces
	 * @return
	 */
	public Point getPlayerCentroid(HashMap<Integer, Point> pieces) {
		double centroidX = 0, centroidY = 0;

		for(Integer i : pieces.keySet()) {
			Point point = pieces.get(i);
			centroidX += point.x;
			centroidY += point.y;
		}
		return new Point(centroidX / pieces.size(), centroidY / pieces.size());
	}

	/**
	 * Get angle between two points
	 * @param target
	 * @return
	 */
	public float getAngle(Point origin, Point target) {
		float angle = (float) Math.toDegrees(Math.atan2(target.y - origin.y, target.x - origin.x));

		if(angle < 0){
			angle += 360;
		}

		return angle;
	}

	@Override
	public Pair<Integer, Point> getHybridMove() {
		Pair<Integer, Point> move = null; // TODO: Change this implementation
		return move;
	}
}
