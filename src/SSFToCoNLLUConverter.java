import cz.ufal.udapi.core.impl.DefaultBundle;
import cz.ufal.udapi.core.impl.DefaultDocument;
import cz.ufal.udapi.core.impl.DefaultEnhancedDeps;
import cz.ufal.udapi.core.impl.DefaultNode;
import cz.ufal.udapi.core.io.impl.CoNLLUWriter;
import in.co.sanchay.GlobalProperties;
import in.co.sanchay.corpus.ssf.SSFProperties;
import in.co.sanchay.corpus.ssf.SSFSentence;
import in.co.sanchay.corpus.ssf.SSFStory;
import in.co.sanchay.corpus.ssf.features.FeatureAttribute;
import in.co.sanchay.corpus.ssf.features.FeatureStructure;
import in.co.sanchay.corpus.ssf.features.FeatureStructures;
import in.co.sanchay.corpus.ssf.features.FeatureValue;
import in.co.sanchay.corpus.ssf.features.impl.FSProperties;
import in.co.sanchay.corpus.ssf.features.impl.FeatureStructuresImpl;
import in.co.sanchay.corpus.ssf.impl.SSFSentenceImpl;
import in.co.sanchay.corpus.ssf.impl.SSFStoryImpl;
import in.co.sanchay.corpus.ssf.tree.SSFLexItem;
import in.co.sanchay.corpus.ssf.tree.SSFNode;
import in.co.sanchay.corpus.ssf.tree.SSFPhrase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.nio.file.Path;
import java.util.Map;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Scanner;

import cz.ufal.udapi.core.*;
public class SSFToCoNLLUConverter {

    SSFStory ssfStory = new SSFStoryImpl();

    public SSFToCoNLLUConverter() {
        ssfStory = new SSFStoryImpl();
    }
    private static final Map<String, String> drelToUdMap = new HashMap<>();

    static {
        drelToUdMap.put("k1", "nsubj");
        drelToUdMap.put("k2", "obj");
        drelToUdMap.put("k3", "obl:instrument");
        drelToUdMap.put("k4", "obl:beneficiary");
        drelToUdMap.put("k5", "obl:source");
        drelToUdMap.put("k6", "nmod:poss");
        drelToUdMap.put("k7", "obl:loc");
        drelToUdMap.put("k7t", "obl:tmod");
        drelToUdMap.put("k7p", "obl:purpose");
        drelToUdMap.put("k1s", "csubj");
        drelToUdMap.put("r6", "nmod:poss");
        drelToUdMap.put("ccof", "conj");
        drelToUdMap.put("rsym", "punct");
        drelToUdMap.put("pof", "xcomp");
        drelToUdMap.put("nmod", "nmod");
        drelToUdMap.put("nmod:poss", "nmod:poss");
        drelToUdMap.put("nmod:npmod", "nmod");
        drelToUdMap.put("nmod:advmod", "advmod");
        drelToUdMap.put("nmod:lmod", "obl:loc");
    }

    public static String getUDRelation(String drel) {
        return drelToUdMap.getOrDefault(drel, "unknown");
    }
    private void processLexItem(SSFLexItem ssfLexItem, Root root, List<Node> nodesList, List<Integer> headIds) {
        Node node = root.createChild();

        String lemma = "_";
        String upos = "X";
        String xpos = ssfLexItem.getName();
        String feats = "_";
        String deprel = "_";
        int headId = 0;

        node.setForm(ssfLexItem.getLexData());
        //System.out.println(ssfLexItem.getLexData());

        FeatureStructures fs = ssfLexItem.getFeatureStructures();
        if (fs == null) {
            nodesList.add(node);
            headIds.add(headId);
            return;
        }

        FeatureStructure lexFSS = fs.getAltFSValue(0);
        List<String> featureAttributes = lexFSS.getAttributeNames();
        StringBuilder featsBuilder = new StringBuilder();
        for (String featureName : featureAttributes) {
            String lexFValue = lexFSS.getAttributeValueString(featureName);
//            System.out.print(featureName.toLowerCase());
//            System.out.print(" ");
//            System.out.println(lexFValue);

            switch (featureName.toLowerCase()) {
                case "lex":
                    lemma = lexFValue;
                    break;
                case "cat":
                    upos = lexFValue;
                    break;
                case "deprel":
                    deprel = lexFValue;
                    break;
                case "head":
                    try {
                        headId = Integer.parseInt(lexFValue);
                    } catch (NumberFormatException e) {
                        headId = 0;
                    }
                    break;
                default:
                    if (featsBuilder.length() > 0) {
                        featsBuilder.append("|");
                    }
                    featsBuilder.append(featureName).append("=").append(lexFValue);
            }
        }
        feats = featsBuilder.length() > 0 ? featsBuilder.toString() : "_";

        // Set properties for the token (lexical item)
        node.setLemma(lemma);
        node.setUpos(upos);
        node.setXpos(xpos);
        node.setFeats(feats);
        node.setDeprel(deprel);
        node.setDeps(new DefaultEnhancedDeps("_", root)); // Prevent null pointer error

        nodesList.add(node);
        headIds.add(headId);

        // Check if this lex item is part of a chunk, if so, transfer chunk properties
        if (ssfLexItem.getParent() instanceof SSFPhrase) {
            SSFPhrase chunkNode = (SSFPhrase) ssfLexItem.getParent();
            FeatureStructures chunkFS = chunkNode.getFeatureStructures();
            if (chunkFS != null) {
                FeatureStructure chunkFSS = chunkFS.getAltFSValue(0);
                if (chunkFSS != null) {
                    // Set chunk-level properties on this token (lexical item)
                    String chunkDepRel = chunkFSS.getAttributeValueString("drel"); // Example of transferring chunk property
                    if (chunkDepRel != null) {
                        String[] parts = chunkDepRel.split(":");
                        node.setDeprel(getUDRelation(parts[0]));
//                        node.setDeprel(chunkDepRel); // Set chunk's dependency relation to the token
                    }
                }
            }
        }
    }



    public void readSSFStory(File ssfFile, Charset cs, File conllufile)
    {
        try {
            FSProperties fsp = new FSProperties();
            SSFProperties ssfp = new SSFProperties();

            fsp.read(GlobalProperties.resolveRelativePath("props/fs-mandatory-attribs.txt"),
                    GlobalProperties.resolveRelativePath("props/fs-other-attribs.txt"),
                    GlobalProperties.resolveRelativePath("props/fs-props.txt"),
                    GlobalProperties.resolveRelativePath("props/ps-attribs.txt"),
                    GlobalProperties.resolveRelativePath("props/dep-attribs.txt"),
                    GlobalProperties.resolveRelativePath("props/sem-attribs.txt"),
                    "UTF-8");

            ssfp.read(GlobalProperties.resolveRelativePath("props/ssf-props.txt"), "UTF-8");

            FeatureStructuresImpl.setFSProperties(fsp);
            SSFNode.setSSFProperties(ssfp);

            ssfStory.readFile(ssfFile.getAbsolutePath(), cs.displayName());
//            System.out.println("Sentences read: " + ssfStory.countSentences());
//            System.out.println(ssfStory.convertToPOSTagged("/"));
            Document document = new DefaultDocument();
            int senCount = ssfStory.countSentences();
            // Sentence count
            for(int i = 0; i < senCount; i++) {

                SSFSentence ssfSentence = ssfStory.getSentence(i);
//                System.out.println(ssfSentence.convertToPOSTagged("/"));
                SSFNode rootNode = ssfSentence.getRoot();

                Bundle bundle = document.createBundle();
                Root root = bundle.createTree();

                int chunkCount = rootNode.countChildren();
//                System.out.println(chunkCount);
                List<Node> nodesList = new ArrayList<>();
                List<Integer> headIds = new ArrayList<>();

                for(int j = 0; j < chunkCount; j++) {

                    SSFNode childNode = (SSFNode) rootNode.getChildAt(j);
//                    System.out.println(childNode.convertToPOSTagged("}"));

                    if (childNode instanceof SSFPhrase) {
                        SSFPhrase chunkNode = (SSFPhrase) childNode;
                        FeatureStructures fs1 = chunkNode.getFeatureStructures();
                        FeatureStructure lexFSS1 = fs1.getAltFSValue(0);
                        List<String> featureAttributes1 = chunkNode.getAttributeNames();
//                        for (int l2=0; l2<featureAttributes1.size(); l2++)
//                        {
//                            System.out.print(featureAttributes1.get(l2));
//                            System.out.print(" ");
//                            System.out.println(lexFSS1.getAttributeValueString(featureAttributes1.get(l2)));
//                        }

//                        System.out.println("Check");
                        int lexCount = chunkNode.countChildren();
//                        System.out.println(lexCount);
                        for (int k = 0; k < lexCount; k++) {
                            SSFNode innerChild = (SSFNode) chunkNode.getChildAt(k);
                            if (innerChild instanceof SSFLexItem) {
//                                System.out.println("Check1");
                                SSFLexItem ssfLexItem = (SSFLexItem) innerChild;
                                processLexItem(ssfLexItem, root, nodesList, headIds);
                            }
                        }
                    }
                    else if (childNode instanceof SSFLexItem) {
                        SSFLexItem ssfLexItem = (SSFLexItem) childNode;
                        processLexItem(ssfLexItem, root, nodesList, headIds);
                    }

                }
                for (int n = 0; n < nodesList.size(); n++) {
                    Node node = nodesList.get(n);
                    int headIndex = headIds.get(n);

                    if (headIndex > 0 && headIndex <= nodesList.size()) {
                        node.setParent(nodesList.get(headIndex - 1));
                    }
                }
            }
            CoNLLUWriter conlluWriter = new CoNLLUWriter();
            Path outputpath = Paths.get(conllufile.getAbsolutePath());
            conlluWriter.writeDocument(document,outputpath);

            System.out.println("Conversion completed. CoNLL-U output written to: "
                    + conllufile.getAbsolutePath());
//            System.out.println(ssfStory.makeString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args)
    {
        System.out.println("This class converts linguistically annotated data from SSF to CoLLU format.");
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the folder path containing .ssf files: ");
        String folderPath = scanner.nextLine().trim();
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Provided path is not a valid directory: " + folderPath);
            return;
        }
        // Filter for .ssf files
        File[] ssfFiles = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".ssf");
            }
        });
        if (ssfFiles == null || ssfFiles.length == 0) {
            System.out.println("No .ssf files found in the directory: " + folderPath);
            return;
        }
        // Sort files for consistent progress
        Arrays.sort(ssfFiles);
        int totalFiles = ssfFiles.length;
        SSFToCoNLLUConverter converter = new SSFToCoNLLUConverter();
        for (int i = 0; i < totalFiles; i++) {
            File ssfFile = ssfFiles[i];
            String baseName = ssfFile.getName().substring(0, ssfFile.getName().lastIndexOf('.'));
            File outFile = new File(folder, baseName + ".conllu");
            System.out.printf("[%d/%d] Converting %s ... ", i + 1, totalFiles, ssfFile.getName());
            try {
                converter.readSSFStory(ssfFile, StandardCharsets.UTF_8, outFile);
                System.out.println("Done.");
            } catch (Exception e) {
                System.out.println("Failed: " + e.getMessage());
            }
            // Progress bar
            int progress = (int) (((i + 1) / (double) totalFiles) * 50);
            StringBuilder bar = new StringBuilder("[");
            for (int j = 0; j < 50; j++) {
                if (j < progress) bar.append("#");
                else bar.append("-");
            }
            bar.append("]");
            System.out.println(bar.toString() + String.format(" %d%%", (int) (((i + 1) / (double) totalFiles) * 100)));
        }
        System.out.println("All conversions completed successfully!");
    }

}
