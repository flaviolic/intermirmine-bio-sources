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

import java.io.Reader;

/** Flavio */
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;


import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;


/**
 * Read miRBase file .tsv exported from miRBase.xls (only human)
 * @author Flavio Licciulli
 */

public class MiRBaseConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "miRBase Database";
    private static final String DATA_SOURCE_NAME = "miRBase";
    private static final String TAXON_ID = "9606";
    private Map<String, String> matureMirnas = new HashMap<String, String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public MiRBaseConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        // skip header
        lineIter.next();

        // each gene is on a new line
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            // primary transcript columns
            String mirnaPriAccession = line[0];
            String mirnaPriID = line[1];
            String mirnaPriSequence = line[3];
            // 5p columns
            String mirna5pAccession = line[4];
            String mirna5pID = line[5];
            String mirna5pSequence = line[6];
            //3p columns
            String mirna3pAccession = line[7];
            String mirna3pID = line[8];
            String mirna3pSequence = line[9];

            // create primary transcript Item
            Item mirnaPrimaryTranscript = createItem("MirnaPrimaryTranscript");

            Item mirna5p = null;
            Item mirna3p = null;
            List<String> mirnaList = new ArrayList<>();

            // store 5p
            if (StringUtils.isNotEmpty(mirna5pID)) {
                // checks if 5p is present
                String mirnaInternalID = matureMirnas.get(mirna5pID);
                if (mirnaInternalID  == null) {

                    mirna5p = createItem("MatureMirna");
                    mirna5p.setAttribute("primaryIdentifier", mirna5pID);
                    mirna5p.setAttribute("secondaryIdentifier", mirna5pAccession);
                    mirna5p.setReference("organism", getOrganism(TAXON_ID));
                    mirna5p.setAttribute("mirnaSequence", mirna5pSequence);
                    mirna5p.setReference("mirnaPrimary", mirnaPrimaryTranscript);
                    store(mirna5p);
                    mirnaInternalID = mirna5p.getIdentifier();
                    matureMirnas.put(mirna5pID, mirnaInternalID);
                }

                mirnaList.add(mirnaInternalID);
            }

            // store 3p
            if (StringUtils.isNotEmpty(mirna3pID)) {
                // checks if 3p is present
                String mirnaInternalID = matureMirnas.get(mirna3pID);
                if (mirnaInternalID  == null) {
                    mirna3p = createItem("MatureMirna");
                    mirna3p.setAttribute("primaryIdentifier", mirna3pID);
                    mirna3p.setAttribute("secondaryIdentifier", mirna3pAccession);
                    mirna3p.setReference("organism", getOrganism(TAXON_ID));
                    mirna3p.setAttribute("mirnaSequence", mirna3pSequence);
                    mirna3p.setReference("mirnaPrimary", mirnaPrimaryTranscript);
                    store(mirna3p);
                    mirnaInternalID = mirna3p.getIdentifier();
                    matureMirnas.put(mirna3pID, mirnaInternalID);

                }

                mirnaList.add(mirnaInternalID);
            }

            //store primary transcripts
            mirnaPrimaryTranscript.setAttribute("primaryIdentifier", mirnaPriID);
            mirnaPrimaryTranscript.setAttribute("secondaryIdentifier", mirnaPriAccession);
            mirnaPrimaryTranscript.setAttribute("mirnaPriSequence", mirnaPriSequence);
            mirnaPrimaryTranscript.setReference("organism", getOrganism(TAXON_ID));

            //add mature mirnas 5p and 3p to Collection
            mirnaPrimaryTranscript.setCollection("mirnas", mirnaList);

            //store primaryTranscript Item
            store(mirnaPrimaryTranscript);


        }
    }
}
