package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

public class Blob implements Serializable {
    public Blob(String name) {
        this._name = name;
        File f = new File(CWD, name);
        if (f.exists()) {
            this._contents = Utils.readContents(f);
            this._hash = Utils.sha1(this._contents);
        }
    }

    /**
     * Get SHA1 ID of blob.
     * @return SHA1 ID.
     */
    public String getHash() {
        return this._hash;
    }


    /**
     * Get contents of blob.
     * @return Contents as bytes.
     */
    public byte[] getContents() {
        return _contents;
    }

    /**
     * Get file name of blob.
     * @return File name.
     */
    public String getName() {
        return _name;
    }

    /**
     * Determine if two blobs have the same SHA1 ID.
     * @param otherBlob Blob being compared to.
     * @return boolean if blobs are identical.
     */
    public boolean equals(Blob otherBlob) {
        return Objects.equals(otherBlob._hash, this._hash);
    }

    /**
     * File name of blob to serialize.
     */
    private String _name;

    /**
     * Contents of blob stored as bytes.
     */
    private byte[] _contents;

    /**
     * SHA1 ID of blob for serialization.
     */
    private String _hash;

    /**
     * Common Working Directory of project.
     */
    static final File CWD = Tree.CWD;

}
