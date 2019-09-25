package flip.g6;

import java.util.HashMap;
import java.util.Random;
import flip.sim.Point;
import javafx.util.Pair;

public class ObstacleAvoidance extends Move {

	private HashMap<Integer, Point> player_pieces;
	private HashMap<Integer, Point> opponent_pieces;
	private boolean isplayer1;
	private Integer n;
	private Double diameter_piece;

	public ObstacleAvoidance(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, Integer n, Double diameter_piece) {
		this.player_pieces = player_pieces;
		this.opponent_pieces = opponent_pieces;
		this.isplayer1 = isplayer1;
		this.n = n;
		this.diameter_piece = diameter_piece;
	}

	@Override
	public void updatePieceInfo(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
		this.player_pieces = player_pieces;
		this.opponent_pieces = opponent_pieces;
	}

	@Override
	public Pair<Integer, Point> getMove() {

		Pair<Integer, Point> move = null;
		HashMap<Integer, Point> unfinished_pieces = getUnfinishedPlayerPieces(player_pieces, isplayer1, Approach.AVOIDANCE);
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
		double maxInteriorDistance = player_pieces.size()/10 + 4;
		double maxCentroidDistance = player_pieces.size()/10;

		if((isplayer1 && curr_position.x < -(20 + maxInteriorDistance)) || (!isplayer1 && curr_position.x > (20 + maxInteriorDistance))) {
			try {
				return getMove();
			} catch (Exception e) {
				return null;
			}
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

		new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
		new_position.y += delta_y;
		move = new Pair<Integer, Point>(piece_id, new_position);

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
}