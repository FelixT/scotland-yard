package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.util.*;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;


// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private List<ScotlandYardPlayer> players;
	private int round = 0;
	private Colour currentPlayer = BLACK;

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {
		// TODO
		this.rounds = Objects.requireNonNull(rounds);
		this.graph = Objects.requireNonNull(graph);
		if(rounds.isEmpty())
			throw new IllegalArgumentException("Empty rounds");
		if(graph.isEmpty())
			throw new IllegalArgumentException("Empty map");
		if(mrX.colour != BLACK)
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

			if(!configuration.tickets.keySet().containsAll(allticketset))
				throw new IllegalArgumentException("Each player must have each ticket type even if their values are zero");

			if(configuration.colour != BLACK) {
				// check detectives have the right tickets
				if(configuration.tickets.get(DOUBLE) != 0)
					throw new IllegalArgumentException("Detectives should not have double tickets");
				if(configuration.tickets.get(SECRET) != 0)
					throw new IllegalArgumentException("Detectives should not have secret tickets");
			}
			ScotlandYardPlayer player = new ScotlandYardPlayer(configuration.player, configuration.colour, configuration.location, configuration.tickets);

		}

	}

	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void startRotate() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public boolean isGameOver() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Colour getCurrentPlayer() {
		// TODO
		//throw new RuntimeException("Implement me");
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
		// TODO
		throw new RuntimeException("Implement me");
	}

}
