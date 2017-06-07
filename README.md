# easy-car-multi
Easy game of car racing with client and server based on UDP and multithreading in Java

Several of packets needs confirm, so after receiving client or server send confirm of that packet.
Server open a new thread while a new player is connecting to game. Server is connecting through port 11331, it can be changed in GameServer class (static var PORT).
Client uses one thread to parse packets, and second to execute the game.
Car has to pass checkpoints on the track to count the lap, checkpoints are written in the .txt files, so far the starting position is.
For now only racetrack4 has configuration file and is ready to open and play.
After all laps (the number of laps is specified during starting the server) to all players are sending results of the race including usernames and time of race.

Steering:
KEY UP          - accelerate
KEY DOWN        - release/go back
KEY LEFT/RIGHT  - turn left/right
B               - left bomb if you got it from bonus point
SPACE           - left oil stain if you got it from bonus point
R               - reset to the starting position