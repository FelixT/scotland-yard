package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.util.*;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;


// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private List<ScotlandYardPlayer> players = new ArrayList<>();
	private List<Spectator> spectators = new ArrayList<>();
	private int round = 0;
	private Colour currentPlayer = BLACK;
	private Move lastmove;

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {
		this.rounds = Objects.requireNonNull(rounds);
		this.graph = Objects.requireNonNull(graph);
		if (rounds.isEmpty())
			throw new IllegalArgumentException("Empty rounds");
		if (graph.isEmpty())
			throw new IllegalArgumentException("Empty map");
		if (mrX.colour != BLACK)
			throw new IllegalArgumentException("MrX should be black");

		ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
		for (PlayerConfiguration configuration : restOfTheDetectives)
			configurations.add(Objects.requireNonNull(configuration));
		configurations.add(0, firstDetective);
		configurations.add(0, mrX);

		Set<Ticket> allticketset = new HashSet<>();
		allticketset.add(Ticket.TAXI);
		allticketset.add(Ticket.BUS);
		allticketset.add(Ticket.UNDERGROUND);
		allticketset.add(Ticket.DOUBLE);
		allticketset.add(Ticket.SECRET);
		Set<Integer> locationset = new HashSet<>();
		Set<Colour> colourset = new HashSet<>();
		for (PlayerConfiguration configuration : configurations) {
			if (locationset.contains(configuration.location))
				throw new IllegalArgumentException("Duplicate location");
			if (colourset.contains(configuration.colour))
				throw new IllegalArgumentException("Duplicate colour");
			locationset.add(configuration.location);
			colourset.add(configuration.colour);

			if (!configuration.tickets.keySet().containsAll(allticketset))
				throw new IllegalArgumentException("Each player must have each ticket type even if their values are zero");

			if (configuration.colour != BLACK) {
				// check detectives have the right tickets
				if (configuration.tickets.get(DOUBLE) != 0)
					throw new IllegalArgumentException("Detectives should not have double tickets");
				if (configuration.tickets.get(SECRET) != 0)
					throw new IllegalArgumentException("Detectives should not have secret tickets");
			}

			ScotlandYardPlayer player = new ScotlandYardPlayer(configuration.player, configuration.colour, configuration.location, configuration.tickets);
			players.add(Objects.requireNonNull(player));

		}

	}

	@Override
	public void accept(Move move) {
		if (move == null)
			throw new NullPointerException("Move can't be null");
		Set<Move> validmoves = validMove(currentPlayer);
		if (!validmoves.contains(move))
			throw new IllegalArgumentException("Invalid move");
		lastmove = move;
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		spectators.add(Objects.requireNonNull(spectator));
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		spectators.remove(spectator);
	}

	private Set<Move> validMove(Colour player) {
		return emptySet();
	}

	private void nextPlayer() {
		// get current player index
		int playerIndex = 0;
		for (ScotlandYardPlayer player: players)
			if (player.colour() != currentPlayer)
				playerIndex++;

		int nextPlayer = (playerIndex + 1) % players.size();
		// get colour of next player
		currentPlayer = players.get(nextPlayer).colour();
	}

	@Override
	public void startRotate() {
		int turn = 0;
		while (turn < players.size()) {
			Set<Move> moves = validMove(currentPlayer);

			for (ScotlandYardPlayer player : players) {
				if (player.colour() == currentPlayer) {
					player.player().makeMove(this, player.location(), moves, this);
				}
			}
			nextPlayer();
			if (currentPlayer == BLACK) {
				round++;
				for (Spectator spectator: spectators)
					spectator.onRoundStarted(this, round);
			}
			turn++;
			for (Spectator spectator: spectators)
				spectator.onMoveMade(this, lastmove);
		}
		for (Spectator spectator: spectators)
			spectator.onRotationComplete(this);

	}

	@Override
	public Collection<Spectator> getSpectators() {
		return spectators;
	}

	@Override
	public List<Colour> getPlayers() {
		List<Colour> playerColours = new ArrayList<>();
		for (ScotlandYardPlayer player: players)
			playerColours.add(Objects.requireNonNull(player.colour()));
		return Collections.unmodifiableList(playerColours);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		return unmodifiableSet(emptySet());
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		for (ScotlandYardPlayer player:players) {
			if (colour == player.colour()) {
				if (colour == BLACK)
					return Optional.of(0);
				return Optional.of(player.location());
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		for (ScotlandYardPlayer player:players) {
			if (colour == player.colour())
				return Optional.of(player.tickets().get(ticket));
		}
		return Optional.empty();
	}

	@Override
	public boolean isGameOver() {
		return false;
	}

	@Override
	public Colour getCurrentPlayer() {
		return currentPlayer;
	}

	@Override
	public int getCurrentRound() {
		return round;
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
        return new ImmutableGraph<>(graph);
	}

}
