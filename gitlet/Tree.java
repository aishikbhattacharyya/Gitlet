package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Repository class that runs all Git commands.
 * @author Aishik Bhattacharyya
 */
public class Tree {
    /**
     * Head commit of repository.
     */
    private Commit _head;

    /**
     * Current branch of repository.
     */
    private Branch _currentBranch;

    /**
     * Staging area for addition.
     */

    private TreeMap<String, String> _stagingAreaAdd;

    /**
     * Staging are for removal.
     */
    private TreeMap<String, String> _stagingAreaRemove;

    /**
     * Common Working Directory file.
     */
    static final File CWD = new File(System.getProperty("user.dir"));

    /**
     * Branches storage file.
     */
    static final File BRANCHES_DIR = new File(".gitlet/branches");

    /**
     * Commits storage file.
     */
    static final File COMMITS_DIR = new File(".gitlet/commits");

    /**
     * Blobs storage file.
     */
    static final File BLOB_DIR = new File(".gitlet/blobs");

    /**
     * Staging area for addition storage file.
     */
    static final File STAGING_ADD = new File(".gitlet/staging/add");

    /**
     * Staging area for removal storage file.
     */
    static final File STAGING_REMOVE = new File(".gitlet/staging/remove");

    /**
     * Maximum possible commit ID length.
     */
    static final int MAX_COMMIT_ID_LENGTH = 40;

    @SuppressWarnings("unchecked")

    public Tree() {
        File currentBranchFile = Utils.join
                (BRANCHES_DIR, "current");
        if (currentBranchFile.exists()) {
            Branch currentBranch = Utils.readObject
                    (currentBranchFile, Branch.class);
            String lastCommitSha1 = currentBranch.getHeadCommit();
            _head = Utils.readObject(Utils.join
                    (COMMITS_DIR, lastCommitSha1), Commit.class);
            _currentBranch = currentBranch;

            try {
                _stagingAreaAdd = Utils.readObject
                        (STAGING_ADD, TreeMap.class);
            } catch (Exception e) {
                _stagingAreaAdd = new TreeMap<>();
            }

            try {
                _stagingAreaRemove = Utils.readObject
                        (STAGING_REMOVE, TreeMap.class);
            } catch (Exception e) {
                _stagingAreaRemove = new TreeMap<>();
            }
        }
    }

    public void init() throws Exception {
        File rootDir = new File(".gitlet");
        if (!rootDir.exists()) {
            new File(".gitlet").mkdir();

            File blobDir = BLOB_DIR;
            blobDir.mkdir();

            File commitDir = COMMITS_DIR;
            commitDir.mkdir();

            File branchDir = BRANCHES_DIR;
            branchDir.mkdir();
            File currBranch = Utils.join(BRANCHES_DIR, "current");
            currBranch.createNewFile();

            File stagingDir = new File(".gitlet", "staging");
            stagingDir.mkdir();
            File add = STAGING_ADD;
            add.createNewFile();
            File remove = STAGING_ADD;
            remove.createNewFile();

            String initMsg = "initial commit";
            String initDate = "Thu Jan 1 00:00:00 1970 -0800";
            Commit initCommit = new Commit(initMsg, initDate);
            serializeCommit(initCommit);
            _head = initCommit;

            Branch master = new Branch("master");
            master.addCommit(initCommit);
            Utils.writeObject(new File(".gitlet/branches/current"), master);
        } else {
            System.out.println("Gitlet version-control system"
                    + " already exists in the current directory.");
        }
    }

    public void add(String fileName) throws IOException {
        File file = new File(CWD, fileName);
        if (file.exists()) {
            TreeMap<String, String> stagingAreaAdd;

            Blob blob = new Blob(fileName);
            File newBlobFile = Utils.join(BLOB_DIR, blob.getHash());

            if (_head.getFiles().containsKey(fileName)) {
                Blob headBlob = Utils.readObject
                        (Utils.join(BLOB_DIR,
                                        _head.getFiles().get(fileName)),
                                Blob.class);
                byte[] currContents = Utils.readContents
                        (new File(CWD, fileName));
                if (!Arrays.equals(headBlob.getContents(), currContents)) {
                    _stagingAreaAdd.put(fileName, blob.getHash());
                    newBlobFile.createNewFile();
                    Utils.writeObject(newBlobFile, blob);
                } else if (_stagingAreaRemove.containsKey(fileName)) {
                    _stagingAreaRemove.remove(fileName);
                } else if (_stagingAreaAdd.containsKey(fileName)) {
                    _stagingAreaAdd.remove(fileName);
                }
            } else {
                _stagingAreaAdd.put(fileName, blob.getHash());
                Utils.writeObject(newBlobFile, blob);
            }
            Utils.writeObject(STAGING_ADD, _stagingAreaAdd);
            Utils.writeObject(STAGING_REMOVE, _stagingAreaRemove);
        } else if (_stagingAreaRemove.containsKey(fileName)) {
            Blob removedBlob = Utils.readObject
                    (Utils.join(BLOB_DIR, _stagingAreaRemove.get(fileName)),
                            Blob.class);
            Utils.writeContents(new File(CWD, fileName),
                    removedBlob.getContents());
            _stagingAreaRemove.remove(fileName);
            Utils.writeObject(new File(".gitlet/staging/remove"),
                    _stagingAreaRemove);
        } else {
            System.out.println("File does not exist.");
        }
    }

    @SuppressWarnings("unchecked")
    public void commit(String commitMsg) throws IOException {
        if (commitMsg.length() == 0) {
            System.out.println("Please enter a commit message.");
            return;
        }

        if (_stagingAreaAdd.isEmpty() && _stagingAreaRemove.isEmpty()) {
            System.out.println("No changes added to the commit");
            return;
        }
        TreeMap<String, String> filesCopy = (TreeMap<String, String>)
                _head.getFiles().clone();
        if (_stagingAreaAdd != null && !_stagingAreaAdd.isEmpty()) {
            for (String toAdd : _stagingAreaAdd.keySet()) {
                filesCopy.put(toAdd, _stagingAreaAdd.get(toAdd));
            }
        }
        if (_stagingAreaRemove != null && !_stagingAreaRemove.isEmpty()) {
            for (String toRemove : _stagingAreaRemove.keySet()) {
                filesCopy.remove(toRemove);
            }
        }

        DateFormat df = new SimpleDateFormat("EE MMM dd HH:mm:ss yyyy Z");
        df.setTimeZone(TimeZone.getTimeZone("America/Anchorage"));
        String dStr = df.format(new Date());
        Commit newCommit = new Commit(commitMsg, dStr);
        newCommit.addParent(_head.getID());
        newCommit.setFiles(filesCopy);
        serializeCommit(newCommit);
        _head = newCommit;

        File currBranchFile = new File(".gitlet/branches/current");
        _currentBranch.addCommit(newCommit);
        Utils.writeObject(currBranchFile, _currentBranch);

        _stagingAreaAdd.clear();
        _stagingAreaRemove.clear();

        Utils.writeObject(STAGING_ADD, _stagingAreaAdd);
        Utils.writeObject(STAGING_REMOVE, _stagingAreaRemove);
    }

    public void log() {
        LinkedHashMap<String, String> commits = _currentBranch.getCommits();
        List<String> allKeys = new ArrayList<>(commits.keySet());
        Collections.reverse(allKeys);
        boolean foundHead = false;
        for (String sha1 : allKeys) {
            Commit currCommit = Utils.readObject(Utils.join(COMMITS_DIR, sha1),
                    Commit.class);
            if (_currentBranch.getHeadCommit().equals(sha1)) {
                foundHead = true;
            }
            if (foundHead) {
                System.out.println(currCommit.toString());
            }
        }
    }

    public void globalLog() {
        Set<String> commitIDs = new HashSet<>();

        for (File commitFile : COMMITS_DIR.listFiles()) {
            Commit currCommit = Utils.readObject(commitFile, Commit.class);
            if (!commitIDs.contains(currCommit.getID())) {
                commitIDs.add(currCommit.getID());
                System.out.println(currCommit);
            }
        }
    }

    public void checkout(String str) {
        if (str.contains("++")) {
            System.out.println("Incorrect operands.");
            return;
        }
        if (!str.contains("--")) {
            checkoutBranch(str);
        } else if (str.indexOf('-') == 0) {
            String fileName = str.substring(3);
            TreeMap<String, String> headFiles = _head.getFiles();
            File f = new File(CWD, fileName);
            try {
                String blobID = headFiles.get(fileName);
                File prevFile = new File(".gitlet/blobs", blobID);
                Blob fileBlob = Utils.readObject(prevFile, Blob.class);
                Utils.writeContents(f, fileBlob.getContents());
            } catch (Exception e) {
                System.out.println("File does not exist in that commit.");
                return;
            }
        } else {
            String commitID = str.substring(0, str.indexOf("-") - 1);
            commitID = findFullID(commitID);
            String fileName = str.substring(str.indexOf("-") + 3);
            if (!_currentBranch.getCommits().containsKey(commitID)) {
                System.out.println("No commit with that id exists.");
                return;
            }
            File commitFile = Utils.join(COMMITS_DIR, commitID);
            Commit c = Utils.readObject(commitFile, Commit.class);
            if (!c.getFiles().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                return;
            }
            String blobID = c.getFiles().get(fileName);
            File blobFile = Utils.join(BLOB_DIR, blobID);
            Blob blob = Utils.readObject(blobFile, Blob.class);
            Utils.writeContents(Utils.join(CWD, fileName),
                    blob.getContents());
        }
    }

    private void checkoutBranch(String branchName) {
        boolean found = false;
        String branchSha1 = "";
        for (File file : CWD.listFiles()) {
            String fileName = file.getName();
            if (!_head.getFiles().containsKey(fileName)
                    && fileName.charAt(0) != '.' && file.isFile()) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }
        for (File currFile : BRANCHES_DIR.listFiles()) {
            Branch tempBranch = Utils.readObject(currFile, Branch.class);
            if (tempBranch.getName().equals(branchName)) {
                found = true;
                branchSha1 = currFile.getName();
                break;
            }
        }
        if (!found) {
            System.out.println("No such branch exists.");
            return;
        }
        if (_currentBranch.getName().equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        File newBranchFile = new File(".gitlet/branches", branchSha1);
        Branch newBranch = Utils.readObject(newBranchFile, Branch.class);
        String newHeadStr = newBranch.getHeadCommit();
        Commit newHead = Utils.readObject(new File(".gitlet/commits",
                newHeadStr), Commit.class);
        for (String fileName : newHead.getFiles().keySet()) {
            String sha1 = newHead.getFiles().get(fileName);
            Blob blob = Utils.readObject(new File(".gitlet/blobs",
                    sha1), Blob.class);
            Utils.writeContents(new File(CWD, fileName),
                    blob.getContents());
        }
        for (String fileName : _head.getFiles().keySet()) {
            if (!newHead.getFiles().containsKey(fileName)) {
                File toDel = new File(CWD, fileName);
                if (toDel.exists()) {
                    toDel.delete();
                }
            }
        }
        Utils.writeObject(new File(".gitlet/branches",
                _currentBranch.getID()), _currentBranch);
        Utils.writeObject(new File(".gitlet/branches/current"), newBranch);
        File curr = new File(".gitlet/branches", newBranch.getID());
        if (curr.exists()) {
            curr.delete();
        }
        _currentBranch = newBranch;
        _head = newHead;
        clearStagingArea();
    }

    private void clearStagingArea() {
        _stagingAreaAdd.clear();
        _stagingAreaRemove.clear();

        Utils.writeObject(new File(".gitlet/staging/add"),
                _stagingAreaAdd);
        Utils.writeObject(new File(".gitlet/staging/remove"),
                _stagingAreaRemove);
    }

    private String findFullID(String commitID) {
        if (commitID.length() == MAX_COMMIT_ID_LENGTH) {
            return commitID;
        }
        int length = commitID.length();
        for (File commitFile : COMMITS_DIR.listFiles()) {
            String subName = commitFile.getName().substring(0, length);
            if (subName.equals(commitID)) {
                return commitFile.getName();
            }
        }
        return "";
    }

    public void removeFile(String fileName) {
        if (_stagingAreaAdd.containsKey(fileName)) {
            _stagingAreaAdd.remove(fileName);
        } else if (_head.getFiles().containsKey(fileName)) {
            _stagingAreaRemove.put(fileName, _head.getFiles().get(fileName));
            Utils.join(CWD, fileName).delete();
        } else {
            System.out.println("No reason to remove the file.");
            return;
        }

        Utils.writeObject(STAGING_ADD, _stagingAreaAdd);
        Utils.writeObject(STAGING_REMOVE, _stagingAreaRemove);
    }

    public void removeBranch(String branchName) {
        File[] allBranchFiles = BRANCHES_DIR.listFiles();

        for (File currBranchFile : allBranchFiles) {
            Branch currBranch = Utils.readObject
                    (currBranchFile, Branch.class);
            if (currBranch.getName().equals(branchName)
                    && currBranchFile.getName().equals("current")) {
                System.out.println("Cannot remove the current branch.");
                return;
            }
        }
        for (File currBranchFile : allBranchFiles) {
            Branch currBranch = Utils.readObject(currBranchFile, Branch.class);
            if (currBranch.getName().equals(branchName)) {
                currBranchFile.delete();
                return;
            }
        }
        System.out.println("A branch with that name does not exist.");
    }

    public void reset(String commitID) throws IOException {
        commitID = findFullID(commitID);
        if (!Arrays.asList(COMMITS_DIR.list()).contains(commitID)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        File commitFile = Utils.join(COMMITS_DIR, commitID);
        Commit commit = Utils.readObject(commitFile, Commit.class);

        for (File file : CWD.listFiles()) {
            String fileName = file.getName();
            if (!_head.getFiles().containsKey(fileName)
                    && !_stagingAreaAdd.containsKey(fileName)
                    && fileName.charAt(0) != '.' && file.isFile()) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                return;
            }
        }

        ArrayList<String> toDelete = new ArrayList<>();
        for (File file : CWD.listFiles()) {
            String fileName = file.getName();
            if (fileName.charAt(0) != '.' && file.isFile()) {
                if (!commit.getFiles().containsKey(fileName)) {
                    toDelete.add(file.getName());
                } else {
                    File currBlobFile = Utils.join
                            (BLOB_DIR, commit.getFiles().get(fileName));
                    Blob currBlob = Utils.readObject
                            (currBlobFile, Blob.class);
                    Utils.writeContents(file, currBlob.getContents());
                }
            }
        }

        for (String delFileName : toDelete) {
            File delFile = new File(CWD, delFileName);
            delFile.delete();
        }

        for (String fileName : commit.getFiles().keySet()) {
            if (!new File(CWD, fileName).exists()) {
                File newFile = new File(CWD, fileName);
                newFile.createNewFile();
                Blob blob = Utils.readObject(Utils.join(BLOB_DIR,
                        commit.getFiles().get(fileName)), Blob.class);
                Utils.writeContents(newFile, blob.getContents());
            }
        }
        _head = commit;
        _currentBranch.setHead(_head.getID());

        Utils.writeObject(Utils.join(BRANCHES_DIR, "current"), _currentBranch);

        _stagingAreaAdd.clear();
        _stagingAreaRemove.clear();
        Utils.writeObject(STAGING_ADD, _stagingAreaAdd);
        Utils.writeObject(STAGING_REMOVE, _stagingAreaRemove);
    }

    @SuppressWarnings("unchecked")
    public void branch(String branchName) throws IOException {
        for (File file : BRANCHES_DIR.listFiles()) {
            Branch currBranch = Utils.readObject(file, Branch.class);
            if (currBranch.getName().equals(branchName)) {
                System.out.println("A branch with that name already exists.");
                return;
            }
        }
        Branch newBranch = new Branch(branchName);
        newBranch.setCommits((LinkedHashMap<String, String>)
                _currentBranch.getCommits().clone());
        newBranch.setHead(_head.getID());

        File newBranchFile = Utils.join(BRANCHES_DIR, newBranch.getID());
        newBranchFile.createNewFile();

        Utils.writeObject(newBranchFile, newBranch);
    }

    public void find(String commitMsg) {
        boolean found = false;
        for (File commitFile : COMMITS_DIR.listFiles()) {
            Commit currCommit = Utils.readObject(commitFile, Commit.class);
            if (currCommit.getMessage().equals(commitMsg)) {
                System.out.println(currCommit.getID());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        if (!new File(".gitlet").exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        branchStatus();

        System.out.println();
        stagedFileStatus();

        System.out.println();
        removedFileStatus();

        System.out.println();
        modifiedAndUntrackedStatus();
    }

    private void modifiedAndUntrackedStatus() {
        ArrayList<String> modifiedNames = new ArrayList<>();
        ArrayList<String> modRemovedNames = new ArrayList<>();
        ArrayList<String> untrackedNames = new ArrayList<>();

        for (String fileName : _head.getFiles().keySet()) {
            File cwdFile = new File(CWD, fileName);
            if (!cwdFile.exists()) {
                if (!_stagingAreaRemove.containsKey(fileName)) {
                    modRemovedNames.add(fileName);
                }
            } else {
                byte[] contents = Utils.readContents(cwdFile);
                String blobID = _head.getFiles().get(fileName);
                Blob blob = Utils.readObject(Utils.join(BLOB_DIR, blobID),
                        Blob.class);
                if (!Arrays.equals(contents, blob.getContents())) {
                    if (_stagingAreaAdd.containsKey(fileName)) {
                        if (!Arrays.equals(blob.getContents(), contents)) {
                            modifiedNames.add(fileName);
                        }
                    } else {
                        modifiedNames.add(fileName);
                    }
                }
            }
        }

        for (String stagingAdd : _stagingAreaAdd.keySet()) {
            File file = new File(CWD, stagingAdd);
            if (!file.exists()) {
                modRemovedNames.add(stagingAdd);
            }
        }
        Collections.sort(modifiedNames);
        Collections.sort(modRemovedNames);
        Collections.sort(untrackedNames);

        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String modifiedName : modifiedNames) {
            System.out.println(modifiedName + " (modified)");
        }
        for (String modRemoveName : modRemovedNames) {
            System.out.println(modRemoveName + " (deleted)");
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String cwdFileName : CWD.list()) {
            if (!_stagingAreaAdd.containsKey(cwdFileName)
                    && !_head.getFiles().containsKey(cwdFileName)) {
                File currFile = new File(cwdFileName);
                if (!currFile.isDirectory() && cwdFileName.charAt(0) != '.') {
                    untrackedNames.add(cwdFileName);
                }
            }
        }
        for (String removedName : untrackedNames) {
            System.out.println(removedName);
        }
    }

    private void removedFileStatus() {
        System.out.println("=== Removed Files ===");
        ArrayList<String> removedNames = new ArrayList<>();
        for (String removeName : _stagingAreaRemove.keySet()) {
            removedNames.add(removeName);
        }
        Collections.sort(removedNames);
        for (String removedName : removedNames) {
            System.out.println(removedName);
        }
    }

    private void stagedFileStatus() {
        System.out.println("=== Staged Files ===");
        ArrayList<String> stagedNames = new ArrayList<>();
        for (String stagedName : _stagingAreaAdd.keySet()) {
            Blob blob = Utils.readObject
                    (Utils.join(BLOB_DIR, _stagingAreaAdd.get(stagedName)),
                            Blob.class);
            stagedNames.add(blob.getName());
        }
        Collections.sort(stagedNames);
        for (String stagedName : stagedNames) {
            System.out.println(stagedName);
        }
    }

    private void branchStatus() {
        System.out.println("=== Branches ===");
        ArrayList<String> branchNames = new ArrayList<>();
        for (File branchFile : BRANCHES_DIR.listFiles()) {
            Branch currBranch = Utils.readObject(branchFile, Branch.class);
            branchNames.add(currBranch.getName());
        }
        Collections.sort(branchNames);
        for (String branchName : branchNames) {
            if (branchName.equals(_currentBranch.getName())) {
                System.out.println("*" + branchName);
            } else {
                System.out.println(branchName);
            }
        }
    }

    public void merge(String otherBranchName) throws IOException {
        if (!_stagingAreaAdd.isEmpty() || !_stagingAreaRemove.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        Branch otherBranch = null;
        for (File branchFile: new File(".gitlet/branches").listFiles()) {
            Branch currBranch = Utils.readObject(branchFile, Branch.class);
            if (currBranch.getName().equals(otherBranchName)) {
                otherBranch = currBranch;
                break;
            }
        }
        if (otherBranch == null) {
            System.out.println("A branch with that name does not exist.");
            return;
        }

        if (_currentBranch.getName().equals(otherBranchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        for (File file: CWD.listFiles()) {
            String fileName = file.getName();
            if (!_head.getFiles().containsKey(fileName)
                    && fileName.charAt(0) != '.' && file.isFile()) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }

        String splitPoint = Branch.splitPoint(_currentBranch, otherBranch);
        if (splitPoint.equals(otherBranch.getHeadCommit())) {
            System.out.println("Given branch is an ancestor of "
                    + "the current branch.");
            return;
        }
        if (splitPoint.equals(_currentBranch.getHeadCommit())) {
            System.out.println("Current branch fast-forwarded.");
            checkout(otherBranchName);
            return;
        }
        Commit splitPointCommit = Utils.readObject(Utils.join(COMMITS_DIR,
                        splitPoint), Commit.class);

        Commit otherBranchHead = Utils.readObject(Utils.join(COMMITS_DIR,
                otherBranch.getHeadCommit()), Commit.class);

        boolean conflict = runMergeWithSplitPoint(splitPointCommit,
                otherBranchHead);
        mergeInfoUpdate(otherBranchHead, otherBranch, conflict);
    }

    private boolean runMergeWithSplitPoint(Commit splitPointCommit,
                                           Commit otherBranchHead)
            throws IOException {
        boolean conflict = false;
        for (String fileName : otherBranchHead.getFiles().keySet()) {
            File otherBranchFile = Utils.join(BLOB_DIR,
                    otherBranchHead.getFiles().get(fileName));
            Blob otherBranchBlob = Utils.readObject
                    (otherBranchFile, Blob.class);
            if (splitPointCommit.getFiles().keySet().contains(fileName)) {

                File splitPointFile = Utils.join(BLOB_DIR,
                        splitPointCommit.getFiles().get(fileName));
                Blob splitPointBlob = Utils.readObject
                        (splitPointFile, Blob.class);

                if (!Arrays.equals(otherBranchBlob.getContents(),
                        splitPointBlob.getContents())) {
                    if (_head.getFiles().containsKey(fileName)) {
                        if (_head.getFiles().get(fileName).equals
                                (splitPointCommit.getFiles().get(fileName))) {
                            Utils.writeContents(new File(CWD, fileName),
                                    otherBranchBlob.getContents());
                            _stagingAreaAdd.put(fileName,
                                    otherBranchHead.getFiles().get(fileName));
                        } else {
                            conflictUpdate(fileName, otherBranchBlob);
                            conflict = true;
                        }
                    }
                }
            } else {
                if (_head.getFiles().containsKey(fileName)) {
                    if (!_head.getFiles().get(fileName).equals
                            (otherBranchHead.getFiles().get(fileName))) {
                        conflictUpdate(fileName, otherBranchBlob);
                        conflict = true;
                    }
                } else {
                    Utils.writeContents(new File(CWD, fileName),
                            otherBranchBlob.getContents());
                    _stagingAreaAdd.put(fileName,
                            otherBranchHead.getFiles().get(fileName));
                }

            }
        }

        conflict = mergeHeadSplitFiles(splitPointCommit,
                otherBranchHead) || conflict;
        return conflict;
    }

    private boolean mergeHeadSplitFiles(Commit splitPointCommit,
                                        Commit otherBranchHead)
            throws IOException {
        boolean conflict = false;
        for (String fileName : _head.getFiles().keySet()) {
            if (splitPointCommit.getFiles().containsKey(fileName)) {
                if (!splitPointCommit.getFiles().get(fileName).equals
                        (_head.getFiles().get(fileName))) {
                    if (!otherBranchHead.getFiles().containsKey(fileName)) {
                        conflictUpdate(fileName, null);
                        conflict = true;
                    }
                }
            }
        }
        for (String fileName : splitPointCommit.getFiles().keySet()) {
            if (_head.getFiles().containsKey(fileName)) {
                if (_head.getFiles().get(fileName).equals
                        (splitPointCommit.getFiles().get(fileName))) {
                    if (!otherBranchHead.getFiles().containsKey
                            (fileName)) {
                        new File(CWD, fileName).delete();
                        _stagingAreaRemove.put(fileName,
                                splitPointCommit.getFiles().get
                                        (fileName));
                    }
                }
            }
        }
        return conflict;
    }

    private void mergeInfoUpdate(Commit otherBranchHead, Branch otherBranch,
                                 boolean conflict) throws IOException {
        commit("Merged " + otherBranch.getName() + " into "
                + _currentBranch.getName() + ".");
        _head.addParent(otherBranchHead.getID());
        Utils.writeObject(new File(".gitlet/commits",
                _head.getID()), _head);
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    private void conflictUpdate(String fileName, Blob otherBranchBlob)
            throws IOException {
        File currentBranchBlobFile = Utils.join(BLOB_DIR,
                _head.getFiles().get(fileName));
        Blob currentBranchBlob = Utils.readObject(currentBranchBlobFile,
                Blob.class);
        String newContent = "<<<<<<< HEAD" + "\n";
        newContent += new String(currentBranchBlob.getContents(),
                StandardCharsets.UTF_8);
        newContent += "=======" + "\n";
        if (otherBranchBlob != null) {
            newContent += new String(otherBranchBlob.getContents(),
                    StandardCharsets.UTF_8);
        }
        newContent += ">>>>>>>" + "\n";
        Utils.writeContents(new File(CWD, fileName), newContent);
        add(fileName);
    }

    private void serializeCommit(Commit initCommit) throws IOException {
        File commitFile = new File(".gitlet/commits",
                initCommit.getID());
        commitFile.createNewFile();
        Utils.writeObject(commitFile, initCommit);
    }
}
