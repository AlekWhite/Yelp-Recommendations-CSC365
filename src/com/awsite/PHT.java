package com.awsite;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.awsite.Main.allBusinesses;


class IndexArray implements Serializable {
    long[] index;
    int size;

    public IndexArray(Bucket[] table){
        this.size = table.length;
        long[] index = new long[table.length];

        // looks for duplicate buckets in table, replaces it with a reference to an existing objects
        for(int k=0; k<table.length; k++){
            boolean firstInstance = true;
            int firstInd = 999999999;

            // compare against all others buckets
            for(int i=0; i<table.length; i++){
                if ((table[i] == null) || (table[k] == null)) continue;
                for(int j=0; j<Bucket.MAX_COUNT; j++){

                    // compare keys list of the buckets
                    if (((table[i] != null) && (table[k] != null))
                            && ((table[i].keys[j] != null) && (table[k].keys[j] != null))){
                        if (!table[i].keys[j].equals(table[k].keys[j])){continue;}
                        if(i < k) {
                            firstInstance = false;
                            firstInd = Integer.min(firstInd, i);}}}}

            if(firstInstance) index[k] = k;
            else index[k] = firstInd;}

        this.index = index;

        System.out.print("[ ");
        for (long l: index){
            System.out.print(l + ", ");
        } System.out.println("]");}

    long getBucketPosition(String key) {
        return index[(key.hashCode() & (size - 1))];
    }

    @Serial
    private void readObject(ObjectInputStream  s) throws IOException {
        size = s.readInt();
        index = new long[size];
        for (int i=0; i<size; i++){
            index[i] = s.readInt();}}

    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeInt(size);
        for (long l: index){
            s.writeInt((int) l);}}

}

class PHT {
    private int size = 0;
    Bucket[] table;
    IndexArray indexArray;

    PHT(boolean created) throws IOException, ClassNotFoundException {
        if (created){
            indexArray = (IndexArray)
                    new ObjectInputStream(new FileInputStream("INDEX")).readObject();
            table = new Bucket[indexArray.size];}
        else loadPHT();}

    // produces table with data from allBusinesses
    void loadPHT() throws IOException {
        table = new Bucket[2];
        size = 2;
        for (Business business: allBusinesses){
            add(business.getBusiness_id(), 1);}
        System.out.println("\n");
        System.out.println(this);
        indexArray = new IndexArray(table);

        // create object files for unique buckets
        Set<Integer> binNumbers = new HashSet<>();
        for (long val : indexArray.index){
            binNumbers.add((int)val);}
        for (int val: binNumbers){
            if (table[val] == null) continue;

            // fill byteBuffer with bucket object, then write bb to disk
            ByteBuffer bbOut = ByteBuffer.allocate(1024*4400);
            table[val].writeBucket(bbOut);
            RandomAccessFile fileO = new RandomAccessFile("pHash/bucket_" + val + ".src", "rw");
            FileChannel fileChannelO = fileO.getChannel();
            bbOut.flip();
            fileChannelO.write(bbOut);
            fileChannelO.close();
            fileO.close();}

        // put index file on disk
        FileOutputStream fos2 = new FileOutputStream("INDEX");
        ObjectOutputStream oos2 = new ObjectOutputStream(fos2);
        oos2.writeObject(indexArray);
        oos2.close();

    }

    // adds a new business id to the tree
    void add(String key, int depth){

        // get index from key
        String bin = Integer.toBinaryString(key.hashCode());
        int indexLength = (int)(Math.log(size) / Math.log(2));
        int index = Integer.parseInt(bin.substring(bin.length()-indexLength), 2);

        // get bucket at index
        Bucket bucket = table[index];
        if (bucket == null){
            bucket = new Bucket(depth);
            table[index] = bucket;}

        // if bucket is full, get old keys and empty overflowing bucket
        if (bucket.count >= Bucket.MAX_COUNT){
            int newPos = (int)bucket.pos + 1;
            String[] oldKeys = bucket.keys;
            ArrayList<Integer> linkedIndexes = new ArrayList<>();

            // empty bucket, if multiple indexes map to this bucket, empty those buckets too and re-add keys
            for(int i=0; i<table.length; i++){
                for(int j=0; j<table[i].count; j++){
                    if ((table[i].keys[j] != null) && (bucket.keys[j] != null)){
                        if (!table[i].keys[j].equals(bucket.keys[j])){continue;}
                        table[i] = new Bucket(newPos);
                        linkedIndexes.add(i);}}}

            // expand table if needed
            if(linkedIndexes.size() <= 1){
                Bucket[] oldTable = table;
                int oldCapacity = oldTable.length;
                int newCapacity = oldCapacity << 1;
                Bucket[] newTable = new Bucket[newCapacity];

                // copy oldTable to newTable, and add duplicate references
                for(int i=0; i<oldCapacity; i++){
                    newTable[i] = oldTable[i];
                    newTable[i+oldCapacity] = oldTable[i];}
                table = newTable;
                size = newCapacity;}

            // add old keys into the new table
            add(key,depth+1);
            for (String k: oldKeys){
                add(k,depth+1);}

        } else {
            bucket.add(key);}}

    // returns the values associated with a given key
    ArrayList<Double> get(String key) throws IOException, ClassNotFoundException {

        // get filename (pos) from indexArray
        long pos = indexArray.getBucketPosition(key);
        if (table[(int) pos] != null) return table[(int) pos].get(key);

        // read file with byte buffer
        System.out.println("Reading File: " + "pHash//bucket_" + pos + ".src");
        RandomAccessFile fileI = new RandomAccessFile("pHash//bucket_" + pos + ".src", "r");
        FileChannel fileChannelI = fileI.getChannel();
        ByteBuffer bbIn = ByteBuffer.allocate(1024*4400);
        fileChannelI.read(bbIn);
        bbIn.flip();

        // read bucket from file
        Bucket bin = new Bucket(-1);
        bin.readBucket(bbIn);
        fileChannelI.close();
        fileI.close();
        table[(int) pos] = bin;
        return bin.get(key);}

    public String toString(){
        String out = "[";
        for (int i=0; i<table.length; i++){
            if (table[i] != null){
                out += "ind{" + i + "} " + table[i].toString() + ", \n";
            } else {
                out += "ind{" + i + "} " + "null, \n";}}
        return out + "]";}

}