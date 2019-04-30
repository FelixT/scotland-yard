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
	private List<ScotlandYardPlayer> players = new ArrayList<>();
	private List<Spectator> spectators = new ArrayList<>();
	private int round = 0;
	private Colour currentPlayer = BLACK;
	private int lastMrX = 0;
	private boolean wasmrx = false;
	private Map<Colour, ScotlandYardPlayer> colourMap = new HashMap<>();
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

	@Override
    public void visit(PassMove move) {
        System.out.println("Pass move");
        nextPlayer();
        System.out.println("Next player " + currentPlayer);

        System.out.println("On move made");
        for (Spectator spectator: spectators)
            spectator.onMoveMade(this, move);

        if (wasmrx) {
            round++;
            System.out.println("Increased round");
            System.out.println("On round started");

            for (Spectator spectator: spectators)
                spectator.onRoundStarted(this, round);

        }
    }

	@Override
	public void visit(TicketMove move) {
		System.out.println("Ticket move, " + move.ticket());

		for (ScotlandYardPlayer player : players) {
			if (player.colour() == currentPlayer) {
				player.location(move.destination());
				// decrease number of tickets of player
				int ticketsleft = player.tickets().get(move.ticket()) - 1;
				player.tickets().replace(move.ticket(), ticketsleft);
				if(currentPlayer != BLACK) {
					// detectives transfer their tickets to mr x
					for (ScotlandYardPlayer mrx : players) {
						if (mrx.colour() == BLACK) {
							int newtickets = mrx.tickets().get(move.ticket()) + 1;
							mrx.tickets().replace(move.ticket(), newtickets);
						}
					}
				}
			}
		}
		nextPlayer();
		System.out.println("Next player " + currentPlayer);

		System.out.println("On move made");
		for (Spectator spectator: spectators)
			spectator.onMoveMade(this, move);

		if (wasmrx) {
			round++;
			System.out.println("Increased round");
			System.out.println("On round started");

			for (Spectator spectator: spectators)
				spectator.onRoundStarted(this, round);

		}

	}

	@Override
	public void visit(DoubleMove move) {
		System.out.println("double move");

		// check if going from reveal to non reveal
		TicketMove firstmove = move.firstMove();
		if(rounds.get(round) && !rounds.get(round + 1))
			firstmove = new TicketMove(move.firstMove().colour(), move.firstMove().ticket(), 0);

		// perform logic to do moves

		nextPlayer();
		System.out.println("Next player " + currentPlayer);

		System.out.println("on move made");

		for (Spectator spectator: spectators)
			spectator.onMoveMade(this, move);


		round++;
		System.out.println("Increased round");
		System.out.println("On round started");

		for (Spectator spectator: spectators)
			spectator.onRoundStarted(this, round);

		System.out.println("on first move made");

		for (Spectator spectator: spectators)
			spectator.onMoveMade(this, firstmove);

		round++;
		System.out.println("Increased round");
		System.out.println("On round started");

		for (Spectator spectator: spectators)
			spectator.onRoundStarted(this, round);


		for (ScotlandYardPlayer player : players) {
			if (player.colour() == BLACK) {
				// decrease number of tickets of player
				int ticketsleft = player.tickets().get(move.firstMove().ticket()) - 1;
				player.tickets().replace(move.firstMove().ticket(), ticketsleft);
				player.location(move.firstMove().destination());

			}
		}


		System.out.println("on second move made");

		for (Spectator spectator: spectators)
			spectator.onMoveMade(this, move.secondMove());

		for (ScotlandYardPlayer player : players) {
			if (player.colour() == BLACK) {
				player.location(move.secondMove().destination());
				// decrease number of tickets of player
				int ticketsleft = player.tickets().get(move.secondMove().ticket()) - 1;
				player.tickets().replace(move.secondMove().ticket(), ticketsleft);
				int doubleticketsleft = player.tickets().get(DOUBLE) - 1;
				player.tickets().replace(DOUBLE, doubleticketsleft);
			}
		}
	}

	@Override
	public void accept(Move move) {
		System.out.println("Consumer.accept");

		if (currentPlayer == BLACK)
			wasmrx = true;

		if (move == null)
			throw new NullPointerException("Move can't be null");

		Set<Move> validMoves = validMoves(currentPlayer);

		//if (!validMoves.contains(move)) {
			//throw new IllegalArgumentException("Invalid move");
		//}

		move.visit(this);

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
		for (ScotlandYardPlayer player : players) {
			if (player.colour() != BLACK && player.location() == space)
				return false;
		}
		return true;
	}

	private Set<Move> validMoves(Colour colour) {

		ScotlandYardPlayer player = colourMap.get(colour);
		Node<Integer> playerNode = graph.getNode(player.location());
		Set<Move> moves = new HashSet<>();
		Move pass = new PassMove(colour);

		for (Edge<Integer, Transport> edge: graph.getEdgesFrom(playerNode)) {

			int canSecret;
			Ticket ticket = Ticket.fromTransport(edge.data());
			int destination = edge.destination().value();

			Move maybeSecretAdd = new TicketMove(colour, SECRET, destination);

			if (colour == BLACK && player.hasTickets(SECRET)) {
				if (player.hasTickets(SECRET, 2)) canSecret = 2;
				else canSecret = 1;
				moves.add(maybeSecretAdd);
			}

			if (player.hasTickets(ticket) && noDetectiveOnSpace(destination)) {

				Move moveToAdd = new TicketMove(colour, ticket, destination);
				moves.add(moveToAdd);

				if (player.hasTickets(DOUBLE)) {

					for (Edge<Integer, Transport> edge2: graph.getEdgesFrom(edge.destination())) {

						TicketMove move1 = new TicketMove(colour, ticket, destination);
						Ticket ticket2   = Ticket.fromTransport(edge2.data());
						int destination2 = edge2.destination().value();
						System.out.println(ticket);
						System.out.println(ticket2);

						// Check player has correct tickets for double move and detective not on final space,
						// either 2 of the same or different according to move being tried,
						// else moveToAdd remains unchanged and only single move is used.
						if (((ticket.name().equals(ticket2.name()) && player.hasTickets(ticket, 2))
								|| (!ticket.name().equals(ticket2.name()) && player.hasTickets(ticket2)))
								&& noDetectiveOnSpace(destination2)) {

							TicketMove move2 = new TicketMove(colour, ticket2, destination2);
							moveToAdd = new DoubleMove(colour, move1, move2);
							moves.add(moveToAdd);

						}

					}

				}

			}

		}

		if (player.colour() != BLACK && moves.isEmpty())
			moves.add(pass);

		return moves;

	}

	private void nextPlayer() {
		// get current player index
		int playerIndex = 0;

		for (ScotlandYardPlayer player: players) {
			if (player.colour() != currentPlayer)
				playerIndex++;
			else
				break;
		}

		int nextPlayer = (playerIndex + 1) % players.size();

		// get colour of next player
		currentPlayer = players.get(nextPlayer).colour();

	}

	/*
	private ScotlandYardPlayer playerFromColour(Colour colour) {

		for (ScotlandYardPlayer player : players) {
			if (player.colour() == colour) {
				return player;
			}
		}

		throw new IllegalArgumentException("Player does not exist");

	}*/

	@Override
	public void startRotate() {
		if(isGameOver()) {
			throw new IllegalStateException("Can't start new round when the game is already over");
		}

		System.out.println("Start rotate");
		System.out.println("Round " + round);
		System.out.println("Current player " + currentPlayer);

        Set<Move> moves = validMoves(currentPlayer);

		System.out.println("Make move");
		for (ScotlandYardPlayer player : players)
			if (player.colour() == currentPlayer)
				player.player().makeMove(this, player.location(), moves, this);

		System.out.println("On rotation complete");

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
		return unmodifiableSet(winners);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {

		for (ScotlandYardPlayer player:players) {

			if (colour == player.colour()) {
				if (colour == BLACK) {
					if(round != 0 && rounds.get(round - 1)) { // if its a reveal round give location
						lastMrX = player.location();
						return Optional.of(player.location());
					} else  // otherwise return 0
						return Optional.of(lastMrX);

				}

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
		boolean notickets = true;
		boolean playerinmrxposition = false;
		int mrxposition = -1;
		for (ScotlandYardPlayer mrx : players)
			if (mrx.colour() == BLACK)
				mrxposition = mrx.location();

		for (ScotlandYardPlayer player : players) {
			// check if any detective has tickets remaining
			if (player.colour() != BLACK && (player.hasTickets(TAXI) || player.hasTickets(BUS) || player.hasTickets(UNDERGROUND))) {
				notickets = false;
			}
			// check if detective in same position as mr x
			if (player.colour() != BLACK && player.location() == mrxposition) {
				playerinmrxposition = true;
			}
		}
		// check if max rounds exceeded
		boolean roundsexceeded = (round > rounds.size());

		if(notickets)
			winners.add(BLACK);
		if(roundsexceeded)
			winners.add(BLACK);
		if(playerinmrxposition) {
			// all detectives win
			for (ScotlandYardPlayer player : players)
				if (player.colour() != BLACK)
					winners.add(player.colour());
		}

		return (notickets || roundsexceeded || playerinmrxposition);
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
