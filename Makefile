run:
	java flip.sim.Simulator -t 10000 --players random random -n 30 -r 1 -l log.txt

gui:
	java flip.sim.Simulator -t 10000 --players random g8wall -n 180 -r 1 --gui -l log.txt

compile:
	javac flip/sim/*.java

clean:
	rm flip/*/*.class
