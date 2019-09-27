import os
import thread
import subprocess
import sys, time
from subprocess import Popen, list2cmdline, call


players = ['g1','g2','g3','g4','group5','g6','g7','g8wall','g9']

n_list = [20]
commands = []
for n in n_list:
	path ='tournament_logs/pieces_' +str(n)
	os.system('mkdir -p '+path)
	for i in range(len(players)):
		for  j in range(i+1,len(players)):

			log_file = players[i] + '_vs_' + players[j] + '.txt'
			log_file = os.path.join(path,log_file)
			cmd = 'java flip.sim.Simulator -t 3000 --players ' +  players[i] +' '+ players[j]+ ' -n ' + str(n) + ' -r 20  -l ' + log_file
			print(cmd)
			commands.append(cmd)



def cpu_count():
    ''' Returns the number of CPUs in the system
    '''
    num = 1
    if sys.platform == 'win32':
        try:
            num = int(os.environ['NUMBER_OF_PROCESSORS'])
        except (ValueError, KeyError):
            pass
    elif sys.platform == 'darwin':
        try:
            num = int(os.popen('sysctl -n hw.ncpu').read())
        except ValueError:
            pass
    else:
        try:
            num = os.sysconf('SC_NPROCESSORS_ONLN')
        except (ValueError, OSError, AttributeError):
            pass

    return num

def exec_commands(cmds):
    ''' Exec commands in parallel in multiple process 
    (as much as we have CPU)
    '''
    if not cmds: return # empty list

    def done(p):
        return p.poll() is not None
    def success(p):
        return p.returncode == 0
    def fail():
        sys.exit(1)

    max_task = cpu_count()
    processes = []
    while True:
        while cmds and len(processes) < max_task:
            task = cmds.pop()
	    print(task)
            processes.append(Popen(task,shell=True))

        for p in processes:
            if done(p):
                if success(p):
                    processes.remove(p)
                else:
                    fail()

        if not processes and not cmds:
            break
        else:
            time.sleep(0.05)

exec_commands(commands)
	
