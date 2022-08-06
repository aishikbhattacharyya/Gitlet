package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class Branch implements Serializable {
    public Branch(String name) {
        this._name = name;
        this._ID = Utils.sha1(Utils.serialize(this));
        this._commits = new LinkedHashMap<>();
    }

    /**
     * Returns commit history of branch.
     * @return Commit history of branch.
     */
    public LinkedHashMap<String, String> getCommits() {
        return _commits;
    }

    /**
     * Sets commit history of branch.
     * @param commits New commit history.
     */
    public void setCommits(LinkedHashMap<String, String> commits) {
        _commits = commits;
    }

    /**
     * Sets new SHA1 ID of head commit.
     * @param head New SHA1 ID of head commit.
     */
    public void setHead(String head) {
        _head = head;
    }

    /**
     * Returns SHA1 ID of branch.
     * @return SHA1 ID of branch.
     */
    public String getID() {
        return _ID;
    }

    /**
     * Returns name of branch.
     * @return Name of branch.
     */
    public String getName() {
        return _name;
    }

    /**
     * Returns SHA1 ID of head commit.
     * @return SHA1 ID of head commit.
     */
    public String getHeadCommit() {
        return _head;
    }

    /**
     * Adds a new commit to branch's commit history.
     * @param newCommit New commit to add.
     */
    public void addCommit(Commit newCommit) {
        _commits.put(newCommit.getID(), "");
        _head = newCommit.getID();
    }

    /**
     * Calculate split point of two branches for merging purposes.
     * @param currentBranch Current branch to merge on.
     * @param otherBranch Other branch to merge with.
     * @return SHA1 ID of split point commit.
     */
    public static String splitPoint(Branch currentBranch, Branch otherBranch) {
        String currentBranchHead = currentBranch._head;
        String otherBranchHead = otherBranch._head;

        ArrayList<String> allCurrentCommits = Branch.bfs(currentBranchHead);
        ArrayList<String> otherBranchCommits = Branch.bfs(otherBranchHead);

        for (String commit : allCurrentCommits) {
            if (otherBranchCommits.contains(commit)) {
                return commit;
            }
        }
        return "";
    }

    /**
     * Helper breadth-first-search method to find commit's ancestor history.
     * @param branchHead SHA1 ID of root.
     * @return Commit's ancestors.
     */
    private static ArrayList<String> bfs(String branchHead) {
        LinkedList<String> queue = new LinkedList<>();
        ArrayList<String> visited = new ArrayList<>();
        ArrayList<String> result = new ArrayList<>();
        visited.add(branchHead);
        queue.add(branchHead);

        while (!queue.isEmpty()) {
            String commit = queue.remove();
            result.add(commit);
            Commit currCommit = Utils.readObject(
                    Utils.join(Tree.COMMITS_DIR, commit), Commit.class);

            ArrayList<String> neighbors = new ArrayList
                    <>(currCommit.getParents().keySet());
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    queue.add(neighbor);
                    visited.add(neighbor);
                }
            }
        }
        return result;
    }

    /**
     * Name of branch.
     */
    private String _name;
    /**
     * SHA1 ID of this branch.
     */
    private String _ID;

    /**
     * Commit history of branch.
     */
    private LinkedHashMap<String, String> _commits;

    /**
     * SHA1 ID of head commit.
     */
    private String _head;
}
