package gitlet;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.TreeMap;

public class Commit implements Serializable {
    public Commit(String message, String timeStamp) {
        this._message = message;
        this._timeStamp = timeStamp;
        this._ID = Utils.sha1(Utils.serialize(this));
        this.files = new TreeMap<String, String>();
        this._parents = new LinkedHashMap<>();
    }

    /**
     * Add another parent.
     *
     * @param commitID SHA1 ID of new parent commit.
     */
    public void addParent(String commitID) {
        this._parents.put(commitID, "");
    }

    /**
     * Get parents of commit.
     *
     * @return Parents of commit.
     */
    public LinkedHashMap<String, String> getParents() {
        return _parents;
    }

    /**
     * Get commit message.
     *
     * @return Commit message.
     */
    public String getMessage() {
        return _message;
    }

    /**
     * Get time stamp of commit.
     *
     * @return Time stamp of commit.
     */
    public String getTimeStamp() {
        return _timeStamp;
    }

    /**
     * Set files of commit.
     * @param newFiles New commit files.
     */
    public void setFiles(TreeMap<String, String> newFiles) {
        this.files = newFiles;
        this._ID = Utils.sha1(Utils.serialize(this));
    }

    /**
     * Get SHA1 ID of commit.
     *
     * @return SHA1 ID of commit.
     */
    public String getID() {
        return _ID;
    }

    /**
     * Get files of commit.
     *
     * @return TreeMap of file names and SHA1 IDs
     */
    public TreeMap<String, String> getFiles() {
        return files;
    }

    /**
     * Returns String representation of commit.
     *
     * @return String representation of commit.
     */
    public String toString() {
        String result = "===" + "\n" + "commit " + _ID + "\n";
        result += "Date: " + _timeStamp + "\n";
        return result + _message + "\n";
    }

    /**
     * Message of commit.
     */
    private String _message;

    /**
     * Time stamp of commit.
     */
    private String _timeStamp;

    /**
     * SHA1 ID of commit.
     */
    private String _ID;

    /**
     * History of file names to file SHA1 IDs in the commit.
     */
    private TreeMap<String, String> files;

    /**
     * Parents of commit.
     */
    private LinkedHashMap<String, String> _parents;
}
