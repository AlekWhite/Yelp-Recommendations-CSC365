package com.awsite;

import com.google.gson.JsonElement;

public class Review{

    private String review_id;
    private String user_id;
    private String business_id;
    private String date;
    private double stars;
    private int useful;
    private int cool;
    private int funny;
    private String text;

    public Review(){}

    public String getReview_id(){return review_id;}
    public String getBusiness_id(){return business_id;}
    public String getUser_id(){return user_id;}
    public double getStars() {return stars;}
    public int getUseful(){return useful;}
    public int getCool(){return cool;}
    public int getFunny(){return funny;}
    public String getText(){return text;}
    public String getDate(){return date;}

}
