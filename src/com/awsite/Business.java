package com.awsite;

import java.io.IOException;
import java.util.*;

public class Business{

    private String business_id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String postal_code;
    private double latitude;
    private double longitude;
    private double stars;
    private int review_count;
    private int is_open;
    private Object attributes;
    private String categories;
    private Object hours;

    private HFT tfTable;
    private HFT catTable;
    ArrayList<Double> simVec;

    public Business(){}
    public String getBusiness_id(){
        return business_id;}
    public String getName(){
        return name;}
    public String getAddress(){
        return address;}
    public String getCity(){
        return city;}
    public String getState(){
        return state;}
    public String getPostal_code(){
        return postal_code;
    }
    public double getLatitude(){
        return latitude;
    }
    public double getLongitude() {
        return longitude;
    }
    public double getStars(){
        return stars;
    }
    public int getReview_count(){return review_count;}
    public double getIs_open(){
        return is_open;
    }
    public String getCategories(){
        return categories;
    }
    public Object getHours(){
        return hours;
    }
    public Object getAttributes(){
        return attributes;
    }

    // find a business in allBusinesses
    public static Business getBusinessFromId(String id){
        for (Business b: Main.allBusinesses) {if (b.getBusiness_id().equals(id)) return b;}
        return null;}


    /*                               *\
    #-{[ Word-Frequency Comparison ]}-#
    \*                               */

    // returns a double between 0 and 1, 1 being most similar
    public static double getSimilarity(Business business1, Business business2){

        // ignores comparison between the same business
        if (business1.getBusiness_id().equals(business2.getBusiness_id())){
            return -1;}

        if(business1.simVec != null){
            int loc =0;
            for (int i=0; i<Main.allBusinesses.length; i++){
                if (Main.allBusinesses[i].getBusiness_id().equals(business2.getBusiness_id())){
                    loc = i;
                    break;}}
            return business1.getSimilarityVector().get(loc);}
        if(business2.simVec != null){
            int loc =0;
            for (int i=0; i<Main.allBusinesses.length; i++){
                if (Main.allBusinesses[i].getBusiness_id().equals(business1.getBusiness_id())){
                    loc = i;
                    break;}}
            return business2.getSimilarityVector().get(loc);}

        // gets term frequency tables constructed from all reviews associated with the businesses
        HFT wordCountsOne = business1.getTFTable();
        HFT wordCountsTwo = business2.getTFTable();

        // calculates similarity with cosin vector
        double countOne, countTwo;
        double val1=0, val2=0, val3=0;

        //Object[] keyList = wordCountsOne.getAllKeys();
        ArrayList<Object> keyList = new ArrayList<>();
        keyList.addAll(Arrays.asList(wordCountsOne.getAllKeys()));
        keyList.addAll(Arrays.asList(wordCountsTwo.getAllKeys()));

        for (Object key: keyList){
            if (wordCountsOne.contains(key) && wordCountsTwo.contains(key)){
                countOne = wordCountsOne.getFrequency(key) * Main.idfTable.getFrequency(key);
                countTwo = wordCountsTwo.getFrequency(key) * Main.idfTable.getFrequency(key);

                val1 += countOne*countTwo;
                val2 += countOne*countOne;
                val3 += countTwo*countTwo;}}

        return val1 / (Math.sqrt(val2)*Math.sqrt(val3));

    }

    // gets all reviews associated with a given business
    private ArrayList<Review> getReviews(){
        ArrayList<Review> out = new ArrayList<>();
        for (Review review: Main.allReviews){
            if (review==null) continue;
            if (review.getBusiness_id().equals(this.getBusiness_id())){
                out.add(review);}
        }
        return out;
    }

    // returns a list of all categories associated with this business
    public HFT getCategoryTable(){

        // return table if its already in memory
        if (catTable != null){
            return catTable;}

        if(categories == null){
            return new HFT(0);}

        String[] words = categories.split("\\s+");
        HFT out = new HFT(words.length);
        for (String word : words) {
            String cleanedWord = word.toLowerCase(Locale.ROOT);
            out.add(cleanedWord.replace(",", ""), -1);}
        catTable = out;
        return out;
    }

    // gets a term frequency table for all reviews associated with this business
    public HFT getTFTable(){

        // return table if its already in memory
        if (tfTable != null){return tfTable;}

        // get all reviews as an array of words
        ArrayList<Review> reviews = getReviews();
        String text = "";
        for (Review rev: reviews){
            text += rev.getText();}
        String[] words = text.split("\\s+");

        // count the occurrences of each word in this array
        HFT table = new HFT(words.length);
        for (String word : words){
            String cleanedWord = word.toLowerCase(Locale.ROOT);
            table.add(cleanedWord.replace(".", ""), -1);}

        tfTable = table;
        return tfTable;}

    // return values for how similar this business is to every other business
    public ArrayList<Double> getSimilarityVector(){
        if (simVec != null) return simVec;

        // read PHT if possible
        if (Main.tree != null){
            ArrayList<Double> out = new ArrayList<>();
            try {
                out = Main.tree.get(business_id);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();}
            if (!out.isEmpty()) return out;}

        // else: get categories associated with selected business
        HFT selectedCategories = getCategoryTable();
        Object[] catKeys = selectedCategories.getAllKeys();

        // check the similarity between categories
        float[] categoryOverlapMeasure = new float[10000];
        for (int i=0; i<Main.allBusinesses.length; i++){
            Business b = Main.allBusinesses[i];

            // get categories associated another business
            HFT testCategories = b.getCategoryTable();
            if ((testCategories == null) || b.getBusiness_id().equals(getBusiness_id())){
                categoryOverlapMeasure[i] = 0;
                continue;}

            // count number of shared categories
            int sharesCategoryCount = 0;
            for (Object key: catKeys){
                if (testCategories.contains(key)) sharesCategoryCount++;}

            // record percentage of overlapping categories
            categoryOverlapMeasure[i] = sharesCategoryCount/(float)selectedCategories.size;}

        // select businesses with a high number of overlapping categories
        float[] sortedStats = categoryOverlapMeasure.clone();
        Arrays.sort(sortedStats);
        double similarThreshold = 0;
        int outlierIndex = (int)(Main.allBusinesses.length * 0.99);
        for (int i= outlierIndex; i<Main.allBusinesses.length; i++){
            similarThreshold += sortedStats[i];}
        similarThreshold = similarThreshold / (Main.allBusinesses.length - outlierIndex);

        // get similarity between given business and all businesses
        ArrayList<Double> similarityArray = new ArrayList<>();
        for (int i=0; i<Main.allBusinesses.length; i++){
            if (categoryOverlapMeasure[i] >= similarThreshold){
                similarityArray.add(getSimilarity(this, Main.allBusinesses[i]));}
            else {
                similarityArray.add(getSimilarity(this, Main.allBusinesses[i])/2);}}

        simVec = new ArrayList<>(similarityArray);
        return similarityArray;}


    /*                               *\
    #----{[ Location Comparison ]}----#
    \*                               */

    // use Haversine formula to get distances
    public Double getLocationDifference(Business b){
        double a = Math.pow(Math.sin(Math.toRadians(b.latitude - latitude)/ 2), 2) +
                Math.pow(Math.sin(Math.toRadians(b.longitude - longitude) / 2), 2) *
                        Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(b.latitude));
        return 6371 * 2 * Math.asin(Math.sqrt(a));}

    // return the 4 closest business within the scope
    public Business[] getClosestBusinesses(Business[] scope){
        ArrayList<Double> dist = new ArrayList<>();
        for (Business b: scope){
            if (b == null) {
                dist.add(Double.MAX_VALUE);
                continue;}
            dist.add(getLocationDifference(b));}

        ArrayList <Double> oldDist = new ArrayList<>(dist);
        Collections.sort(dist);

        // get sorted list of businesses
        Business[] targets = new Business[dist.size()-1];
        int cont = 0;
        for (int i=0; i<targets.length; i++){
            Business nextB = scope[oldDist.indexOf(dist.get(i+1))];
            if (nextB != null){
                targets[cont] = nextB;
                cont++;
            }
        }
        return targets;}

    public String toString(){
        return name;}

}
