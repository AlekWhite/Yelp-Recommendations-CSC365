package com.awsite;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class Gui extends JFrame {

    Business[] selection = new Business[2];
    boolean mode = true;
    JPanel panelG;
    JPanel panelS;
    JPanel panelR = null;
    int sInd = 0;
    int region = 0;

    /*                               *\
    #-----{[ Create UI Elements ]}----#
    \*                               */

    public Gui(){

        // main stuff
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.setLayout(null);
        this.setTitle("CSC365-AW");
        this.setSize(1000, 640);
        this.getContentPane().setBackground(new Color(0x353836));
        this.setVisible(true);

        // add background image
        BufferedImage myPicture = null;
        try {
            myPicture = ImageIO.read(new File("gui/background.png"));
        } catch (IOException e) {
            e.printStackTrace();}
        JLabel picLabel = new JLabel(new ImageIcon(myPicture));
        picLabel.setBounds(0, 0, 1000, 740);
        picLabel.setVisible(true);

        //add selection panel
        panelS = new JPanel();
        panelS.setBounds(600, 20, 350, 200);
        panelS.setBackground(new Color(44, 43, 43));
        panelS.setBorder(BorderFactory.createDashedBorder(new Color(0x293559), 3, 3));
        panelS.setVisible(true);
        panelS.setLayout(null);
        JLabel selcText =  addText("Selection:", 5, 10, new Color(0x58adfc), 18, panelS);
        Map attributes = selcText.getFont().getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        selcText.setFont(selcText.getFont().deriveFont(attributes));
        this.add(panelS);

;
        panelR = new JPanel();
        panelR.setBounds(600, 390, 350, 180);
        panelR.setBackground(new Color(44, 43, 43));
        panelR.setBorder(BorderFactory.createDashedBorder(new Color(0x293559), 3, 3));
        panelR.setLayout(null);
        panelR.setVisible(false);
        this.add(panelR);
        JLabel t = addText("Path:", 5, 10, new Color(0x58adfc), 16, panelR);
        Map atts = t.getFont().getAttributes();
        atts.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        t.setFont(t.getFont().deriveFont(atts));
        panelR.repaint();

        buildRecommendationsPanels(this);
        displaySearchBox();
        this.add(picLabel);
        this.repaint();

    }

    //add path generation panel
    private void buildPathDisplayPanels(JFrame root){
        panelS.setBounds(600, 20, 350, 360);
        JButton sub = addButton("Generate", 10, 330, panelS, 6*12+10);
        JButton clear = addButton("Clear", 100, 330, panelS, 6*12+10);
        JButton display = addButton("Display", 190, 330, panelS, 6*12+10);
        JButton togReg = addButton("Change Region", 160, 30, panelS, 7*12+10);
        JButton tog = addButton("Recommendations", 10, 30, panelS, 10*12+10);
        String[] loc = {"Tampa FL", "Nashville TN", "Indianapolis IN", "Philadelphia PA"};
        final JLabel[] disText = {addText("Location: " + loc[region], 120, 10, new Color(0x357DC2), 12, panelS)};

        panelR.setVisible(true);
        ArrayList<JLabel> pathText = new ArrayList<>();

        // clear button
        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // remove old text
                Component[] oldComps = panelS.getComponents();
                for (Component component: oldComps){
                    if (component.getClass().equals(JLabel.class) && (!((JLabel) component).getText().equals(disText[0].getText()))){
                        panelS.remove(component);}}
                JLabel selcText = addText("Selection:", 5, 10, new Color(0x58adfc), 18, panelS);
                Map attributes = selcText.getFont().getAttributes();
                attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                selcText.setFont(selcText.getFont().deriveFont(attributes));
                selection = new Business[2];
                sInd = 0;
                root.repaint();}});

        //generate button
        sub.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // ensure the selected businesses are connected
                Integer[] setInds = {-1, -2};
                for (int i=0; i<2; i++){
                    for(int j=0; j<Main.graph.disjointSets.size(); j++){
                        for(Business bu: Main.graph.disjointSets.get(j)){
                            if (bu.getBusiness_id().equals(selection[i].getBusiness_id())){
                                setInds[i] = j;
                                break;}}}}

                // if not connected, clear
                if ( (!setInds[0].equals(setInds[1])) || (selection[0].getBusiness_id().equals(selection[1].getBusiness_id()))){
                    Component[] oldComps = panelS.getComponents();
                    for (Component component: oldComps){
                        if (component.getClass().equals(JLabel.class)){
                            panelS.remove(component);}}
                    JLabel selcText = addText("Selection:", 5, 10, new Color(0x58adfc), 18, panelS);
                    Map attributes = selcText.getFont().getAttributes();
                    addText("No-Path", 290, 390, new Color(0xE11F1F), 12, panelS);
                    attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                    selcText.setFont(selcText.getFont().deriveFont(attributes));
                    disText[0] = addText("Location: " + loc[region], 120, 10, new Color(0x357DC2), 12, panelS);

                    selection = new Business[2];
                    sInd = 0;
                    root.repaint();}

                // otherwise, get and display path
                else {

                    Double[][] cityLocation = {{-82.6588, 27.9017}, {-86.7816, 36.1627}, {-86.15181, 39.7684}, {-75.1652, 39.9526}};
                    int zone = region, rng = 3;
                    double[] LngRange = {selection[0].getLongitude()+rng, selection[0].getLongitude()-rng};
                    double[] LatRange = {selection[0].getLatitude()+rng, selection[0].getLatitude()-rng};
                    for (int i=0; i<cityLocation.length; i++){
                        if ((cityLocation[i][0] < LngRange[0]) &&
                            (cityLocation[i][0] > LngRange[1]) &&
                            (cityLocation[i][1] < LatRange[0]) &&
                            (cityLocation[i][1] > LatRange[1])) {
                            zone = i;
                            break;}}

                    for(JLabel jl: pathText){if(jl != null) panelR.remove(jl);}

                    ArrayList<Business> path = Main.graph.buildShortestPath(selection[0], selection[1]);
                    String out = "";
                    int yOffset = 0;
                    for(Business b: path){
                        if (out.length() + b.getName().length() < 56) out += " -> " + b.getName();
                        else{
                            pathText.add(addText(out, 5, 25+yOffset, new Color(0xafb3b0), 10, panelR));
                            out = " -> " + b.getName();
                            yOffset += 16;}}
                    pathText.add(addText(out, 5, 25+yOffset, new Color(0xafb3b0), 10, panelR));

                    System.out.println(path);
                    Main.graph.buildPathXML();
                    try {
                        Process p = Runtime.getRuntime().exec("cmd /c start \"\" C:/Users/alekw/IdeaProjects/CSC365A1/gui/run.bat " + zone);
                    } catch (IOException ex) {
                        ex.printStackTrace();}
                    addText("Done", 290, 390, new Color(0x367A4D), 12, panelS);}
            }});

        // render selected region
        display.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Process p = Runtime.getRuntime().exec("cmd /c start \"\" C:/Users/alekw/IdeaProjects/CSC365A1/gui/run.bat " + region);
                } catch (IOException ex) {
                    ex.printStackTrace();}}
        });

        // change selected region
        togReg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (region == 3) region = 0;
                else  region += 1;
                panelS.remove(disText[0]);
                String[] loc = {"Tampa FL", "Nashville TN", "Indianapolis IN", "Philadelphia PA"};
                disText[0] = addText("Location: " + loc[region], 120, 10, new Color(0x357DC2), 12, panelS);
                root.repaint();

            }
        });

        // switch mode button
        tog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // remove elements here
                panelS.setBounds(600, 20, 350, 200);
                panelS.remove(tog);
                panelS.remove(sub);
                panelS.remove(clear);
                panelS.remove(display);
                panelS.remove(togReg);
                panelS.remove(disText[0]);
                panelR.setVisible(false);
                root.repaint();
                buildRecommendationsPanels(root);
                mode = true;
            }});

        this.repaint();}

    //add recommendation panel
    private void buildRecommendationsPanels(JFrame root){
        if (panelG == null){
            panelG = new JPanel();
            panelG.setBounds(600, 250, 350, 320);
            panelG.setBackground(new Color(44, 43, 43));
            panelG.setBorder(BorderFactory.createDashedBorder(new Color(0x293559), 3, 3));
            panelG.setVisible(true);
            panelG.setLayout(null);
            this.add(panelG);
            JLabel recText =  addText("Recommendations:", 5, 10, new Color(0x58adfc), 18, panelG);
            Map attributes1 = recText.getFont().getAttributes();
            attributes1.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            recText.setFont(recText.getFont().deriveFont(attributes1));}
        panelG.setVisible(true);

        JLabel selcText =  addText("Selection:", 5, 10, new Color(0x58adfc), 18, panelS);
        Map attributes = selcText.getFont().getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        selcText.setFont(selcText.getFont().deriveFont(attributes));

        JButton tog = addButton("Generate Path", 5, 30, panelS, 10*12+10);
        tog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mode = false;
                panelS.remove(tog);
                panelS.remove(selcText);
                panelG.setVisible(false);
                root.repaint();
                buildPathDisplayPanels(root);}});

        this.repaint();}

    // adds business search-box
    private void displaySearchBox(){

        // background panel
        JPanel panel = new JPanel();
        panel.setBounds(20, 20, 550, 550);
        panel.setBackground(new Color(44, 43, 43));
        panel.setBorder(BorderFactory.createDashedBorder(new Color(0x293559), 3, 3));
        panel.setVisible(true);
        panel.setLayout(null);

        // input box
        JTextField inputBox = new JTextField(255);
        inputBox.setBackground(new Color(0xd1c3ab));
        inputBox.setBorder(BorderFactory.createDashedBorder(new Color(0xd1c3ab), 3, 3));
        inputBox.setBounds(185, 10, 160, 20 );
        inputBox.setVisible(true);
        panel.add(inputBox);

        JLabel srchText = addText("Search Restaurants:", 10, 10, new Color(0x58adfc), 16, panel);
        Map attributes = srchText.getFont().getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        srchText.setFont(srchText.getFont().deriveFont(attributes));


        // add buttons
        ArrayList<String> savedButtons = new ArrayList<>();
        JButton subButton = addButton("Submit", 360, 10, panel, 6*12+10);
        savedButtons.add(subButton.getText());
        panel.add(subButton);
        JButton randButton = addButton("Random", 450, 10, panel, 6*12+10);
        panel.add(randButton);
        savedButtons.add(randButton.getText());

        // submit button function
        subButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // add result to panel
                displaySearchResult(inputBox.getText(), panel, savedButtons);
            }});

        // random button function
        randButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // remove old text
                Component[] oldComps = panelS.getComponents();
                for (Component component: oldComps){
                    if (component.getClass().equals(JLabel.class)){
                        panelS.remove(component);}}

                JLabel selcText =  addText("Selection:", 5, 10, new Color(0x58adfc), 18, panelS);
                Map attributes = selcText.getFont().getAttributes();
                attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                selcText.setFont(selcText.getFont().deriveFont(attributes));
                Business b = getRandomBusiness();
                displayBusiness(b, 5, 45, panelS);
                displayRecommendations(b, panelG);
            }});

        this.add(panel);
        this.repaint();

        displaySearchResult("A", panel, savedButtons);

    }

    // shows restaurants with a similar name to prompt
    private void displaySearchResult(String prompt, JPanel panel, ArrayList<String> savedButtons){

        // remove old text and buttons
        Component[] oldComps = panel.getComponents();
        for (Component component: oldComps){

            if (component.getClass().equals(JLabel.class)){
                panel.remove(component);}
            else if (component.getClass().equals(JButton.class) &&
                    !(savedButtons.contains(((JButton)component).getText()))){
                panel.remove(component);}
        }

        Color[] textColors = {
                new Color(231, 67, 67),
                new Color(20, 128,128),
                new Color(80, 159, 238),
                new Color(245, 140, 65),
                new Color(246, 246, 72),
                new Color(53, 157, 64),
                new Color(174, 102, 241),
                new Color(163, 246, 115),
                new Color(243, 140, 234),
                new Color(20, 229, 173, 255),};

        // add result to panel
        ArrayList<Business> results = searchForBusiness(prompt);
        for (int i=0; i<results.size(); i++){
            Business b = results.get(i);

            Color col = new Color(0xafb3b0);
            for(int j=0; j<Main.graph.disjointSets.size(); j++){
                for(Business bu: Main.graph.disjointSets.get(j)){
                    if (bu == null) continue;
                    if (bu.getBusiness_id().equals(b.getBusiness_id())){
                        if (j<textColors.length) col = textColors[j];
                        else  col = textColors[textColors.length-1];
                        break;}}}

            String[] loc = {"Tampa FL", "Nashville TN", "Indianapolis IN", "Philadelphia PA"};
            String text = b.getName() + " (" + b.getAddress() + ", " + b.getCity() + ", " + b.getPostal_code()  + ")";
            addText(text.substring(0, Math.min(text.length(), 65)), 25, 60+(i*20), col ,12, panel);

            // add select buttons
            JButton button = addButton("+", 510, 60+(i*20), panel, 12+10);
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setForeground(new Color(0x92BBFF));
            button.setMinimumSize(new Dimension(16, 16));
            int finalI = i;
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // remove old text
                    Component[] oldComps = panelS.getComponents();
                    for (Component component: oldComps){
                        if (component.getClass().equals(JLabel.class) && (!((JLabel) component).getText().equals( "Location: " + loc[region]))){
                            panelS.remove(component);}}
                    JLabel selcText =  addText("Selection:", 5, 10, new Color(0x58adfc), 18, panelS);
                    Map attributes = selcText.getFont().getAttributes();
                    attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                    selcText.setFont(selcText.getFont().deriveFont(attributes));

                    // recommendations
                    if (mode){
                        displayBusiness(results.get(finalI), 5, 80, panelS);
                        displayRecommendations(results.get(finalI), panelG);}

                    // paths
                    else {
                        selection[sInd] = results.get(finalI);
                        sInd = 1-sInd;
                        if (selection[0] != null) displayBusiness(selection[0], 5, 80, panelS);
                        if (selection[1] != null) displayBusiness(selection[1], 5, 220, panelS);}}});

            panel.add(button);}

        // re-add text
        JLabel srchText = addText("Search Restaurants:", 10, 10, new Color(0x58adfc), 16, panel);
        Map attributes = srchText.getFont().getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        srchText.setFont(srchText.getFont().deriveFont(attributes));
    }

    // shows recommended restaurants
    private void displayRecommendations(Business business, JPanel panelG){

        // remove old text
        Component[] oldComps = panelG.getComponents();
        for (Component component: oldComps){
            if (component.getClass().equals(JLabel.class)){
                panelG.remove(component);}}

        Business[] out = Main.getRecommendations(business);
        displayBusiness(out[0], 5, 55, panelG);
        displayBusiness(out[1], 5, 200, panelG);

        addText( "(" + out[2].getName() + ")", 5, 30, new Color(0x58adfc), 12, panelG);

        //re add text
        JLabel recText =  addText("Recommendations:", 5, 10, new Color(0x58adfc), 18, panelG);
        Map attributes1 = recText.getFont().getAttributes();
        attributes1.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        recText.setFont(recText.getFont().deriveFont(attributes1));

    }


    /*                               *\
    #----------{[ Helpers ]}----------#
    \*                               */

    // get the first 24 business name that start with prompt
    public static ArrayList<Business> searchForBusiness(String prompt){
        ArrayList<Business> results = new ArrayList<>();
        Business[] fullSet = Main.allBusinesses;

        //TODO: make this better
        if (prompt.charAt(0) == '/'){
            int ind = "abcdefghijklmnop".indexOf(prompt.charAt(1));
            ArrayList<Business> set = Main.graph.disjointSets.get(ind);
            fullSet = new Business[set.size()];
            for(int i=0; i<set.size(); i++) fullSet[i] = set.get(i);

            if(prompt.charAt(2) != ' '){
                int pos = Integer.parseInt(prompt.substring(2)+"")*24;
                for (int i=pos; i<fullSet.length; i++){
                    if (results.size() == 24) break;
                    results.add(fullSet[i]);}
                return results;}
            prompt = prompt.substring(3);
        }

        for (Business business: fullSet){
            String bisStr = business.getName() + " (" + business.getAddress() + ", " + business.getCity() + ", " + business.getPostal_code() + ")";
            if (prompt.length() <= bisStr.length()){
                if (bisStr.startsWith(prompt)){
                    results.add(business);}}
            if (results.size() >= 24){
                return results;}}
        return results;}

    // gets a random business
    public static Business getRandomBusiness(){
        Random rand = new Random();
        int ind = rand.nextInt(Main.allBusinesses.length);
        return Main.allBusinesses[ind];}


    /*                               *\
    #------{[ General display ]}------#
    \*                               */

    // display a business object
    private void displayBusiness(Business business, int xPos, int yPos, JPanel loc){

        Color headerTextColor =  new Color(0x1b873a);
        Color bodyTextColor = new Color(0x999fa8);

        addText("Name: ", xPos, yPos, headerTextColor, 12, loc);
        addText(business.getName(), xPos+50, yPos, new Color(0xcccf34), 14, loc);

        addText("Address: ", xPos, yPos+40, headerTextColor, 12, loc);
        addText(business.getAddress(), xPos+75, yPos+30, bodyTextColor, 14, loc);
        addText(business.getCity() + " " + business.getState() + " " + business.getPostal_code(),
                xPos+75, yPos+50, bodyTextColor, 14, loc);


        addText("Stars: ", xPos, yPos+80, headerTextColor, 12, loc);
        addText(String.format("%.1f", business.getStars()) + "/5.0"  , xPos+60, yPos+80, bodyTextColor, 14, loc);

        addText("Review-Count: ", xPos+140, yPos+80, headerTextColor, 12, loc);
        addText(String.valueOf(business.getReview_count()), xPos+240, yPos+80, bodyTextColor, 14, loc);


    }

    // general method for adding text
    private JLabel addText(String text, int xPos, int yPos, Color color, int size, JPanel loc){
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Consolas", Font.PLAIN, size));
        label.setBounds(xPos, yPos, text.length()*12+10, 20);
        loc.add(label);
        loc.repaint();
        return label;
    }

    // general form for adding buttons
    private JButton addButton(String text, int xPos, int yPos, JPanel loc, int width){
        JButton button = new JButton(text);
        button.setBounds(xPos, yPos, width, 20);
        button.setBorder(BorderFactory.createLineBorder(new Color(0x232222), 1));
        button.setBackground(new Color(0x9B9A9A));
        button.setFocusPainted(false);
        loc.add(button);
        return button;
    }

}
