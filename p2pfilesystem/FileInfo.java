package p2pfilesystem;

import java.io.Serializable;

public class FileInfo implements Serializable {

    public int peerid;
    public String fileName;
    public int chunkNumber;
    public int originalHash; //for integrity test

    @Override
    public boolean equals(Object otherObj) {
        FileInfo other = (FileInfo) otherObj;
        if (this.peerid == other.peerid && this.fileName.equals(other.fileName) && this.chunkNumber == other.chunkNumber) {
            return true;
        } else {
            return false;
        }
    }
}


