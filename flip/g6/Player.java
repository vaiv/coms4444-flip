package flip.g6;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import javafx.util.Pair; 
import java.util.ArrayList;
import flip.sim.Point;

public class Player implements flip.sim.Player
{
	private static final double THRESHOLD = 0.7;
	private int seed = 42;
	private Random random;
	private boolean isPlayer1;
	private Integer n;
	private Double diameterPiece;
	private boolean strategiesDefined = false;
	private Aggressive aggressive;
	private ObstacleAvoidance obstacleAvoidance;
	private ObstacleCreation obstacleCreation;
	private HashMap<Integer, Point> playerPieces;
	private HashMap<Integer, Point> opponentPieces;

	/**
	 * Player constructor
	 */
	public Player()
	{
		random = new Random();
	}

	// Initialization function.
	// pieces: Location of the pieces for the player.
	// n: Number of pieces available.
	// t: Total turns available.
	/**
	 * 
	 */
	public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isPlayer1, double diameterPiece)
	{
		this.n = n;
		this.isPlayer1 = isPlayer1;
		this.diameterPiece = diameterPiece;
	}

	/**
	 * numMoves:
	 * playerPieces: 
	 * opponentPieces: 
	 * isPlayer1: 
	 */
	public List<Pair<Integer, Point>> getMoves(Integer numMoves, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, boolean isPlayer1) {

		this.playerPieces = playerPieces;
		this.opponentPieces = opponentPieces;	
		
		List<Pair<Integer, Point>> moves = new ArrayList<>();		
			
		if(!strategiesDefined) {
			aggressive = new Aggressive(playerPieces, opponentPieces, isPlayer1, n, diameterPiece);
			obstacleAvoidance = new ObstacleAvoidance(playerPieces, opponentPieces, isPlayer1, n, diameterPiece);
			obstacleCreation = new ObstacleCreation(playerPieces, opponentPieces, isPlayer1, n, diameterPiece);
			this.strategiesDefined = true;
		}
		else {
			aggressive.updatePieceInfo(playerPieces, opponentPieces);
			obstacleAvoidance.updatePieceInfo(playerPieces, opponentPieces);
			obstacleCreation.updatePieceInfo(playerPieces, opponentPieces);
		}

		for(int i = 0; i < numMoves; i++) {
			if(n <= 15) {
				moves.add(aggressive.getMove());
			}
			else if(n > 11) {
				if(aggressive.RUNNER_STRATEGY_SET) {
					Pair<Integer, Point> possMove = aggressive.run(numMoves, i % 2 == 0);
					if(possMove != null) {
						moves.add(possMove);
						continue;
					}
				}
				HashMap<Integer, Point> aggressivePlayerPieces = aggressive.getPlayerPieces();
				aggressivePlayerPieces.remove(aggressive.getHighPieceID());
				aggressivePlayerPieces.remove(aggressive.getLowPieceID());
				aggressive.setPlayerPieces(aggressivePlayerPieces);
				if(playerPieces.containsKey(aggressive.getHighPieceID()))
					playerPieces.remove(aggressive.getHighPieceID());
				if(playerPieces.containsKey(aggressive.getLowPieceID()))
					playerPieces.remove(aggressive.getLowPieceID());
				Pair<Integer, Point> possMove = aggressive.getMove();
				if(possMove != null) {
					moves.add(possMove);
					continue;
				}
				HashMap<Integer, Point> obstacleCreationPlayerPieces = obstacleCreation.getPlayerPieces();
				obstacleCreationPlayerPieces.remove(aggressive.getHighPieceID());
				obstacleCreationPlayerPieces.remove(aggressive.getLowPieceID());
				obstacleCreation.setPlayerPieces(obstacleCreationPlayerPieces);
				if(i == 0) {
					List<Pair<Integer, Point>> possMoves = obstacleCreation.doubleWallSwap();
					System.out.println(possMoves);
					if(possMoves != null) {
						moves.addAll(possMoves);
						return moves;
					}						
				}
				moves.add(obstacleCreation.getMove());
			}
		}
						
		return moves;
	}
	
	public int getSeed() {
		return seed;
	}

	public void setSeed(int seed) {
		this.seed = seed;
	}

	public Random getRandom() {
		return random;
	}

	public void setRandom(Random random) {
		this.random = random;
	}

	public boolean isPlayer1() {
		return isPlayer1;
	}

	public void setPlayer1(boolean isPlayer1) {
		this.isPlayer1 = isPlayer1;
	}

	public Integer getN() {
		return n;
	}

	public void setN(Integer n) {
		this.n = n;
	}

	public Double getDiameterPiece() {
		return diameterPiece;
	}

	public void setDiameterPiece(Double diameterPiece) {
		this.diameterPiece = diameterPiece;
	}

	public boolean isStrategiesDefined() {
		return strategiesDefined;
	}

	public void setStrategiesDefined(boolean strategiesDefined) {
		this.strategiesDefined = strategiesDefined;
	}

	public Aggressive getAggressive() {
		return aggressive;
	}

	public void setAggressive(Aggressive aggressive) {
		this.aggressive = aggressive;
	}

	public ObstacleAvoidance getObstacleAvoidance() {
		return obstacleAvoidance;
	}

	public void setObstacleAvoidance(ObstacleAvoidance obstacleAvoidance) {
		this.obstacleAvoidance = obstacleAvoidance;
	}

	public ObstacleCreation getObstacleCreation() {
		return obstacleCreation;
	}

	public void setObstacleCreation(ObstacleCreation obstacleCreation) {
		this.obstacleCreation = obstacleCreation;
	}

	public static double getThreshold() {
		return THRESHOLD;
	}
		
	public HashMap<Integer, Point> getPlayerPieces() {
		return playerPieces;
	}

	public HashMap<Integer, Point> getOpponentPieces() {
		return opponentPieces;
	}
}
