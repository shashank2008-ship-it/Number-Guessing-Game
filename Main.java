import java.io.*;
import java.util.*;

public class Main {

    // Filenames used by the program (can be changed)
    private static final String INPUT_FILE = "input.txt";
    private static final String OUTPUT_FILE = "output.txt";
    private static final String SCORES_FILE = "scores.csv";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        FileManager fileManager = new FileManager(INPUT_FILE, OUTPUT_FILE, SCORES_FILE);

        try {
            // 1) Get player name from input file if present, otherwise prompt
            String nameFromFile = null;
            try {
                nameFromFile = fileManager.readPlayerNameFromInputFile();
            } catch (GameException ge) {
                System.out.println("Warning: couldn't read input file: " + ge.getMessage());
            }

            String playerName;
            if (nameFromFile != null && !nameFromFile.isEmpty()) {
                System.out.println("Using player name from input file: " + nameFromFile);
                playerName = nameFromFile;
            } else {
                System.out.print("Enter your name: ");
                playerName = scanner.nextLine().trim();
                while (playerName.isEmpty()) {
                    System.out.print("Name cannot be empty. Enter your name: ");
                    playerName = scanner.nextLine().trim();
                }
            }

            Player player = new Player(playerName);

            // 2) Load leaderboard into HashMap
            HashMap<String, ScoreRecord> leaderboard;
            try {
                leaderboard = fileManager.loadLeaderboard();
            } catch (GameException ge) {
                System.out.println("Warning: couldn't load leaderboard: " + ge.getMessage());
                leaderboard = new HashMap<>();
            }

            // Show leaderboard snapshot
            if (leaderboard.isEmpty()) {
                System.out.println("No leaderboard data yet.");
            } else {
                System.out.println("Leaderboard snapshot:");
                leaderboard.forEach((k, v) -> System.out.printf("  %s -> %s%n", k, v));
            }

            // 3) Configure and run the game
            int lower = 1;
            int upper = 100;
            int maxAttempts = 10; // 0 = unlimited
            GameManager gameManager = new GameManager(player, scanner, lower, upper, maxAttempts);
            GameResult result = gameManager.play();

            // 4) Update leaderboard using HashMap
            ScoreRecord sr = leaderboard.getOrDefault(player.getName(), new ScoreRecord());
            if (result.attempts > 0) { // Only record if player made guesses
                sr.recordGame(result.attempts, result.won);
            }
            leaderboard.put(player.getName(), sr);

            // 5) Save leaderboard and write output file
            try {
                fileManager.saveLeaderboard(leaderboard);
            } catch (GameException ge) {
                System.out.println("Warning: couldn't save leaderboard: " + ge.getMessage());
            }

            StringBuilder out = new StringBuilder();
            out.append("Player: ").append(player.getName()).append(System.lineSeparator());
            out.append("Secret Range: ").append(lower).append(" - ").append(upper).append(System.lineSeparator());
            out.append("Attempts this round: ").append(result.attempts).append(System.lineSeparator());
            out.append("Result: ").append(result.won ? "Guessed Correctly" : "Did not guess")
                    .append(System.lineSeparator());
            out.append(System.lineSeparator());
            out.append("Aggregated stats for ").append(player.getName()).append(System.lineSeparator());
            out.append("  Total attempts: ").append(sr.getTotalAttempts()).append(System.lineSeparator());
            out.append("  Games played: ").append(sr.getGamesPlayed()).append(System.lineSeparator());
            out.append("  Wins: ").append(sr.getWins()).append(System.lineSeparator());
            out.append("  Best attempts (lowest successful attempts): ").append(sr.getBestAttempts())
                    .append(System.lineSeparator());
            out.append(String.format("  Average attempts per game: %.2f%n", sr.averageAttempts()));

            try {
                fileManager.writeGameResult(out.toString());
                System.out.println("Game result saved to " + OUTPUT_FILE);
            } catch (GameException ge) {
                System.out.println("Warning: couldn't write output file: " + ge.getMessage());
            }

            // Final leaderboard print
            System.out.println("\nUpdated Leaderboard:");
            leaderboard.forEach((k, v) -> System.out.printf("  %s -> %s%n", k, v));

        } catch (IllegalArgumentException iae) {
            System.err.println("Invalid data: " + iae.getMessage());
        } catch (Exception ex) {
            System.err.println("Unexpected error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}

/*
 * ------------------------
 * Supporting classes below
 * (package-private classes within the same file)
 * ------------------------
 */

/**
 * Simple Player model demonstrating encapsulation.
 */
class Player {
    private final String name;

    public Player(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Player name cannot be null or empty.");
        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Player{name='" + name + "'}";
    }
}

/**
 * Holds aggregated stats for a player.
 */
class ScoreRecord {
    private int totalAttempts;
    private int gamesPlayed;
    private int wins;
    private int bestAttempts; // lowest successful attempts (Integer.MAX_VALUE means none yet)

    public ScoreRecord() {
        this.totalAttempts = 0;
        this.gamesPlayed = 0;
        this.wins = 0;
        this.bestAttempts = Integer.MAX_VALUE;
    }

    public ScoreRecord(int totalAttempts, int gamesPlayed, int wins, int bestAttempts) {
        this.totalAttempts = totalAttempts;
        this.gamesPlayed = gamesPlayed;
        this.wins = wins;
        this.bestAttempts = bestAttempts == 0 ? Integer.MAX_VALUE : bestAttempts;
    }

    public void recordGame(int attempts, boolean won) {
        this.totalAttempts += attempts;
        this.gamesPlayed++;
        if (won) {
            this.wins++;
            if (attempts < bestAttempts) {
                this.bestAttempts = attempts;
            }
        }
    }

    public double averageAttempts() {
        return gamesPlayed == 0 ? 0.0 : (double) totalAttempts / gamesPlayed;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getWins() {
        return wins;
    }

    public int getBestAttempts() {
        return bestAttempts == Integer.MAX_VALUE ? 0 : bestAttempts;
    }

    @Override
    public String toString() {
        return String.format("ScoreRecord{totalAttempts=%d,gamesPlayed=%d,wins=%d,bestAttempts=%d}",
                totalAttempts, gamesPlayed, wins, getBestAttempts());
    }
}

class GameException extends Exception {
    public GameException(String msg) {
        super(msg);
    }

    public GameException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

class FileManager {
    private final File inputFile;
    private final File outputFile;
    private final File scoresFile;

    public FileManager(String inputFileName, String outputFileName, String scoresFileName) {
        this.inputFile = new File(inputFileName);
        this.outputFile = new File(outputFileName);
        this.scoresFile = new File(scoresFileName);
    }

    /**
     * Read player name from input file (first non-empty line). Return null if file
     * missing or empty.
     */
    public String readPlayerNameFromInputFile() throws GameException {
        if (!inputFile.exists())
            return null;
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty())
                    return line.trim();
            }
            return null;
        } catch (IOException e) {
            throw new GameException("Error reading " + inputFile.getName(), e);
        }
    }

    /**
     * Write the game result summary (overwrites).
     */
    public void writeGameResult(String content) throws GameException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            bw.write(content);
        } catch (IOException e) {
            throw new GameException("Error writing " + outputFile.getName(), e);
        }
    }

    /**
     * Load leaderboard CSV into a HashMap.
     */
    public HashMap<String, ScoreRecord> loadLeaderboard() throws GameException {
        HashMap<String, ScoreRecord> map = new HashMap<>();
        if (!scoresFile.exists())
            return map;

        try (BufferedReader br = new BufferedReader(new FileReader(scoresFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                if (line.startsWith("#"))
                    continue;
                String[] parts = line.split(",");
                if (parts.length < 5)
                    continue; // skip malformed lines
                String name = parts[0].trim();
                int totalAttempts = Integer.parseInt(parts[1].trim());
                int gamesPlayed = Integer.parseInt(parts[2].trim());
                int wins = Integer.parseInt(parts[3].trim());
                int bestAttempts = Integer.parseInt(parts[4].trim());
                map.put(name, new ScoreRecord(totalAttempts, gamesPlayed, wins, bestAttempts));
            }
        } catch (IOException | NumberFormatException e) {
            throw new GameException("Error loading leaderboard from " + scoresFile.getName(), e);
        }
        return map;
    }

    /**
     * Save leaderboard HashMap into CSV (overwrites).
     */
    public void saveLeaderboard(HashMap<String, ScoreRecord> leaderboard) throws GameException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(scoresFile))) {
            bw.write("# name,totalAttempts,gamesPlayed,wins,bestAttempts");
            bw.newLine();
            for (Map.Entry<String, ScoreRecord> e : leaderboard.entrySet()) {
                String name = e.getKey();
                ScoreRecord sr = e.getValue();
                bw.write(String.format("%s,%d,%d,%d,%d",
                        name,
                        sr.getTotalAttempts(),
                        sr.getGamesPlayed(),
                        sr.getWins(),
                        sr.getBestAttempts()));
                bw.newLine();
            }
        } catch (IOException e) {
            throw new GameException("Error saving leaderboard to " + scoresFile.getName(), e);
        }
    }
}

/**
 * Encapsulates game logic.
 */
class GameManager {
    private final Player player;
    private final Scanner scanner;
    private final int lowerBound;
    private final int upperBound;
    private final int maxAttempts; // 0 = unlimited
    private final Random random;

    public GameManager(Player player, Scanner scanner, int lowerBound, int upperBound, int maxAttempts) {
        if (lowerBound >= upperBound)
            throw new IllegalArgumentException("lowerBound must be less than upperBound.");
        this.player = player;
        this.scanner = scanner;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.maxAttempts = Math.max(0, maxAttempts);
        this.random = new Random();
    }

    /**
     * Play one round and return a GameResult.
     */
    public GameResult play() {
        int secret = random.nextInt(upperBound - lowerBound + 1) + lowerBound;
        System.out.printf("Hello %s! I have chosen a number between %d and %d.%n",
                player.getName(), lowerBound, upperBound);
        System.out.println("Try to guess it. Type 'q' or 'quit' to give up.");

        int attempts = 0;
        boolean won = false;

        while (true) {
            if (maxAttempts > 0 && attempts >= maxAttempts) {
                System.out.println("Maximum attempts reached. Giving up.");
                break;
            }

            System.out.print("Enter your guess: ");
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("q") || line.equalsIgnoreCase("quit")) {
                System.out.println("You chose to quit. The secret number was: " + secret);
                break;
            }

            int guess;
            try {
                guess = Integer.parseInt(line);
            } catch (NumberFormatException nfe) {
                System.out.println(
                        "Invalid input. Please enter an integer between " + lowerBound + " and " + upperBound + ".");
                continue;
            }

            if (guess < lowerBound || guess > upperBound) {
                System.out.println("Guess out of range. Try again.");
                continue;
            }

            attempts++;
            if (guess == secret) {
                System.out.printf("Correct! You guessed the number in %d attempt%s.%n", attempts,
                        attempts == 1 ? "" : "s");
                won = true;
                break;
            } else if (guess < secret) {
                System.out.println("Too low. Try again.");
            } else {
                System.out.println("Too high. Try again.");
            }
        }

        return new GameResult(attempts, won);
    }
}

/**
 * Simple result holder for a played round.
 */
class GameResult {
    public final int attempts;
    public final boolean won;

    public GameResult(int attempts, boolean won) {
        this.attempts = attempts;
        this.won = won;
    }
}