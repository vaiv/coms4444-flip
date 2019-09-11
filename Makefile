run:
	java flip.sim.Simulator -t 10000 --players random random -n 30 -r 10 -l log.txt

gui:
	java flip.sim.Simulator -t 10000 --players greedy beginner -n 30 -r 1 --gui -l log.txt

compile:
	javac flip/sim/*.java

clean:
	rm flip/*/*.class