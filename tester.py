import subprocess;
import re;
import tabulate;

configs = ["4 4 4",
           "5 4 4",
           "6 4 4",
           "7 4 4",
           "4 5 4",
           "5 5 4",
           "6 5 4",
           "7 5 4",
           "4 6 4",
           "5 6 4",
           "6 6 4",
           "7 6 4",
           "4 7 4",
           "5 7 4",
           "6 7 4",
           "7 7 4",
           "5 4 5",
           "6 4 5",
           "7 4 5",
           "4 5 5",
           "5 5 5",
           "6 5 5",
           "7 5 5",
           "4 6 5",
           "5 6 5",
           "6 6 5",
           "7 6 5",
           "4 7 5",
           "5 7 5",
           "6 7 5",
           "7 7 5",
           "25 25 10",
           "50 50 15",
           "75 75 20",
           "100 100 30"
		   ]

class player:
	name = ""
	score = 0
	wins = 0;
	losses = 0;
	ties = 0;
	errors = 0;

	def __init__(self, name, score):
		self.name = name
		self.score = score

	def update(self, score, wins, losses, ties, errors):
		self.score += score
		self.wins += wins
		self.losses += losses
		self.ties += ties
		self.errors += errors

	def __lt__(self, other):
		return self.score < other.score
	
	def __eq__(self, other):
		return self.score == other.score

	def to_print(self):
		return [self.name[self.name.rfind(".")+1:], self.score, self.wins, self.losses, self.ties, self.errors]

player1 = player("schillaci.Schillaci", 0)
player2 = player("connectx.L0.L0", 0)
player3 = player("connectx.L1.L1", 0)
players = [player1, player2, player3]

def play(player1: player, player2: player):
	for config in configs:
		ret = subprocess.check_output("java -cp build/ connectx.CXPlayerTester " + config + " " + player1.name + " " + player2.name, shell=True)
		ret = ret.decode("utf-8")

		name1 = player1.name[player1.name.rfind(".")+1:]
		name2 = player2.name[player2.name.rfind(".")+1:]

		ret = ret.replace(name1, "PLAYER")
		ret = ret.replace(name2, "PLAYER")

		arr = re.findall("[0-9]+", ret)
		player1.update(int(arr[0]), int(arr[1]), int(arr[2]), int(arr[3]), int(arr[4]))
		player2.update(int(arr[-5]), int(arr[-4]), int(arr[-3]), int(arr[-2]), int(arr[-1]))

	for config in configs:
		ret = subprocess.check_output("java -cp build/ connectx.CXPlayerTester " + config + " " + player2.name + " " + player1.name, shell=True)		
		ret = ret.decode("utf-8")

		name1 = player1.name[player1.name.rfind(".")+1:]
		name2 = player2.name[player2.name.rfind(".")+1:]

		ret = ret.replace(name1, "PLAYER")
		ret = ret.replace(name2, "PLAYER")
		
		arr = re.findall("[0-9]+", ret)
		player2.update(int(arr[0]), int(arr[1]), int(arr[2]), int(arr[3]), int(arr[4]))
		player1.update(int(arr[-5]), int(arr[-4]), int(arr[-3]), int(arr[-2]), int(arr[-1]))

play(player1, player2)
play(player1, player3)
play(player2, player3)

players.sort(reverse=True)

table = [["Name", "Score", "Wins", "Losses", "Ties", "Errors"]]
for player in players:
	table.append(player.to_print())

print(tabulate.tabulate(table, headers="firstrow", tablefmt="fancy_grid"))
