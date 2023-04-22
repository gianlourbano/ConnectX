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

# Default parameters (can also be specified from command line)
CX = 6 7 4
REPS = 1

app:
	@$(JR) $(OPTIONS) $(MAIN_CLASS) $(CX) $(QUASI_RANDOM)

run: 
	@echo "Running..."
	@$(JR) $(OPTIONS) $(PLAYER_TESTER_CLASS) $(CX) $(PLAYER_CLASS) $(QUASI_RANDOM) -r $(REPS)

build: clean-build
	@echo "Building..."
	@mkdir -p $(BUILD_DIR)
	@$(JC) -cp "$(SRC_DIR)/" -d "$(BUILD_DIR)/" -sourcepath "$(SRC_DIR)/" "$(SRC_DIR)/$(PLAYER_FILE)"

# Removes both binaries and documentation
clean: clean-build 

# Removes binaries
clean-build:
	@$(RM) -rf $(BUILD_DIR)