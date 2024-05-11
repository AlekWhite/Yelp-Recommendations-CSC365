package com.awsite;

import java.util.Collection;

class PQ {
    final BusinessGraph.Node[] array;
    int size;
    static int leftOf(int k) { return (k << 1) + 1; }
    static int rightOf(int k) { return leftOf(k) + 1; }
    static int parentOf(int k) { return (k - 1) >>> 1; }
    PQ(Collection<BusinessGraph.Node> nodes, BusinessGraph.Node root) {
        array = new BusinessGraph.Node[nodes.size()];
        root.best = 0;
        root.pqIndex = 0;
        array[0] = root;
        int k = 1;
        for (BusinessGraph.Node p : nodes) {
            p.parent = null;
            if (p != root) {
                p.best = Double.MAX_VALUE;
                array[k] = p; p.pqIndex = k++;
            }
        }
        size = k;
    }
    void resift(BusinessGraph.Node x) {
        int k = x.pqIndex;
        assert (array[k] == x);
        while (k > 0) {
            int parent = parentOf(k);
            BusinessGraph.Node p = array[parent];
            if (x.getBest() >= p.getBest())
                break;
            array[k] = p; p.pqIndex = k;
            k = parent;
        }
        array[k] = x; x.pqIndex = k;
    }
    void add(BusinessGraph.Node x) { // unused; for illustration
        x.pqIndex = size++;
        resift(x);
    }

    BusinessGraph.Node poll() {
        int n = size;
        if (n == 0) return null;
        BusinessGraph.Node least = array[0];
        if(least.best == Double.MAX_VALUE) return null;
        size = --n;
        if (n > 0) {
            BusinessGraph.Node x = array[n]; array[n] = null;
            int k = 0, child;  // while at least a left child
            while ( (child = leftOf(k)) < n ) {
                BusinessGraph.Node c = array[child];
                int right = child + 1;
                if (right < n) {
                    BusinessGraph.Node r = array[right];
                    if (c.getBest() > r.getBest()) {
                        c = r;
                        child = right;
                    }
                }
                if (x.getBest() <= c.getBest())
                    break;
                array[k] = c; c.pqIndex = k;
                k = child;
            }
            array[k] = x; x.pqIndex = k;
        }
        return least;
    }
}
