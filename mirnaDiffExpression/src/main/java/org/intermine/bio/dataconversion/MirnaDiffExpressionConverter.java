package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2019 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * Read ncRNA differential expression from different files (comparison)
 * ncarena output
 *
 * @author Flavio Licciulli
 */

public class MirnaDiffExpressionConverter extends BioDirectoryConverter
{
    //
    private static final String DATASET_TITLE = "PedMS miRNA diff expression";
    private static final String DATA_SOURCE_NAME = "miRNADiffExpression";

    private static final String TAXON_ID = "9606";
    private static String filePrefix = "";

    private Properties conditions = new Properties();
    private String conditionFilePath = null;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public MirnaDiffExpressionConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    public void setMappingfilename (String mappingFileName) {

        conditionFilePath = mappingFileName;

    }

    /**
     * 
     *
     * {@inheritDoc}
     */

    @Override
    public void process(File dataDir) throws Exception {
        InputStream is = new FileInputStream(new File(conditionFilePath));
        conditions.load(is);
        System.out.println("conditions loaded");
        System.out.println("conditions size" + conditions.size());

        List<File> files = readFilesInDir(dataDir);

        for (File f : files) {
            filePrefix = f.getName();
            processFile(new FileReader(f));
        }
    }

    private List<File> readFilesInDir(File dir) {
        List<File> files = new ArrayList<File>();
        for (File file : dir.listFiles()) {
            files.add(file);
        }
        return files;
    }

    private void processFile(Reader reader) throws Exception {

        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        // skip header
        lineIter.next();

        // each gene is on a new line
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line.length >= 7) {
                // primary transcript columns
                String ncrnaAccession = line[0];
        //            String mirnaPriID = line[1];
                String log2FC = line[1];
                String pValue = line[2];
                String adjpValue = line[4];
                String tgwDisp = line[5];
                String upDown = line[6];

                if (StringUtils.contains(ncrnaAccession, "MIMAT")) {
    //            System.out.println();
                // create mirna diff item
                    Item mirnaDiffExpression = createItem("MirnaDiffExpression");

                    // get miRNA internal ID from ACC
                    mirnaDiffExpression.setReference("matureMirna", getmirnaReferenceFromAcc(ncrnaAccession));
                    mirnaDiffExpression.setAttribute("log2FC", log2FC);
                    mirnaDiffExpression.setAttribute("pValue", pValue);
                    mirnaDiffExpression.setAttribute("adjpValue", adjpValue);
                    mirnaDiffExpression.setAttribute("tgwDisp", tgwDisp);
                    mirnaDiffExpression.setAttribute("upDown", upDown);
                    mirnaDiffExpression.setAttribute("condition",((String) conditions.get(filePrefix)));
                    store(mirnaDiffExpression);
                }
            }
        }
    }

    public String getmirnaReferenceFromAcc(String mirnaAccession) {
        Item mirna = createItem("MatureMirna");
        mirna.setAttribute("secondaryIdentifier", mirnaAccession);
        mirna.setReference("organism", getOrganism(TAXON_ID));
        try {
            store(mirna);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("failed to store mirna with Accesion: " + mirnaAccession, e);
        }
        return mirna.getIdentifier();
    }

}
