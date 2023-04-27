# Java Commands
JC = javac
JR = java

# Directories
LIB_DIR = connectx
SRC_DIR = src
BUILD_DIR = build

# Packages and classes
MAIN_CLASS = connectx.CXGame
PLAYER_CLASS = schillaci.Schillaci
RANDOM = connectx.L0.L0
QUASI_RANDOM = connectx.L1.L1
PLAYER_TESTER_CLASS = connectx.CXPlayerTester

# Command line options
OPTIONS = -cp "$(BUILD_DIR)/"

# Source files
PLAYER_FILE = schillaci/Schillaci.java
MAIN_FILE = connectx/CXGame.java

ALL_FILES = $(wildcard $(SRC_DIR)/*.java) $(wildcard $(SRC_DIR)/*/*.java) $(wildcard $(SRC_DIR)/*/*/*.java)

# Default parameters (can also be specified from command line)
CX = 6 7 4
REPS = 50

PLAYER_2 = $(QUASI_RANDOM)

app:
	@$(JR) $(OPTIONS) $(MAIN_CLASS) $(CX) $(QUASI_RANDOM) $(PLAYER_CLASS) 

test:
	@py tester.py

run: 
	@echo "Running..."
	@$(JR) $(OPTIONS) $(PLAYER_TESTER_CLASS) $(CX) $(PLAYER_CLASS) $(PLAYER_2) -r $(REPS)
	@$(JR) $(OPTIONS) $(PLAYER_TESTER_CLASS) $(CX) $(PLAYER_2) $(PLAYER_CLASS) -r $(REPS)

build: clean-build $(ALL_FILES)
	@echo "Building..."
	@mkdir $(BUILD_DIR)
	@$(JC) -cp "$(SRC_DIR)/" -d "$(BUILD_DIR)/" -sourcepath "$(SRC_DIR)/" $(ALL_FILES)

# Removes both binaries and documentation
clean: clean-build 

# Removes binaries
clean-build:
	@rd /s /q $(BUILD_DIR)