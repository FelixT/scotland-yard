Scotland Yard Project Report
====


First we perform checks on the game info we have been given, making sure that it's a valid configuration for starting the game.
We make sure the configuration for every player has every ticket item in their ticket map, which was accomplished by creating a set with each type of ticket in, and checking this is equal the keys from the ticket map of every player, converted to a set. This was done using tickets.keySet().containsAll(allTicketSet). We also make sure that only Mr X has any 'secret' or 'double' tickets, that there are no two players are in the same location or have the same colour, and also that other arguments are not null or empty.

Provided none of the above errors occur, we create a ScotlandYardPlayer object from the confiration information we have been given for each player, which is then added to a LinkedHashMap, mapping from colours to ScotlandYardPlayer objects. This is useful as we often work in terms of player colours and need to find their player object based on that.

LinkedHashMap is used over a regular HashMap as it guaratees the order of its items if it is iterated through, which is needed for determining the next player in our code, as this needs to give consistent results.

There is also a basic AI for Mr X which, for each turn it has, picks the move it can make which places it as far as possible from its last known position, based on the assumption this is where detectives will attempt to head to.

The startRotate method is called by the User Interface, it first checks to see if the game is over (and throws an exception if so). The validMoves method is then called to get a set of moves to give to the current player (initially Mr X).
 
The validMoves method finds all the connections ('edges') from the node the player is on and cycles through them to find which the player can go to with the tickets they have, and whether or not the space is already occupied by a detective (Mr X may be on the space, this is a way to win), this applies to both detectives and Mr X. The logic is much more complicated for Mr X, primarily due to double and secret moves. Double moves can only be used by Mr X, only if he has Double Tickets left, and only if there are enough rounds left in the game. Each possible combination of additional moves from each connection is considered. For Mr X, this includes having the first, second, or both of the moves done as secret moves, and that there are enough secret tickets. If the correct tickets are present, the move (double, ticket, or secret) is added to the set of moves that will be passed back. A set containing a single PassMove is passed back if no other moves have been added by the end of the loop.

Once the player has selected their move, the method 'accept' is used as a callback. This accept method verifies if the move picked is valid, and if it is, it calls the visitor for the specific ticket used, as each type of ticket has a different outcome. 

Each visitor contains code which updates the tickets which each player should have, using the updateTickets method, updates the current player using method nextPlayer, and notifies spectators if the round has change, if a move has been made



## Achievements

- OO concepts?
- polymorphism used by moves: allows us to return e.g. a set containing 'Moves' while each of these might be PassMove, TicketMove or DoubleMove. Each of these inherits the Move class
- 'consumer' used for accept (callbacks)
- visitor model
- dynamic dispach (but i don't think we used this?)
- DRY principle
- abstraction
- model view controller?
- used LinkedHashMap
- encapsulation/information
- overloading/overriding, immutables and generics


Polymorphism is used for tickets, as PassMove, TicketMove and DoubleMove can all be referred to by parent class Move, asdone when returning a list of Move objects. Moves are also an example of inheritance, as move types mentioned above extend the base Move class.

Overriding was used extensive in the project, for example ScotlandYardModel implements ScotlandYardGame, so to implement the functions of ScotlandYardGame, @Override is used.

The 


## (brief) reflection

- However, there are some things which retrospectively could have been done better.
The code uses 'instanceof' is used in the function 'isGameOver', whereas ... would have provided a more object-oriented solution.
- Additionally, the AI given is 
Additionally, most of the project ended up in the class ScotlandYardModel, causing to contain many functions and be long as a result. To improve this the class could be split over multiple classes.