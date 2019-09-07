Steps to run the simulator:
1) make compile
2) Update the make file with participating groups. (default is two random players)
3) make run OR make gui

Simulator arguments:

-n or --num_pieces : number of pieces for each player.

-p or --players: space separated players. if more than two players specified then games played in round-robin fashion.

-r or --runs: number of rounds between each pair of opponents. Players switch places at the end of each round.

-t or --turns: maximum number of turns per round.

-s or --seed: seed value for random.

-l or --log : enable logging

-v or --verbose : whether a verbose log should be recorded for the games when logging is enabled.

-g or --gui: enable gui

--fps : fps
