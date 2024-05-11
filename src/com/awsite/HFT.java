/*
Modified from HT example code
 */

package com.awsite;

import java.nio.ByteBuffer;

class HFT implements java.io.Serializable {

    static final int KEY_SIZE = 12;

    static final class Node {
        Object key;
        Node next;
        float frequency;
        Node(Object k, Node n) { key = k; next = n; }}

    Node[] table = new Node[8]; // always a power of 2
    int size = 0;
    private final int documentLength;

    // require document length to be given when creating table
    public HFT(int documentLength){
        this.documentLength = documentLength;}

    // return all keys in the table
    public Object[] getAllKeys(){
        Object[] out = new Object[table.length];
        for (int i = 0; i < table.length; ++i)
            for (Node e = table[i]; e != null; e = e.next)
                out[i] = e.key;
        return out;}

    // return the frequency value of a given key
    public double getFrequency(Object key){
        Node n = getNode(key);
        if (n != null){
            return n.frequency;
        } else return -1;}

    // public contains method
    public boolean contains(Object key){
        return (getNode(key) != null);}

    void add(Object key, float val) {

        // update frequency if the key is already present
        Node e = getNode(key);
        if ((e != null) && (val == -1)) e.frequency = ((e.frequency*documentLength)+1)/documentLength;

        // add a new node with if needed
        else {
            int h = key.hashCode();
            int i = h & (table.length - 1);
            table[i] = new Node(key, table[i]);
            if (val == -1) table[i].frequency = (float)(1.0 / (float)documentLength);
            else table[i].frequency = val;
            ++size;
            if ((float)size/table.length >= 0.75f)
                resize();}
    }

    // gets the node with a given key
    private Node getNode(Object key) {
        if (key == null){
            return null;}
        int h = key.hashCode();
        int i = h & (table.length - 1);
        for (Node e = table[i]; e != null; e = e.next) {
            if (key.equals(e.key))
                return e;}
        return null;}

    // resize
    void resize() { // avoids unnecessary creation
        Node[] oldTable = table;
        int oldCapacity = oldTable.length;
        int newCapacity = oldCapacity << 1;
        Node[] newTable = new Node[newCapacity];
        for (int i = 0; i < oldCapacity; ++i) {
            Node e = oldTable[i];
            while (e != null) {
                Node next = e.next;
                int h = e.key.hashCode();
                int j = h & (newTable.length - 1);
                e.next = newTable[j];
                newTable[j] = e;
                e = next;
            }
        }
        table = newTable;
    }

    // puts all table data into a byteBuffer
    public void writeTable(ByteBuffer out){
        out.putInt(size);
        for (HFT.Node node : table) {
            for (HFT.Node e = node; e != null; e = e.next) {
                for (int i=0; i< HFT.KEY_SIZE; i++){
                    if (i < e.key.toString().length()) out.putChar(e.key.toString().charAt(i));
                    else out.putChar(' ');}
                out.putFloat(e.frequency);}}}

    // read byteBuffer into HFT an object
    public void readTable(ByteBuffer in){
        int n = in.getInt();
        size = n;
        table = new HFT.Node[8];
        for (int i = 0; i < n; ++i){
            String k = "";
            for (int j=0; j<HFT.KEY_SIZE; j++){
                k += in.getChar();}
            float f = in.getFloat();
            add(k, f);}}

    public String toString(){
        String out = "[ ";
        for (Node node : table)
            for (Node e = node; e != null; e = e.next)
                out += e.key + ":" + e.frequency + ", ";
        return out + "]";}
}
