run:
	java flip.sim.Simulator -t 10000 --players g6 g4 -n 5 -r 20 -l log.txt

gui:
	rm flip/*/*.class
	javac flip/sim/*.java
	java flip.sim.Simulator -t 200 --players g4 g6 -n 20 -r 5 --gui -l log.txt

compile:
	javac flip/sim/*.java

clean:
	rm flip/*/*.class
