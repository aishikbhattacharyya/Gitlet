package gitlet;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author Aishik Bhattacharyya
 */
public class Main {
    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
        } else {
            Tree tree = new Tree();
            String rest = "";
            for (int i = 1; i < args.length; i++) {
                rest += args[i] + "";
            }
            String checkoutRest = rest;
            if (rest.contains("-")) {
                if (rest.charAt(0) == '-') {
                    checkoutRest = "-- " + rest.substring(2);
                } else {
                    checkoutRest = rest.substring(0, rest.indexOf("-"))
                            + " -- " + rest.substring
                            (rest.indexOf("-") + 2);
                }
            }
            switch (args[0]) {
            case "init" -> tree.init();
            case "add" -> tree.add(args[1]);
            case "commit" -> tree.commit(rest);
            case "checkout" -> tree.checkout(checkoutRest);
            case "log" -> tree.log();
            case "global-log" -> tree.globalLog();
            case "status" -> tree.status();
            case "branch" -> tree.branch(args[1]);
            case "reset" -> tree.reset(args[1]);
            case "rm-branch" -> tree.removeBranch(args[1]);
            case "rm" -> tree.removeFile(args[1]);
            case "find" -> {
                String newRest = args[1];
                for (int i = 2; i < args.length; i++) {
                    newRest += args[i];
                }
                tree.find(newRest);
            }
            case "merge" -> tree.merge(rest);
            default -> System.out.println("No command with that name exists.");
            }
        }
    }
}
