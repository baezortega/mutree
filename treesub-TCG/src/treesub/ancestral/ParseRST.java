package treesub.ancestral;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import pal.tree.Node;
import pal.tree.Tree;
import pal.tree.TreeTool;
import treesub.Constants;
import treesub.Utils;
import treesub.tree.Attributes;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;


/**
 * @author tamuri@ebi.ac.uk
 *
 **/
public class ParseRST {

    private List<String> names = Lists.newArrayList();
    private Map<String, List<String>> seqs;
    private Tree[] trees;

    private Map<Node, Attributes> nodeAttributes = Maps.newHashMap();
    private Map<Node, List<Substitution>> nodeSubstitutions = Maps.newHashMap();


    public static void main(String[] args) throws Exception {
        ParseRST p = new ParseRST();
        p.run(args[0], args[1], args[2]);  // MODIFIED: uses 3 directories (seqs, reconstruction, output)
    }

    // MODIFIED: override run function using 3 parameters (seqs, reconstruction, output directories)
    public void run(String dir1, String dir2, String dir3) throws  Exception{ 
        loadRealNames(dir1 + "/" + Constants.ALIGNMENT_NAMES);

        seqs = getSequences_RAxML(dir1 + "/" + Constants.RAXML_ORIGINAL_SEQS, dir2 + "/" + Constants.RAXML_RECONSTRUCTION_SEQS);
        trees = getTrees_RAxML(dir2 + "/" + Constants.RAXML_ORIGINAL_TREE, dir2 + "/" + Constants.RAXML_RECONSTRUCTION_TREE);
        traverse(trees[0].getRoot());

        writeResults(dir3);
    }
    
    public void run(String f) throws  Exception{
        // TODO: What is they haven't run treesub and just parsing PAML?!
        loadRealNames(f + "/" + Constants.ALIGNMENT_NAMES);

        seqs = getSequences(f + "/" + Constants.PAML_RECONSTRUCTION_FILE);
        trees = getTrees(f + "/" + Constants.PAML_RECONSTRUCTION_FILE);
        traverse(trees[0].getRoot());

        writeResults(f);
    }

    private void loadRealNames (String f) throws Exception {
        File nameFile = new File(f);
        if (nameFile.exists()) {
            names = Files.readLines(new File(f), Charset.defaultCharset());
        }
    }

    private void traverse(Node n) {
        List<Substitution> substitutions;

        if (n.isLeaf()) {

            substitutions = getSubstitutions(getSequenceKey(n), getSequenceKey(n.getParent()));
            nodeSubstitutions.put(n, substitutions);

            Attributes a = getAttributes(n, substitutions);
            nodeAttributes.put(n, a);

        } else {

            for (int i = 0; i < n.getChildCount(); i++) {
                traverse(n.getChild(i));
            }

            if (!n.isRoot()) {
                substitutions = getSubstitutions(getSequenceKey(n), getSequenceKey(n.getParent()));
                nodeSubstitutions.put(n, substitutions);
                Attributes a = getAttributes(n, substitutions);
                nodeAttributes.put(n, a);
            }
        }
    }

    private void writeResults(String f) throws Exception  {
        // Write out the NEXUS format tree
        BufferedWriter out = new BufferedWriter(new FileWriter(f + "/substitutions.tree"));

        out.write("#NEXUS\n");
        out.write("begin taxa;\n");
        out.write("\tdimensions ntax=" + trees[0].getExternalNodeCount() + ";\n");
        out.write("\ttaxlabels\n");
        for (int i = 0; i < trees[0].getExternalNodeCount(); i++) {
            out.write("\t\t'" + nodeAttributes.get(trees[0].getExternalNode(i)).get(Attributes.Key.REALNAME) + "'");
            out.write(nodeAttributes.get(trees[0].getExternalNode(i)).toString());
            out.write("\n");
        }
        out.write(";\nend;\n\n");
        out.write("begin trees;\n");

        out.write("tree tree_1 = [&R] ");
        Utils.printNH(new PrintWriter(out), trees[0].getRoot(), nodeAttributes);
        out.write(";\nend;\n");
        out.close();

        // table of substitutions
        BufferedWriter subs_out = new BufferedWriter(new FileWriter(f + "/substitutions.tsv"));
        subs_out.write("branch\tsite\tcodon_from\tcodon_to\taa_from\taa_to\tstring\tnon_synonymous\n");


        for (Map.Entry<Node, List<Substitution>> e : nodeSubstitutions.entrySet()) {
            String name = nodeAttributes.get(e.getKey()).get(Attributes.Key.REALNAME);
            List<Substitution> substitutions = e.getValue();

            for (Substitution s : substitutions) {
                subs_out.write(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
                        name,
                        s.site,
                        s.codonFrom,
                        s.codonTo,
                        s.aaFrom,
                        s.aaTo,
                        s.toString(),
                        s.aaFrom == s.aaTo ? "" : "*"));
            }

        }

        subs_out.close();
    }

    private Attributes getAttributes(Node node, List<Substitution> substitutions) {

        String name;
        if (node.isLeaf() && names.size() > 0) {
            name = names.get(Integer.parseInt(getSequenceKey(node).split("_")[1]) - 1);
        } else {
            // MODIFICATION: Change REALNAME of internal nodes (branches)
            //name = getSequenceKey(node);
            name = "branch_" + Integer.toString(getBranchNumber(node));
        }

        Attributes a = new Attributes(Attributes.Key.REALNAME, name);
        a.add(Attributes.Key.NUMBER, Integer.toString(getBranchNumber(node)));

        if (substitutions.size() > 0) a.add(Attributes.Key.ALLSUBS, substitutions.toString());

        Collection<Substitution> nonsynSubs = Collections2.filter(substitutions, Predicates.not(new Substitution.isSynSubPredicate()));
        if (nonsynSubs.size() > 0) a.add(Attributes.Key.NONSYNSUBS, nonsynSubs.toString());

        a.add(Attributes.Key.FULL,
                String.format("%s - %s %s",
                        a.get(Attributes.Key.NUMBER),
                        a.get(Attributes.Key.REALNAME),
                        a.get(Attributes.Key.ALLSUBS)));

        a.add(Attributes.Key.NAME_AND_SUBS,
                String.format("%s %s",
                        a.get(Attributes.Key.REALNAME),
                        a.get(Attributes.Key.NONSYNSUBS)));

        return a;
    }

    private int getBranchNumber(Node n) {
        return (n.isLeaf() ? 0 : trees[0].getExternalNodeCount()) + n.getNumber();
    }


    private String getSequenceKey(Node n) {
        // MODIFIED: now all labels can be obtained from trees[1]
        if (n.isLeaf()) {
            return trees[1].getExternalNode(n.getNumber()).getIdentifier().getName();
            //return trees[0].getExternalNode(n.getNumber()).getIdentifier().getName();
        } else {
            return trees[1].getInternalNode(n.getNumber()).getIdentifier().getName();
            //return "node#" + trees[1].getInternalNode(n.getNumber()).getIdentifier().getName();
        }
    }

    private List<Substitution> getSubstitutions(String child, String parent) {

        List<String> childSeq = seqs.get(child);
        List<String> parentSeq = seqs.get(parent);

        List<Substitution> substitutions = Lists.newArrayList();

        for (int i = 0; i < childSeq.size(); i++) {
            if (!childSeq.get(i).equals(parentSeq.get(i))) {
                
                char aaFrom, aaTo;
                Set<Character> aaFromSet = Utils.getAminoAcidsForCodonTLA(parentSeq.get(i));
                if (aaFromSet.size() > 1) aaFrom = '*'; else aaFrom = aaFromSet.iterator().next();

                Set<Character> aaToSet = Utils.getAminoAcidsForCodonTLA(childSeq.get(i));
                if (aaToSet.size() > 1) aaTo = '*'; else aaTo = aaToSet.iterator().next();

                // MODIFIED: substitution (codon) site is indicated in 0-base
                //Substitution s = new Substitution(i + 1, parentSeq.get(i), childSeq.get(i), aaFrom, aaTo);
                Substitution s = new Substitution(i, parentSeq.get(i), childSeq.get(i), aaFrom, aaTo);
                substitutions.add(s);

            }
        }

        return substitutions;
    }

    private Tree[] getTrees(String file) throws Exception {
        Tree[] trees = new Tree[2];

        BufferedReader reader = Files.newReader(new File(file), Charsets.US_ASCII);

        while (!reader.readLine().startsWith("Ancestral reconstruction by")) { /* empty */ }

        String line;

        // skip all lines until we read the first tree
        while (!(line = reader.readLine()).startsWith("(")) { /* empty */ }

        // this is the true tree - use this to get the true branch lengths
        trees[0] = TreeTool.readTree(new StringReader(line));

        // skip all lines until we have read two more trees
        for (int i = 0; i < 2; i++) while (!(line = reader.readLine()).startsWith("(")) { /* empty */ }

        // this third tree is tree with the branches labeled - use this to get node/branch names
        trees[1] = TreeTool.readTree(new StringReader(line));

        reader.close();

        return trees;
    }

    private Map<String, List<String>> getSequences(String file) throws Exception{
        Map<String, List<String>> sequences = Maps.newHashMap();

        // Read the sequences (in particular, the reconstructed nodes)
        BufferedReader reader = Files.newReader(new File(file), Charsets.US_ASCII);

        while (!reader.readLine().startsWith("List of extant and reconstructed sequences")) { /* empty */ }

        reader.readLine(); // Skip blank line
        reader.readLine(); // Skip header
        reader.readLine(); // Skip black line

        String line;

        // Until we reach the end of the sequences
        while ((line = reader.readLine()).trim().length() > 0) {
            List<String> parts = Lists.newArrayList(line.split("\\s+"));

            String key, full;
            if (parts.get(0).equals("node")) {
                // This is a reconstructed sequence of ancestral node
                key = "node" + parts.get(1);
                parts.remove(0); // 'node'
                parts.remove(0); // '#n'

                full = Joiner.on("").join(parts);
            } else {
                // This is a extant sequence
                key = parts.get(0);
                parts.remove(0); // sequence name
                full = Joiner.on("").join(parts);
            }

            List<String> sequence = Lists.newArrayList();
            for (int i = 0; i < full.length(); i += Constants.CODON_LENGTH) {
                sequence.add(full.substring(i, i + Constants.CODON_LENGTH));
            }
            sequences.put(key, sequence);
        }

        /*
        for (Map.Entry<String, List<String>> e : sequences.entrySet()) {
            System.out.printf("%s\t%s\n", e.getKey(), Joiner.on(",").join(e.getValue()));
        }
        */

        reader.close();

        return sequences;
    }
    
    
    // MODIFIED: ADDITIONAL FUNCTIONS TO INTEGRATE RAXML ANCESTRAL SEQUENCE RECONSTRUCTION
    /***************************************************************************************/
    
    private Map<String, List<String>> getSequences_RAxML(String file1, String file2) throws Exception{
        Map<String, List<String>> sequences = Maps.newHashMap();

        // 1. Read extant (original) sequences
        BufferedReader reader = Files.newReader(new File(file1), Charsets.US_ASCII);
        reader.readLine(); // Skip header

        // Until we reach the end of the sequences
        String line;
        List<String> parts, sequence;
        String key, seq;
        while ((line = reader.readLine()) != null) {
            parts = Lists.newArrayList(line.split("\\s+"));

            key = parts.get(0);  // tip node name
            seq = parts.get(1);  // extant sequence

            sequence = Lists.newArrayList();
            for (int i = 0; i < seq.length(); i += Constants.CODON_LENGTH) {
                sequence.add(seq.substring(i, i + Constants.CODON_LENGTH));
            }
            sequences.put(key, sequence);
            
            //System.out.println(key);
        }
        System.out.println("All extant sequences read");
    
        reader.close();
        
        // 2. Read ancestral (reconstructed) sequences
        reader = Files.newReader(new File(file2), Charsets.US_ASCII);

        // Until we reach the end of the sequences
        while ((line = reader.readLine()) != null) {
            parts = Lists.newArrayList(line.split("\\s+"));

            key = parts.get(0);  // ancestral node name
            seq = parts.get(1);  // ancestral sequence

            sequence = Lists.newArrayList();
            for (int i = 0; i < seq.length(); i += Constants.CODON_LENGTH) {
                sequence.add(seq.substring(i, i + Constants.CODON_LENGTH));
            }
            sequences.put(key, sequence);
            
            //System.out.println(key);
        }
        System.out.println("All ancestral sequences read");
           
        reader.close();
        
        return sequences;
    }
    
    
    private Tree[] getTrees_RAxML(String file1, String file2) throws Exception {
        Tree[] trees = new Tree[2];

        // 1. Read original ROOTED RAxML tree
        // This is the true tree - use this to get the true branch lengths
        BufferedReader reader = Files.newReader(new File(file1), Charsets.US_ASCII);
        
        String line = reader.readLine();
        trees[0] = TreeTool.readTree(new StringReader(line));
        
        reader.close();
        
        
        // 2. Read node-labelled RAxML tree
        // This is the tree with the nodes labeled - use this to get ancestral node names
        reader = Files.newReader(new File(file2), Charsets.US_ASCII);
        
        line = reader.readLine();
        trees[1] = TreeTool.readTree(new StringReader(line));
        
        reader.close();

        return trees;
    }
}
