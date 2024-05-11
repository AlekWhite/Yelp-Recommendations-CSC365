package com.awsite;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static com.awsite.Main.allBusinesses;

class Bucket implements Serializable {

    static final int MAX_COUNT = 63;
    static final int KEY_LENGTH = 22;
    static final int TABLE_LENGTH = 10000;
    int pos;
    int count;
    String[] keys = new String[MAX_COUNT];

    ArrayList<ArrayList<Double>> vals = new ArrayList<>();

    public Bucket(int pos){this.pos = pos;}

    // find tables associated a with given key
    ArrayList<Double> get(String key) {
        for (int j = 0; j < count; ++j) {
            if (key.equals(keys[j]))
                return vals.get(j);}
        return null;}

    // adds key to array
    void add(String key){
        if (!contains(key)){
            keys[count] = key;
            count ++;}}

    boolean contains(String key){
        for (int j = 0; j < count; ++j) {
            if (key.equals(keys[j]))
                return true;}
        return false;}

    public void writeBucket(ByteBuffer out){
        out.putInt(count);
        for (int i = 0; i < MAX_COUNT; ++i) {


            // get business object from id
            if(keys[i] == null) continue;
            int j;
            for (j=0; true; j++){
                if (allBusinesses[j].getBusiness_id().equals(keys[i])) break;}
            Business business = allBusinesses[j];

            System.out.println(i + " " + business);


            byte[] keyBytes = keys[i].getBytes(StandardCharsets.UTF_8);
            out.put(keyBytes);

            ArrayList<Double> simVec = business.getSimilarityVector();
            System.out.println(simVec);

            for (Double d: simVec){
                out.putFloat(d.floatValue());}}}

    public void readBucket(ByteBuffer in){
        count = in.getInt();
        for (int i = 0; i < count; ++i) {

            // load key strings
            byte[] key = new byte[KEY_LENGTH];
            in.get(key);
            String keyStr = new String(key, StandardCharsets.UTF_8);
            keys[i] = keyStr.trim();

            // load tables into vals array
            ArrayList<Double> simVec = new ArrayList<>();
            for(int j=0; j<TABLE_LENGTH; j++){
                simVec.add((double)in.getFloat());}
            vals.add(simVec);}}

    public String toString(){
        String out = "[(" + pos + ")";
        for (String key: keys){
            out += key + ", ";}
        return out + "]";}
}