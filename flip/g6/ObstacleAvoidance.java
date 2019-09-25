package flip.g6;

import java.util.HashMap;
import java.util.Random;
import flip.sim.Point;
import javafx.util.Pair;

public class ObstacleAvoidance extends Move {

	private HashMap<Integer, Point> playerPieces;
	private HashMap<Integer, Point> opponentPieces;
	private boolean isPlayer1;
	private Integer n;
	private Double diameterPiece;

	/**
	 * Constructor for obstacle avoidance strategy
	 * @param playerPieces
	 * @param opponentPieces
	 * @param isPlayer1
	 * @param n: Total coins
	 * @param diameterPiece
	 */
	public ObstacleAvoidance(HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayer1, Integer n, Double diameterPiece) {
		this.playerPieces = playerPieces;
		this.opponentPieces = opponentPieces;
		this.isPlayer1 = isPlayer1;
		this.n = n;
		this.diameterPiece = diameterPiece;
	}

	/**
	 * Update piece info of pieces
	 */
	@Override
	public void updatePieceInfo(HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces) {
		this.playerPieces = playerPieces;
		this.opponentPieces = opponentPieces;
	}

	/**
	 * Get moves for avoidance strategy
	 */
	@Override
	public Pair<Integer, Point> getMove() {

		Pair<Integer, Point> move = null;
		HashMap<Integer, Point> unfinishedPieces = getUnfinishedPlayerPieces(playerPieces, isPlayer1, Approach.AVOIDANCE);
		HashMap<Integer, Point> closestPieces = getClosestPointsToOpponentBoundary(n / 2, unfinishedPieces, isPlayer1);
		HashMap<Integer, Point> relevantPieces = (HashMap<Integer, Point>) playerPieces.clone();
		
		for(Integer id : closestPieces.keySet()) {
			relevantPieces.remove(id);
		}

		Random random = new Random();
		Integer piece_id = random.nextInt(n);
		while(!relevantPieces.containsKey(piece_id))
			piece_id = random.nextInt(n);
		Point currPosition = playerPieces.get(piece_id);
		Point newPosition = new Point(currPosition);

		Point opponentCentroid = getPlayerCentroid(opponentPieces);
		Point teamCentroid = getPlayerCentroid(playerPieces);
		double angleBetweenPalyerCenteroids = 0;
		if(playerPieces.size() > 5) {
			angleBetweenPalyerCenteroids = getAngle(teamCentroid, opponentCentroid);
		}
		double maxInteriorDistance = playerPieces.size()/10 + 4;
		double maxCentroidDistance = playerPieces.size()/10;

		if((isPlayer1 && currPosition.x < -(20 + maxInteriorDistance)) || (!isPlayer1 && currPosition.x > (20 + maxInteriorDistance))) {
			try {
				return getMove();
			} catch (Exception e) {
				return null;
			}
		}

		double theta = -Math.PI/2 + Math.PI * random.nextDouble();
		double delta_x = 0;
		double delta_y = 0;
		if(Math.abs(opponentCentroid.x - teamCentroid.x) > maxCentroidDistance && (opponentCentroid.x > teamCentroid.x) && !isPlayer1) {
			delta_x = diameterPiece * Math.cos(angleBetweenPalyerCenteroids);
			delta_y = diameterPiece * Math.sin(angleBetweenPalyerCenteroids);
		}else if(Math.abs(opponentCentroid.x - teamCentroid.x) > maxCentroidDistance && opponentCentroid.x < teamCentroid.x && isPlayer1) {
			delta_x = diameterPiece * Math.cos(-angleBetweenPalyerCenteroids);
			delta_y = diameterPiece * Math.sin(-angleBetweenPalyerCenteroids);
		}else {
			delta_x = diameterPiece * Math.cos(theta);
			delta_y = diameterPiece * Math.sin(theta);
		}

		newPosition.x = isPlayer1 ? newPosition.x - delta_x : newPosition.x + delta_x;
		newPosition.y += delta_y;
		move = new Pair<Integer, Point>(piece_id, newPosition);

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