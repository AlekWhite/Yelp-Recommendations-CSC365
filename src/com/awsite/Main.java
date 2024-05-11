package com.awsite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

public class Main {

    // LOAD: allBusinesses, allReviews (JSON) //
    // loads json file into objects
    public static <T> T getObjects(String file, Class<T> tClass){
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().setLenient().create();
        try {
            return gson.fromJson(new FileReader(file),  tClass);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;}}
    static Review[] allReviews = getObjects("yelp_review_subset_v2.json", Review[].class);
    static Business[] allBusinesses = getObjects("yelp_business_subset_v2.json", Business[].class);

    // LOAD: idf-table (byte buffer) //
    // first attempt to load from file, then construct the IDF table using all reviews in file
    public static HFT getIDFTable() throws IOException {

        // read idfTable file with byte buffer
        if (new File("idfTable.src").isFile()){
            RandomAccessFile fileI = new RandomAccessFile("idfTable.src", "r");
            FileChannel fileChannelI = fileI.getChannel();
            ByteBuffer bbIn = ByteBuffer.allocate(1024*18000);
            fileChannelI.read(bbIn);
            bbIn.flip();
            HFT table = new HFT(-1);
            table.readTable(bbIn);
            fileChannelI.close();
            fileI.close();
            return table;}

        System.out.println("Started Building IDF-Table");
        int totalDocumentLength = 0;
        for (Review r: allReviews){
            if (r==null) continue;
            String[] words = r.getText().split("\\s+");
            totalDocumentLength += words.length;}

        HFT table = new HFT(totalDocumentLength);
        for (Review r: allReviews){
            if (r==null) continue;
            String[] words = r.getText().split("\\s+");
            for (String word: words){
                table.add(word, -1);}}

        System.out.println("Done Building IDF-Table");
        return table;}
    static HFT idfTable;
    static { try { idfTable = getIDFTable();}
    catch (IOException e) {e.printStackTrace();}}

    // LOAD: similarity metrics (Serialization and byte-buffers) //
    // load persistent hash tree, containing similarity metrics
    static PHT tree;
    static { try {tree = new PHT(true);}
    catch (IOException | ClassNotFoundException e) {e.printStackTrace();}}

    // LOAD: clusters (Serialization) //
    // load word-similarity based cluster from file
    static Cluster c;
    static { try { c = new Cluster(8, true, "clusters.src");}
    catch (IOException | ClassNotFoundException e) {e.printStackTrace();}}

    // LOAD: graph (Serialization) //
    static BusinessGraph graph;
    static {try {graph = new BusinessGraph(true);}
    catch (IOException | ClassNotFoundException e) {e.printStackTrace();}}

    // returns recommended businesses
    public static Business[] getRecommendations(Business business){
        ArrayList<Double> simVec = new ArrayList<>(business.getSimilarityVector());
        ArrayList<Double> oldSimVec = new ArrayList<>(simVec);
        Collections.sort(simVec);

        Business l = c.getNearestCluster(business);
        //Business l = business;
        System.out.println(l);
        return new Business[]{
                allBusinesses[oldSimVec.indexOf(simVec.get(simVec.size()-1))],
                allBusinesses[oldSimVec.indexOf(simVec.get(simVec.size()-2))],
                l};}

    // create GUI after loading all objects and tables
    static Gui gui = new Gui();

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // build a new xml file on startup
        graph.buildPathXML();
        System.out.println("Done with all tasks");
        gui.repaint();}
}


