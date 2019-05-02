package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.util.*;
import java.util.function.Consumer;

import org.apache.commons.lang3.ObjectUtils;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.Node;


// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move>, MoveVisitor {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private List<Spectator> spectators = new ArrayList<>();
	private int round = 0;
	private Colour currentPlayer = BLACK;
	private int lastMrX = 0;
	private boolean wasmrx = false;
	private Map<Colour, ScotlandYardPlayer> colourMap = new LinkedHashMap<>(); // used to guarantee order
	private Set<Colour> winners = new HashSet<>();

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

		List<ScotlandYardPlayer> players = new ArrayList<>();

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

		for (ScotlandYardPlayer player : players) {
			colourMap.put(player.colour(), player);
		}

	}

    private void logicAfterMove() {
        if(isGameOver()) {
            for (Spectator spectator : spectators)
                spectator.onGameOver(this, getWinningPlayers());
        } else if (currentPlayer == BLACK) {
            for (Spectator spectator : spectators)
                spectator.onRotationComplete(this);
            System.out.println("On rotation complete");
        } else {

            Set<Move> moves = validMoves(currentPlayer);
	        giveMovesToCurrentPlayer(moves);
	        
        }
    }


	@Override
    public void visit(PassMove move) {
        System.out.println("Pass move");

        if (currentPlayer == BLACK) { // quite possibly redundant
            round++;
            System.out.println("--Increased round " + round);

            for (Spectator spectator : spectators)
                spectator.onRoundStarted(this, round);
			System.out.println("On round started");

        }

        nextPlayer();

        for (Spectator spectator: spectators)
			spectator.onMoveMade(this, move);
		System.out.println("On move made");

        System.out.println("--Next player " + currentPlayer);


    }

	@Override
	public void visit(TicketMove move) {
		System.out.println("Ticket move, " + move.ticket());

		TicketMove specmove = move;
		// if it's not a reveal round we give mrX's last known location, otherwise set last known location
		if(rounds.size() > round && currentPlayer == BLACK) {
            if (rounds.get(round))
                lastMrX = specmove.destination();
            else
                specmove = new TicketMove(move.colour(), move.ticket(), lastMrX);
        }

		ScotlandYardPlayer player = colourMap.get(currentPlayer);
		player.location(move.destination());

		// decrease number of tickets of player
		int ticketsleft = player.tickets().get(move.ticket()) - 1;
		player.tickets().replace(move.ticket(), ticketsleft);
		if(currentPlayer != BLACK) {
			// detectives transfer their tickets to mr x
			ScotlandYardPlayer mrx = colourMap.get(BLACK);
			int newtickets = mrx.tickets().get(move.ticket()) + 1;
			mrx.tickets().replace(move.ticket(), newtickets);
		}

		nextPlayer();

        if (wasmrx) { // quite possibly redundant
            round++;
            System.out.println("Increased round " + round);

            for (Spectator spectator : spectators)
                spectator.onRoundStarted(this, round);
            System.out.println("On round started");
        }

        for (Spectator spectator : spectators)
			spectator.onMoveMade(this, specmove);
        System.out.println("On move made");

        System.out.println("--Next player" + currentPlayer);

        //startRotate();
	}

	@Override
	public void visit(DoubleMove move) {
		System.out.println("double move");

		ScotlandYardPlayer mrx = colourMap.get(BLACK);

		TicketMove firstmove = move.firstMove();
		TicketMove secondmove = move.secondMove();

		boolean revealOne = (rounds.size() > round) && rounds.get(round);
		boolean revealTwo = (rounds.size() > round) && rounds.get(round + 1);

        if (!revealOne && revealTwo)
            firstmove = new TicketMove(move.firstMove().colour(), move.firstMove().ticket(), lastMrX);
        if (revealOne && !revealTwo)
            secondmove = new TicketMove(move.secondMove().colour(), move.secondMove().ticket(), firstmove.destination());
        if (!revealOne && !revealTwo) {
            firstmove = new TicketMove(move.firstMove().colour(), move.firstMove().ticket(), lastMrX);
            secondmove = new TicketMove(move.secondMove().colour(), move.secondMove().ticket(), lastMrX);
        }

        DoubleMove specmove = new DoubleMove(currentPlayer, firstmove, secondmove);

		nextPlayer();
		System.out.println("--Next player " + currentPlayer);

		// decrease number of double tickets in mrX's posession
		int doubleticketsleft = mrx.tickets().get(DOUBLE) - 1;
		mrx.tickets().replace(DOUBLE, doubleticketsleft);

		for (Spectator spectator : spectators)
			spectator.onMoveMade(this, specmove);
        System.out.println("on overall move made");

		// -- FIRST MOVE --

		// MrX loses ticket used for their first move
		int ticketsleft = mrx.tickets().get(move.firstMove().ticket()) - 1;
		mrx.tickets().replace(move.firstMove().ticket(), ticketsleft);

		mrx.location(move.firstMove().destination());

		round++;
		System.out.println("--Increased round " + round);

		// if either round is hidden we display the location as mrX's last known location instead
		if (revealOne)
			lastMrX = firstmove.destination();

		for (Spectator spectator : spectators)
			spectator.onRoundStarted(this, round);
		System.out.println("On round started");

		for (Spectator spectator : spectators)
			spectator.onMoveMade(this, firstmove);
		System.out.println("on first move made");

		// --SECOND MOVE--

		// decrease number of tickets for second move
		ticketsleft = mrx.tickets().get(move.secondMove().ticket()) - 1;
		mrx.tickets().replace(move.secondMove().ticket(), ticketsleft);

		mrx.location(move.secondMove().destination());
		// if either round is hidden we display the location as mrX's last known location instead
		if (revealTwo)
			lastMrX = secondmove.destination();

		// next rounds starts after first move made
		round++;
		System.out.println("--Increased round" + round);

		for (Spectator spectator : spectators)
			spectator.onRoundStarted(this, round);
		System.out.println("On round started");

		for (Spectator spectator : spectators)
			spectator.onMoveMade(this, secondmove);
		System.out.println("on second move made");

	}

	@Override
	public void accept(Move move) {
		System.out.println("Consumer.accept");

		wasmrx = (currentPlayer == BLACK);

		if (move == null)
			throw new NullPointerException("Move can't be null");

		Set<Move> validMoves = validMoves(currentPlayer);

		if (!validMoves.contains(move)) {
			throw new IllegalArgumentException("Invalid move");
		}

		move.visit(this);

		logicAfterMove();

	}

	@Override
	public void registerSpectator(Spectator spectator) {
		if(spectators.contains(spectator))
			throw new IllegalArgumentException("Spectator already exists");
		else
			spectators.add(Objects.requireNonNull(spectator));
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		if(spectator == null)
			throw new NullPointerException("Spectator can't be null");
		else if(!spectators.contains(spectator))
			throw new IllegalArgumentException("Spectator has never been added");
		else
			spectators.remove(spectator);
	}

	private boolean noDetectiveOnSpace(int space) {

		for (ScotlandYardPlayer player : colourMap.values()) {
			if (player.colour() != BLACK && player.location() == space)
				return false;
		}

		return true;

	}

	private Set<Move> validMoves(Colour colour) {

		ScotlandYardPlayer player = colourMap.get(colour);
		Node<Integer> playerNode  = graph.getNode(player.location());
		Set<Move> moves = new HashSet<>();

		for (Edge<Integer, Transport> edge : graph.getEdgesFrom(playerNode)) {

			Ticket ticket = Ticket.fromTransport(edge.data());
			int destination = edge.destination().value();
		
			if ((player.hasTickets(ticket) || player.hasTickets(SECRET)) && noDetectiveOnSpace(destination)) {

				if (player.hasTickets(ticket))
					moves.add(new TicketMove(colour, ticket, destination));

				if (player.hasTickets(SECRET))
					moves.add(new TicketMove(colour, SECRET, destination));

				if (player.hasTickets(DOUBLE) && round < rounds.size()-2) {

					for (Edge<Integer, Transport> edge2 : graph.getEdgesFrom(edge.destination())) {

						TicketMove move1 = new TicketMove(colour, ticket, destination);
						Ticket ticket2   = Ticket.fromTransport(edge2.data());
						int destination2 = edge2.destination().value();

						if (noDetectiveOnSpace(destination2)) {

							TicketMove move2 = new TicketMove(colour, ticket2, destination2);
							boolean ticketsSame = ticket == ticket2;

							if ((ticketsSame && player.hasTickets(ticket, 2)) ||
									(!ticketsSame && player.hasTickets(ticket) && player.hasTickets(ticket2)))
								moves.add(new DoubleMove(colour, move1, move2));

							if (player.hasTickets(SECRET)) {

								TicketMove move1Secret = new TicketMove(colour, SECRET, destination);
								TicketMove move2Secret = new TicketMove(colour, SECRET, destination2);

								if (ticket != SECRET && ticket2 != SECRET && player.hasTickets(ticket))
									moves.add(new DoubleMove(colour, move1, move2Secret));

								if (ticket != SECRET && ticket2 != SECRET && player.hasTickets(ticket2))
									moves.add(new DoubleMove(colour, move1Secret, move2));

								if (player.hasTickets(SECRET, 2))
									moves.add(new DoubleMove(colour, move1Secret, move2Secret));

							}
						}
					}
				}
			}
		}

		if (moves.isEmpty())
			moves.add(new PassMove(colour));

		return moves;

	}

	private void nextPlayer() {

		// get current player index
		int playerIndex = getPlayers().indexOf(currentPlayer);
		int nextPlayer = (playerIndex + 1) % colourMap.size();

		// get colour of next player
		currentPlayer = getPlayers().get(nextPlayer);

	}

	@Override
	public void startRotate() {

		if(isGameOver()) {
			throw new IllegalStateException("Can't start new round when the game is already over");
		}

		System.out.println("---Start rotate");
		System.out.println("Round " + round);
		System.out.println("Current player " + currentPlayer);

        Set<Move> moves = validMoves(currentPlayer);
		giveMovesToCurrentPlayer(moves);

	}

	private void giveMovesToCurrentPlayer(Set<Move> moves) {

		System.out.println("Make move");
		ScotlandYardPlayer player = colourMap.get(currentPlayer);
		player.player().makeMove(this, player.location(), moves, this);

	}

	@Override
	public Collection<Spectator> getSpectators() {
		return unmodifiableList(spectators);
	}

	@Override
	public List<Colour> getPlayers() {

		List<Colour> playerColours = new ArrayList<>();

		for (ScotlandYardPlayer player : colourMap.values())
			playerColours.add(Objects.requireNonNull(player.colour()));

		return Collections.unmodifiableList(playerColours);

	}

	@Override
	public Set<Colour> getWinningPlayers() {

        isGameOver(); // isGameOver updates the 'winners' set.
		return unmodifiableSet(winners);

	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {

		if(colourMap.containsKey(colour)) { // If the requested player exists..

			ScotlandYardPlayer player = colourMap.get(colour);
			final Optional<Integer> location = Optional.of(player.location());

			if (colour == BLACK) {

				if (round != 0 && rounds.get(round - 1)) {

					// If its a reveal round, give Mr X's location.
					lastMrX = player.location();
					return location;

				}

				// Otherwise return lastMrX.
				return Optional.of(lastMrX);

			}

			// If a detective, always return location.
			return location;

		}

		return Optional.empty();

	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {

		ScotlandYardPlayer player = colourMap.get(colour);

		if (player == null)
			return Optional.empty();

		return Optional.of(player.tickets().get(ticket));

	}

	@Override
	public boolean isGameOver() {

	    winners.clear();

		boolean playerInMrXPosition = false;
		ScotlandYardPlayer mrX = colourMap.get(BLACK);
		int mrXPosition = mrX.location();
		boolean noMoves = true;
		boolean mrXStuck = (validMoves(BLACK).iterator().next() instanceof PassMove) && currentPlayer == BLACK;

		for (ScotlandYardPlayer player : colourMap.values()) {

			// Check if detective in same position as Mr X.
			if (player.colour() != BLACK && player.location() == mrXPosition)
				playerInMrXPosition = true;

			// Check if any detective has any moves remaining.
			Move pass = new PassMove(player.colour());
			boolean playerHasNoPassMove = !(validMoves(player.colour()).iterator().next() instanceof PassMove);

			if (player != mrX && playerHasNoPassMove)
				noMoves = false;

		}

		// Check if max rounds exceeded.
		boolean roundsExceeded = (round >= rounds.size() && currentPlayer == BLACK);

		if(playerInMrXPosition || mrXStuck) {

			// Add all detectives to winning players.
			for (ScotlandYardPlayer player : colourMap.values()) {
				if (player.colour() != BLACK)
					winners.add(player.colour());
			}

		} else if(roundsExceeded || noMoves) {

            winners.add(BLACK);

		}

		// Return true if any of the game over conditions are satisfied.
		// Winners are stored outside the function so are still accessible after return.
		return (noMoves || roundsExceeded || playerInMrXPosition || mrXStuck);

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
