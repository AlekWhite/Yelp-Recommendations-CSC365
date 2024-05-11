package com.awsite;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Cluster implements Serializable{

    int k;
    double[] cost;
    double totalCost;
    Business[] medoids;
    ArrayList<Business>[] table;
    String fileName;

    /*                               *\
    #------{[ Cluster Creation ]}-----#
    \*                               */

    public Cluster(int k, boolean created, String fileName) throws IOException, ClassNotFoundException {
        this.fileName = fileName;

        // load from disk if possible
        if (created){
            Cluster c = (Cluster) new ObjectInputStream(new FileInputStream(fileName)).readObject();
            table = c.table;
            medoids = c.medoids;
            cost = c.cost;
            this.k = c.k;
            totalCost = c.totalCost;
            return;}

        // pick random medoids and do initial grouping
        if (k < 0) return;
        this.k = k;
        cost = new double[k];
        getRandomMedoids();
        generateClusters();
        System.out.println("Random Groupings: " + totalCost);
        improveClusters();

        // put cluster file on disk
        FileOutputStream fos2 = new FileOutputStream(fileName);
        ObjectOutputStream oos2 = new ObjectOutputStream(fos2);
        oos2.writeObject(this);
        oos2.close();
    }

    // generate new clusters with a smaller total cost
    private void improveClusters() throws IOException, ClassNotFoundException {
        double oldCost = totalCost;
        Business[] oldMedoids = medoids.clone();
        Cluster newCluster = this.copy();
        Business[] newMedoids;
        Random r = new Random();
        ArrayList<Integer> testedIndex = new ArrayList<>();

        // do 400-10000 randoms swaps before selecting the combination that decreases totalCost
        for(int i=0; i<Main.allBusinesses.length; i++){
            if ((i>400) && (totalCost != oldCost)) break;

            // get a random business that has not been tested yet
            int pos = r.nextInt(0, Main.allBusinesses.length);
            while (testedIndex.contains(pos)) pos = r.nextInt(0, Main.allBusinesses.length);
            testedIndex.add(pos);
            Business b = Main.allBusinesses[pos];

            // swap this business with each medoid
            for (int j=0; j<k; j++){
                newMedoids = oldMedoids.clone();
                if(newMedoids[j].getBusiness_id().equals(b.getBusiness_id())) continue;
                newMedoids[j] = b;
                medoids = newMedoids;
                generateClusters();

                // remember swap combination with the lowest cost
                if (totalCost < newCluster.totalCost){newCluster = this.copy();}}}

        // replace this object with the new combination
        table = newCluster.table;
        medoids = newCluster.medoids;
        cost = newCluster.cost;
        k = newCluster.k;
        totalCost = newCluster.totalCost;

        // repeat if improvements are possible
        System.out.println(totalCost);
        if (totalCost<oldCost) improveClusters();}

    // get k number of random businesses
    private void getRandomMedoids(){
        Random r = new Random();
        Business[] medoids = new Business[k];
        for (int i=0; i<medoids.length;){
            medoids[i] = Main.allBusinesses[r.nextInt(0, 10000)];
            if (medoids[i] != null) i++;}
        this.medoids = medoids;}

    // uses medoids to populate table
    private void generateClusters(){
        double[] cost = new double[k];
        ArrayList<Business>[] allClusters = new ArrayList[k];
        for (int i=0; i<k; i++){
            allClusters[i] = new ArrayList<>(); cost[i] = 0;}

        // get similarity between each medoid and each business
        double[][] simVal = new double[Main.allBusinesses.length][k];
        for (int i=0; i<k; i++){
            ArrayList<Double> simVec;
            simVec = medoids[i].getSimilarityVector();
            for(int j=0; j<Main.allBusinesses.length; j++){
                simVal[j][i] = simVec.get(j);}}

        // finds medoid with greater simVal for a given businesses
        for(int i=0; i<Main.allBusinesses.length; i++){
            double greatestVal = 0;
            int ind = 0;
            for (int j = 0; j<k; j++){
                if (simVal[i][j] > greatestVal){
                    greatestVal = simVal[i][j];
                    ind = j;}}
            allClusters[ind].add(Main.allBusinesses[i]);
            cost[ind] += 1-greatestVal;}

        // update cost
        double totalCost = 0;
        int costSize = 0;
        for(int i=0; i<k; i++){
            totalCost += cost[i];
            costSize += allClusters[i].size();
            cost[i] = cost[i]/allClusters[i].size();}
        totalCost = totalCost / costSize;
        this.totalCost = totalCost;
        this.cost = cost;
        table = allClusters;}

    private Cluster copy() throws IOException, ClassNotFoundException {
        Cluster out = new Cluster(-1, false, fileName);
        out.totalCost = totalCost;
        out.medoids = medoids;
        out.cost = cost;
        out.table = table;
        out.k = k;
        return out;}

    /*                               *\
    #-----{[ Output Generation ]}-----#
    \*                               */

    // returns the recommended cluster
    public Business getNearestCluster(Business business){
        Business clusterRec = null;
        for (int i=0; i<k; i++){
            for (Business b: table[i]){
                if (b.getBusiness_id().equals(business.getBusiness_id())){
                    clusterRec = getMostSimilarKey()[i];
                    break;}}}
        return clusterRec;
    }

    // finds the key with the greatest similarity val in its cluster
    public Business[] getMostSimilarKey(){
        Business[] out = new Business[k];
        double[] vals = new double[k];
        for (int i=0; i<k; i++){

            // get the average similarity between a business and all other businesses in its cluster
            double[] avgSimVal = new double[table[i].size()];
            for (int j=0; j<table[i].size(); j++){
                double avg = 0;
                ArrayList<Double> simVec;
                simVec = medoids[i].getSimilarityVector();
                for (Double d: simVec){avg += d;}
                avgSimVal[j] = avg/simVec.size();}

            // finds largest average
            ArrayList<Double> oldVals = new ArrayList<>();
            for (double d: avgSimVal) oldVals.add(d);
            Arrays.sort(avgSimVal);
            out[i] = table[i].get(oldVals.indexOf(avgSimVal[avgSimVal.length-1]));
            vals[i] = avgSimVal[avgSimVal.length-1];}

        return out;}

    public String toString(){
        StringBuilder out = new StringBuilder("Displaying Clusters with TotalCost= " + totalCost);
        Business[] catKeys = getMostSimilarKey();
        for (int i=0; i<k; i++){
            out.append("\n(").append(cost[i]).append(") ").append(catKeys[i].getName()).append(": [");

            for (int j=0; j<table[i].size(); j++){
                String name = table[i].get(j).getName();
                for (int m=0; m<12; m++){
                    if (m < name.length()) out.append(name.charAt(m));
                    else out.append(" ");}
                out.append(" ,");
                if (j % 12 == 0){
                    out.append("\n");}}

            out.append("]");}
        out.append("\nTotal Cost (").append(totalCost).append("), Most Similar Keys: ");
        Business [] o = getMostSimilarKey();
        for (Business b: o) out.append(b);
        out.append("\n");
        return out.toString();}

    /*                               *\
    #-------{[ Serialization ]}-------#
    \*                               */

    @Serial
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        k = s.readInt();
        totalCost = s.readDouble();
        double[] cost = new double[k];
        for (int i=0; i<k; i++) cost[i] = s.readDouble();
        this.cost = cost;
        Business[] medoids = new Business[k];
        for (int i=0; i<k; i++) medoids[i] = Business.getBusinessFromId((String) s.readObject());
        this.medoids = medoids;
        ArrayList<Business>[] table = new ArrayList[k];
        for (int i=0; i<k; i++){
            int size = s.readInt();
            table[i] = new ArrayList<>();
            for (int j=0; j<size; j++) table[i].add(Business.getBusinessFromId((String) s.readObject()));}
        this.table = table;}

    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeInt(k);
        s.writeDouble(totalCost);
        for (double d: cost) s.writeDouble(d);
        for (Business b: medoids) s.writeObject(b.getBusiness_id());
        for (ArrayList<Business> ba : table){
            s.writeInt(ba.size());
            for (Business b: ba) s.writeObject(b.getBusiness_id());}}

}
