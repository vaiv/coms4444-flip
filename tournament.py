import numpy
import os
import thread


players = ['g1','g2','g3','g4','group5','g6','g7','g8wall','g9']

n_list = [1, 5]

for n in n_list:
	path ='tournament_logs/pieces_' +str(n)
	os.system('mkdir '+path)
	for i in range(len(players)):
		for  j in range(i+1,len(players)):

			log_file = players[i] + '_vs_' + players[j] + '.txt'
			log_file = os.path.join(path,log_file)
			cmd = 'java flip.sim.Simulator -t 3000 --players ' +  players[i] +' '+ players[j]+ ' -n ' + str(n) + ' -r 20  -l ' + log_file
			print(cmd)
			thread.start_new_thread(os.system, (cmd,))

