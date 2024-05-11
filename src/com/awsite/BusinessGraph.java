package com.awsite;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class BusinessGraph implements Serializable {

    // persistent data
    Business[] fullScope;
    ArrayList<Node> createdNodes = new ArrayList<>();
    ArrayList<Link> createdLinks = new ArrayList<>();

    ArrayList<ArrayList<Business>> disjointSets = new ArrayList<>();

    // used for ui
    ArrayList<Business> renderPath = new ArrayList<>();
    ArrayList<Link> seenLinks = new ArrayList<>();

    /*                               *\
    #----{[ Graph Construction ]}-----#
    \*                               */

    class Link implements Serializable {
        Business b1, b2;
        Node one;
        Node two;
        double weight;

        Link(Node one, Node two){
            this.one = one;
            this.two = two;
            for (int i=0; i<Main.allBusinesses.length; i++){
                if(Main.allBusinesses[i].getBusiness_id().equals(two.base.getBusiness_id())){
                    weight = one.base.getSimilarityVector().get(i);
                    break;}}
            createdLinks.add(this);}

        public boolean equal(Link l){
            return one.base.getBusiness_id().equals(l.one.base.getBusiness_id()) &&
                    two.base.getBusiness_id().equals(l.two.base.getBusiness_id());}

        @Serial
        private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
            b1 = Business.getBusinessFromId((String) s.readObject());
            b2 = Business.getBusinessFromId((String) s.readObject());
            weight = s.readDouble();}

        @Serial
        private void writeObject(ObjectOutputStream s) throws IOException {
            s.writeObject(one.base.getBusiness_id());
            s.writeObject(two.base.getBusiness_id());
            s.writeDouble(weight);}
    }

    class Node implements Serializable {
        static final int allowedLinks = 4;

        Business base;
        Link[] links;
        int linkCount = 0;
        int pqIndex;

        double best = -1;
        Node parent;

        public Node(Business b){
            assert b != null;
            base = b;
            links = new Link[allowedLinks];
            createdNodes.add(this);}

        // fill the links in each node with other nodes
        public void populateLinks(int fillAmount){
            if (linkCount >= links.length) return;

            // attempt to fill fillAmount number of links
            Business[] targets = base.getClosestBusinesses(fullScope);
            int nextTargetInd = -1;

            for(Business b: targets){
                assert b != null;}

            while((nextTargetInd < targets.length-1) && (linkCount<fillAmount)){
                nextTargetInd++;

                // find businesses near the node
                Node nextNode = getNode(targets[nextTargetInd]);
                assert nextNode != null;

                // ensure that the next node does not have too many links
                if(nextNode.linkCount >= links.length) continue;

                // do not allow duplicate links
                boolean passed = true;
                for(Link l: nextNode.links){
                    if (l == null) continue;
                    if ((l.one.base.getBusiness_id().equals(base.getBusiness_id())) ||
                            (l.two.base.getBusiness_id().equals(base.getBusiness_id()))){
                        passed = false;
                        break;}}
                if (!passed) continue;


                //if(base.getLocationDifference(nextNode.base) > 20) continue;

                // add new link to the 2 nodes
                links[linkCount] = new Link(this, nextNode);
                linkCount++;
                nextNode.links[nextNode.linkCount] = links[linkCount-1];
                nextNode.linkCount += 1;}
        }

        public boolean equal(Node n){
            return n.base.getBusiness_id().equals(base.getBusiness_id());}

        public double getBest(){
            if(best != -1) return best;
            for(Link l: links){
                if (l == null) continue;
                if(l.weight > best) best = l.weight;}
            return best;}


        @Serial
        private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
            base = Business.getBusinessFromId((String)s.readObject());
            links = new Link[allowedLinks];}

        @Serial
        private void writeObject(ObjectOutputStream s) throws IOException {
            s.writeObject(base.getBusiness_id());}

    }

    public BusinessGraph(boolean created) throws IOException, ClassNotFoundException {

        // load from disk if possible
        if (created){
            BusinessGraph g = (BusinessGraph) new ObjectInputStream(new FileInputStream("graph.src")).readObject();
            fullScope = g.fullScope;
            createdLinks = g.createdLinks;
            createdNodes = g.createdNodes;
            System.out.println("Disjoint-set-count: " + getNmOfDisjointSets());
            return;}

        // display clusters from project 2
        int subSetSize = 1100;
        System.out.println("Displaying Available Clusters");
        for (int i=0; i<Main.c.k; i++){
            if(Main.c.table[i].size() < subSetSize) continue;
            System.out.println("(Index: " + i + ") " + Main.c.cost[i] + ": " + Main.c.getMostSimilarKey()[i].getName());
            for (int j=0; j<Main.c.table[i].size(); j++){
                String name = Main.c.table[i].get(j).getName();
                for (int m=0; m<12; m++){
                    if (m < name.length()) System.out.print(name.charAt(m));
                    else System.out.print(" ");}
                System.out.print(", ");
                if (j % 12 == 0){
                    System.out.println();}}
            System.out.println();}

        // ask the user to select a cluster
        System.out.println("Ender Index of Desired Cluster: ");
        Scanner s = new Scanner(System.in);
        int clusterInd = s.nextInt();
        Business[] cluster = new Business[Main.c.table[clusterInd].size()];
        for (int i=0; i<cluster.length; i++) cluster[i] = Main.c.table[clusterInd].get(i);

        // create new graph with random subset of businesses from the selected cluster
        Random r = new Random();
        int ind = 0;
        Business[] scope = new Business[subSetSize];
        for (Business allBusiness : cluster) {
            if(allBusiness == null) continue;
            if (ind >= subSetSize) break;
            if ((double) r.nextInt(100) / 100 <= (double)subSetSize/(double)cluster.length) {
                scope[ind] = allBusiness;
                ind++;}}
        fullScope = scope;

        // build graph
        for(Business b: scope){
            if (b == null) continue;
            new Node(b);}

        // populate links with nearby nodes
        for(int i=1; i<5; i++){
            for(Node n: createdNodes){
                if (n.base != null) n.populateLinks(i);}}


        System.out.println("Disjoint-set-count: " + getNmOfDisjointSets());

        // put graph file on disk
        FileOutputStream fos2 = new FileOutputStream("graph.src");
        ObjectOutputStream oos2 = new ObjectOutputStream(fos2);
        oos2.writeObject(this);
        oos2.close();
    }

    @Serial
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        int scopeSize = s.readInt();
        fullScope = new Business[scopeSize];
        for(int i=0; i<scopeSize; i++){
            fullScope[i] = Business.getBusinessFromId((String)s.readObject());}
        int nodeCount = s.readInt();
        createdNodes = new ArrayList<>();
        for(int i=0; i<nodeCount; i++){
            createdNodes.add((Node) s.readObject());}
        int linkCount = s.readInt();
        createdLinks = new ArrayList<>();
        for(int i=0; i<linkCount; i++){
            Link l = (Link) s.readObject();
            l.one = getNode(l.b1);
            l.two = getNode(l.b2);
            createdLinks.add(l);
            for(Node n: createdNodes){
                if (n.equal(l.one) || n.equal(l.two) ){
                    n.links[n.linkCount] = l;
                    n.linkCount++;}}
        }
    }

    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeInt(fullScope.length);
        for(Business b: fullScope) s.writeObject(b.getBusiness_id());
        int realSize = 0;
        for(Node n: createdNodes) if (n != null) realSize++;
        s.writeInt(realSize);
        for(Node n: createdNodes) if (n != null) s.writeObject(n);
        s.writeInt(createdLinks.size());
        for(Link l: createdLinks) s.writeObject(l);
    }


    /*                               *\
    #-----{[ Graph Management ]}------#
    \*                               */


    // get node for given Business
    private Node getNode(Business b){
        assert b != null;
        for(Node n: createdNodes){
            if (n.base.getBusiness_id().equals(b.getBusiness_id())) return n;
        } return null;}

    // get all business that eventually connect to the given one
    private ArrayList<Node> getAllConnections(Node root) {
        ArrayDeque<Node> deq = new ArrayDeque<>();
        deq.addLast(root);

        // set all nodes as unseen
        for(Business b: fullScope){
            if (b == null) continue;
            Node n = getNode(b);
            if (n != null) n.parent = null;}

        // use a queue to process all connected nodes
        Node p, nextNode;
        while ((p = deq.pollFirst()) != null) {
            for (Link l : p.links) {
                if(l == null) continue;
                if (l.one.equal(p)) nextNode = l.two;
                else nextNode = l.one;

                // if not seen, add the node to the tree
                if (nextNode.parent == null){
                    nextNode.parent = p;
                    deq.addLast(nextNode);}}}

        // gather all seen nodes
        ArrayList<Node> seen = new ArrayList<>();
        for(Node n: createdNodes){
            if (n.parent != null) seen.add(n);}
        return seen;
    }

    // counts number of independents graphs, saves the largest scope
    public int getNmOfDisjointSets(){
        if(!disjointSets.isEmpty()) return disjointSets.size();
        ArrayList<ArrayList<Business>> allSets = new ArrayList<>();

        // for every unseen business
        for(Business b: fullScope){
            if (b == null) continue;
            Node n = getNode(b);
            if (n == null) continue;
            if(n.parent == null){

                // get new spanning tree, and add it to all sets
                ArrayList<Business> tree = new ArrayList<>();
                ArrayList<Node> output = getAllConnections(n);
                for(Node nu: output){
                    if (nu == null) tree.add(null);
                    else tree.add(nu.base);}

                allSets.add(tree);

                // rebuild 'seen' status after spanning tree creation
                for(ArrayList<Business> set: allSets){
                    for(Business bu : set){
                        if (bu == null) continue;
                        Node nu = getNode(bu);
                        if (nu != null) nu.parent = nu;}}}}

        disjointSets = allSets;
        return allSets.size();}

    public ArrayList<Business> buildShortestPath(Business b1, Business b2) {
        Node root = getNode(b1);
        Node dest = getNode(b2);
        assert ((root != null) && (dest != null));

        // use a priority queue to process all connected nodes
        PQ pq = new PQ(createdNodes, root);
        Node p;
        while ((p = pq.poll()) != null) {
            if(p.equal(dest)) break;
            for (Link l : p.links){

                //get business from link
                if (l == null) continue;
                Node nextNode;
                if (l.one.equal(p)) nextNode = l.two;
                else nextNode = l.one;

                // get the next node in the queue
                double w = p.getBest() + l.weight;
                if (w < nextNode.getBest()) {
                    nextNode.parent = p;
                    nextNode.best = w;
                    pq.resift(nextNode);}}}

        // build path from parent links
        ArrayList<Business> out = new ArrayList<>();
        Node n = dest.parent;
        out.add(dest.base);
        while(n != null){
            if ((n.parent != null) && (n.parent.equal(n))) break;
            out.add(n.base);
            n = n.parent;}
        renderPath = out;
        return out;

    }

    /*                               *\
    #---------{[ UI Output ]}---------#
    \*                               */

    // outputs a xml file, used buildMap.py to render graph
    public void buildPathXML(){
        seenLinks = new ArrayList<>();
        ArrayList<ArrayList<Business>> allPaths = new ArrayList<>();

        // render this result path separately, and give it a different color
        if(!renderPath.isEmpty()){
            allPaths.add(renderPath);
            for(int i=1; i< renderPath.size(); i++){
                Node n1 = getNode(renderPath.get(i-1));
                Node n2 = getNode(renderPath.get(i));
                assert (n1 != null) && (n2 != null);

                for (Link l: n1.links){
                    if (l == null) continue;
                    if ((l.one.equal(n2)) || (l.two.equal(n2))){
                        seenLinks.add(l);
                        break;}}}}

        // get random paths to draw unused nodes
        while ( (double) seenLinks.size() < (double) createdLinks.size() * 0.90 ){
            allPaths.add(getRandomPath());}
        allPaths.addAll(getRemainingPaths());

        // write to xml
        Document dom;
        Element e;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try{DocumentBuilder db = dbf.newDocumentBuilder();
            dom = db.newDocument();
            Element root = dom.createElement("paths");
            boolean first = true;

            for(ArrayList<Business> path: allPaths){
                Element p;
                if (first && !renderPath.isEmpty()) p = dom.createElement("main_path");
                else p = dom.createElement("path");

                // put single path in xml
                for (Business b : path) {

                    // build hover text
                    Business[] links = getDirectLinks(b);
                    assert links != null;
                    StringBuilder hoverText = new StringBuilder();
                    for (Business link : links) {
                        if (link == null) continue;
                        hoverText.append(link.getName()).append(" ,");}

                    e = dom.createElement("node");
                    e.setAttribute("lat", b.getLatitude() + "");
                    e.setAttribute("lng", b.getLongitude() + "");
                    e.setAttribute("nme", b.getName());
                    e.setAttribute("rad", hoverText.toString());
                    p.appendChild(e);}

                first = false;
                root.appendChild(p);}
            dom.appendChild(root);

            try{Transformer tr = TransformerFactory.newInstance().newTransformer();
                tr.setOutputProperty(OutputKeys.INDENT, "yes");
                tr.setOutputProperty(OutputKeys.METHOD, "xml");
                tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                tr.transform(new DOMSource(dom),
                        new StreamResult(new FileOutputStream("gui/path.xml")));
            } catch (TransformerException | IOException te) {
                System.out.println(te.getMessage());}
        } catch (ParserConfigurationException pce) {
            System.out.println("XML Error" + pce);}}

    // get all business directly connected to the given one
    public Business[] getDirectLinks(Business b){
        Node n = getNode(b);
        if (n == null) return null;
        Business[] out = new Business[n.links.length];
        for(int i=0; i<n.links.length; i++){
            if (n.links[i] == null ) out[i] = null;
            else if (n.links[i].two.base.getBusiness_id().equals(b.getBusiness_id())) out[i] = n.links[i].one.base;
            else out[i] = n.links[i].two.base;}
        return out;}

    // helper for buildPathXML
    private ArrayList<Business> getRandomPath(){
        // get random root to start from
        Random r = new Random();
        Node sn = getNode(fullScope[r.nextInt(fullScope.length)]);
        assert sn != null;
        seenLinks.add(sn.links[0]);
        ArrayList<Business> seen = new ArrayList<>();
        seen.add(sn.base);

        Node n = sn;
        while(true){
            int oldCount = seenLinks.size();

            // look for links connected to last the node
            for(Link l: n.links){
                if (l == null) continue;

                // ensure next link is not already in the path
                boolean passed = true;
                for (Link lu: seenLinks){
                    if(lu.equal(l)){
                        passed = false;
                        break;}}
                if (!passed) continue;

                // add the link to the path
                seenLinks.add(l);
                if(l.one.equal(n)) n = l.two;
                else n = l.one;
                seen.add(n.base);
                break;
            }

            if (seenLinks.size() <= oldCount) break;}

        return seen;}

    // helper for buildPathXML
    private ArrayList<ArrayList<Business>> getRemainingPaths(){
        ArrayList<ArrayList<Business>> out = new ArrayList<>();
        for (Link l: createdLinks){
            if(l == null) continue;

            boolean created = false;
            for(Link lu: seenLinks){
                if(lu == null) continue;
                if(lu.equal(l)){
                    created = true;
                    break;}}
            if(created) continue;

            seenLinks.add(l);
            ArrayList<Business> path = new ArrayList<>();
            path.add(l.one.base);
            path.add(l.two.base);
            out.add(path);}

        return out;}

}
