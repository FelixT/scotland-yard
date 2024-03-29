package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Collections.*;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.util.*;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.Node;

public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move>, MoveVisitor {

	private int round = 0;
	private int lastMrX = 0;
	private List<Boolean> rounds;
	private Colour currentPlayer = BLACK;
	private Graph<Integer, Transport> graph;
	private Set<Colour> winners = new HashSet<>();
	private List<Spectator> spectators = new ArrayList<>();
	private Map<Colour, ScotlandYardPlayer> colourMap = new LinkedHashMap<>(); // Used to guarantee order.

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

		Set<Ticket> allTicketSet = new HashSet<>();
		allTicketSet.add(Ticket.TAXI);
		allTicketSet.add(Ticket.BUS);
		allTicketSet.add(Ticket.UNDERGROUND);
		allTicketSet.add(Ticket.DOUBLE);
		allTicketSet.add(Ticket.SECRET);

		Set<Integer> locationSet = new HashSet<>();
		Set<Colour> colourSet    = new HashSet<>();

		for (PlayerConfiguration configuration : configurations) {

			if (locationSet.contains(configuration.location))
				throw new IllegalArgumentException("Duplicate location");

			if (colourSet.contains(configuration.colour))
				throw new IllegalArgumentException("Duplicate colour");

			locationSet.add(configuration.location);
			colourSet.add(configuration.colour);

			if (!configuration.tickets.keySet().containsAll(allTicketSet))
				throw new IllegalArgumentException("Each player must have each ticket type even if their values are zero");

			if (configuration.colour != BLACK) {

				// Check detectives have the right tickets.
				if (configuration.tickets.get(DOUBLE) != 0)
					throw new IllegalArgumentException("Detectives should not have double tickets");

				if (configuration.tickets.get(SECRET) != 0)
					throw new IllegalArgumentException("Detectives should not have secret tickets");

			}

			ScotlandYardPlayer player = new ScotlandYardPlayer(configuration.player, configuration.colour, configuration.location, configuration.tickets);
			Objects.requireNonNull(player);
			colourMap.put(player.colour(), player);

		}

	}

	/**
	 * Check if game is over, if so notify.
	 * If not, if the rotation is complete, notify.
	 * Otherwise continue rotation.
	 */
    private void logicAfterMove() {

        if (isGameOver()) {

            for (Spectator spectator : spectators)
                spectator.onGameOver(this, getWinningPlayers());

        } else if (currentPlayer == BLACK) {

            for (Spectator spectator : spectators)
                spectator.onRotationComplete(this);

        } else {

        	// Continue the rotation by prompting the next player to move.
            Set<Move> moves = validMoves(currentPlayer);
	        giveMovesToCurrentPlayer(moves);
	        
        }

    }

	/**
	 * Updates the number of tickets of a type the player should have by a specified number.
	 * @param playerColour the colour of the player for whom the tickets are to be updated.
	 * @param ticket the ticket type to be changed.
	 * @param change the number by which the tickets should be changed (positive = increase).
	 */
	private void updateTickets(Colour playerColour, Ticket ticket, int change) {

	    ScotlandYardPlayer player = colourMap.get(playerColour);
        int ticketsAfter = player.tickets().get(ticket) + change;
        player.tickets().replace(ticket, ticketsAfter);

	}

	/**
	 * Deals with pass move. Calls nextPlayer, notifies spectators,
	 * increments round if was Mr X.
	 * @param move the pass move to be processed.
	 */
	@Override
    public void visit(PassMove move) {

        if (currentPlayer == BLACK) {

            round++;
	        notifySpectatorsRoundStarted();

        }

        nextPlayer();
        notifySpectatorsMoveMade(move);

    }

	/**
	 * Deals with ticket move and whether to reveal location.
	 * Updates tickets and calls nextPlayer, updates location,
	 * notifies spectators, increments round if was Mr X.
	 * @param move the ticket move to be processed.
	 */
	@Override
	public void visit(TicketMove move) {

		TicketMove specMove = move;

		// If it's not a reveal round we give mrX's last known location, otherwise set last known location.
		if (rounds.size() > round && currentPlayer == BLACK) {

            if (rounds.get(round))
                lastMrX  = specMove.destination();
            else
                specMove = new TicketMove(move.colour(), move.ticket(), lastMrX);

        }

		ScotlandYardPlayer player = colourMap.get(currentPlayer);
		player.location(move.destination());

		// Decrease number of tickets of player.
		updateTickets(currentPlayer, move.ticket(), -1);

        // Detectives transfer their tickets to Mr X.
		if (currentPlayer != BLACK)
            updateTickets(BLACK, move.ticket(), 1);

		boolean wasMrX = (currentPlayer == BLACK);
		nextPlayer();

		// If player was Mr X then it's the end of the round, so we start the next.
        if (wasMrX) {

            round++;
	        notifySpectatorsRoundStarted();

        }

		notifySpectatorsMoveMade(specMove);

	}

	/**
	 * Deals with double move and what to reveal location-wise if anything.
	 * Updates tickets and calls nextPlayer, updates location, notifies
	 * spectators, increments round if was Mr X.
	 * @param move the double move to be processed.
	 */
	@Override
	public void visit(DoubleMove move) {

		ScotlandYardPlayer mrX = colourMap.get(BLACK);

		TicketMove firstMove  = move.firstMove();
		TicketMove secondMove = move.secondMove();

		boolean revealOne = (rounds.size() > round) && rounds.get(round);
		boolean revealTwo = (rounds.size() > round) && rounds.get(round + 1);

		// If one of the moves occurs during a hidden round we change its destination to the last revealed location.
        if (!revealOne && revealTwo)
            firstMove  = new TicketMove(move.firstMove().colour(), move.firstMove().ticket(), lastMrX);
        if (revealOne && !revealTwo)
            secondMove = new TicketMove(move.secondMove().colour(), move.secondMove().ticket(), firstMove.destination());
        if (!revealOne && !revealTwo) {
            firstMove  = new TicketMove(move.firstMove().colour(), move.firstMove().ticket(), lastMrX);
            secondMove = new TicketMove(move.secondMove().colour(), move.secondMove().ticket(), lastMrX);
        }

        DoubleMove specMove = new DoubleMove(currentPlayer, firstMove, secondMove);

		nextPlayer();

		// Decrease number of double tickets in mrX's possession.
		updateTickets(BLACK, DOUBLE, -1);

		notifySpectatorsMoveMade(specMove);

		// -- FIRST MOVE --

		// Mr X loses ticket used for their first move.
		updateTickets(BLACK, move.firstMove().ticket(), -1);

		mrX.location(move.firstMove().destination());

		round++;

		// If either round is hidden we display the location as Mr X's last known location instead.
		if (revealOne)
			lastMrX = firstMove.destination();

		notifySpectatorsRoundStarted();

		notifySpectatorsMoveMade(firstMove);

		// --SECOND MOVE--

		// Decrease number of tickets for second move.
        updateTickets(BLACK, move.secondMove().ticket(), -1);

		mrX.location(move.secondMove().destination());

		// If either round is hidden we display the location as Mr X's last known location instead.
		if (revealTwo)
			lastMrX = secondMove.destination();

		// Next rounds starts after first move made.
		round++;

		notifySpectatorsRoundStarted();

		notifySpectatorsMoveMade(secondMove);

	}

	/**
	 * Notify all spectators in spectator list that a round has started.
	 */
	private void notifySpectatorsRoundStarted() {

		for (Spectator spectator : spectators)
			spectator.onRoundStarted(this, round);

	}

	/**
	 * Notifies all spectators in spectator list of a move made.
	 * @param move to notify spectators of.
	 */
	private void notifySpectatorsMoveMade(Move move) {

		for (Spectator spectator : spectators)
			spectator.onMoveMade(this, move);

	}

	/**
	 * Accept move (if valid and non-null) and progress with the game (logicAfterMove).
	 * @param move to process.
	 */
	@Override
	public void accept(Move move) {

		if (move == null)
			throw new NullPointerException("Move can't be null");

		Set<Move> validMoves = validMoves(currentPlayer);

		if (!validMoves.contains(move))
			throw new IllegalArgumentException("Invalid move");

		move.visit(this);

		logicAfterMove();

	}

	/**
	 * Adds a spectator to the spectator list (unless null or already added).
	 * @param spectator to register to list.
	 */
	@Override
	public void registerSpectator(Spectator spectator) {

		if (spectators.contains(spectator))
			throw new IllegalArgumentException("Spectator already exists");
		else
			spectators.add(Objects.requireNonNull(spectator));

	}

	/**
	 * Removes a given spectator from spectator list (unless null or never added).
	 * @param spectator to unregister.
	 */
	@Override
	public void unregisterSpectator(Spectator spectator) {

		if (spectator == null)
			throw new NullPointerException("Spectator can't be null");
		else if (!spectators.contains(spectator))
			throw new IllegalArgumentException("Spectator has never been added");
		else
			spectators.remove(spectator);

	}

	/**
	 * Check if a detective is on a particular space.
	 * @param space the node number / location of a space to check for detectives on.
	 * @return true if space occupied by detective, false otherwise.
	 */
	private boolean noDetectiveOnSpace(int space) {

		for (ScotlandYardPlayer player : colourMap.values()) {
			if (player.colour() != BLACK && player.location() == space)
				return false;
		}

		return true;

	}

	/**
	 * Generates the set of valid moves for a player. If no moves are
	 * possible, a single PassMove is added to the set.
	 * @param colour of player for whom valid moves should be generated.
	 * @return Set of valid moves the player can make.
	 */
	private Set<Move> validMoves(Colour colour) {

		ScotlandYardPlayer player = colourMap.get(colour);
		Node<Integer> playerNode  = graph.getNode(player.location());

		Set<Move> moves = new HashSet<>();

		for (Edge<Integer, Transport> edge : graph.getEdgesFrom(playerNode)) {

			Ticket ticket   = Ticket.fromTransport(edge.data());
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
							boolean ticketsSame = (ticket == ticket2);

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

	/**
	 * Update the currentPlayer to be the next player.
	 */
	private void nextPlayer() {

		// Get current player index.
		int playerIndex = getPlayers().indexOf(currentPlayer);
		int nextPlayer = (playerIndex + 1) % colourMap.size();

		// Get colour of next player.
		currentPlayer = getPlayers().get(nextPlayer);

	}

	/**
	 * Start a rotation by passing validMoves to the first player (Mr X).
	 */
	@Override
	public void startRotate() {

		if (isGameOver())
			throw new IllegalStateException("Can't start new round when the game is already over");

        Set<Move> moves = validMoves(currentPlayer);
		giveMovesToCurrentPlayer(moves);

	}

	/**
	 * Passes the set of valid moves to the current player.
	 * @param moves the set of valid moves the current player can make.
	 */
	private void giveMovesToCurrentPlayer(Set<Move> moves) {

		ScotlandYardPlayer player = colourMap.get(currentPlayer);
		player.player().makeMove(this, player.location(), moves, this);

	}

	@Override
	public Collection<Spectator> getSpectators() {
		return unmodifiableList(spectators);
	}

	/**
	 * @return list of all players by colour.
	 */
	@Override
	public List<Colour> getPlayers() {

		List<Colour> playerColours = new ArrayList<>();

		for (ScotlandYardPlayer player : colourMap.values())
			playerColours.add(Objects.requireNonNull(player.colour()));

		return Collections.unmodifiableList(playerColours);

	}

	/**
	 * @return set of winners
	 */
	@Override
	public Set<Colour> getWinningPlayers() {

        isGameOver(); // isGameOver updates the 'winners' set.
		return unmodifiableSet(winners);

	}

	/**
	 * Gives the location of detective requested. If Mr X requested,
	 * location given in reveal round, or lastMrX if not.
	 * @param colour The colour of the player whose location is requested.
	 * @return location of player, or empty if player does not exist.
	 */
	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {

		if (colourMap.containsKey(colour)) { // Check if the player exists / is in use.

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

	/**
	 * Gives the number of tickets of a specified type a player has.
	 * @param colour The colour of the player whose tickets are requested.
	 * @param ticket The type of tickets that is being requested.
	 * @return number of tickets, or empty if player doesn't exist.
	 */
	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {

		ScotlandYardPlayer player = colourMap.get(colour);

		if (player == null)
			return Optional.empty();

		return Optional.of(player.tickets().get(ticket));

	}

	/**
	 * Determines whether or not the game is over.
	 * If over, adds the winner(s) to 'winners'.
	 * @return true if game over, or false otherwise.
	 */
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
			boolean playerHasNoPassMove = !(validMoves(player.colour()).iterator().next() instanceof PassMove);

			if (player != mrX && playerHasNoPassMove)
				noMoves = false;

		}

		// Check if max rounds exceeded.
		boolean roundsExceeded = (round >= rounds.size() && currentPlayer == BLACK);

		if (playerInMrXPosition || mrXStuck) {

			// Add all detectives to winning players.
			for (ScotlandYardPlayer player : colourMap.values()) {
				if (player.colour() != BLACK)
					winners.add(player.colour());
			}

		} else if (roundsExceeded || noMoves) {

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
