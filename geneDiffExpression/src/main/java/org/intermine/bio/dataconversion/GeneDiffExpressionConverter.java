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

        import org.intermine.util.FormattedTextParser;
        import org.intermine.dataconversion.ItemWriter;
        import org.intermine.metadata.Model;
        import org.intermine.xml.full.Item;



/**
 * Read gene differential expression from different files (comparison)
 * ncarena output
 *
 * @author Flavio Licciulli
 */

public class GeneDiffExpressionConverter extends BioDirectoryConverter
{
    //
    private static final String DATASET_TITLE = "PedMS gene diff expression";
    private static final String DATA_SOURCE_NAME = "geneDiffExpression";

    private static final String TAXON_ID = "9606";
    private static String filePrefix = "";
    //
    private Properties conditions = new Properties();
    private String conditionFilePath = null;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public GeneDiffExpressionConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    public void setMappingfilename (String mappingFileName) {
        // read properties file name
        conditionFilePath = mappingFileName;

    }

    /**
     *
     *
     * {@inheritDoc}
     */
    public void process(File dataDir) throws Exception {
        //reads the mapping file and store condition values
        InputStream is = new FileInputStream(new File(conditionFilePath));
        conditions.load(is);
        //reads the files to load in dir
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

        // each gene expression is on a new line
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line.length >= 7) {
                // primary transcript columns
                String ensemblID = line[0];
                Double baseMean = Double.parseDouble(line[1]);
                if (baseMean > 0 ) {
                    Double log2FC = Double.parseDouble(line[2]);
                    Double pValue = Double.parseDouble(line[5]);
                    Double adjpValue = Double.parseDouble(line[6]);

                    // create gene diff item
                    Item geneDiffExpression = createItem("GeneDiffExpression");

                    // get gene internal ID from EnsemblID
                    //geneDiffExpression.setReference("gene", ___getensemblID(ensemblID));
                    geneDiffExpression.setAttribute("ensemblID", ensemblID);
                    geneDiffExpression.setAttribute("baseMean", String.valueOf(baseMean));
                    geneDiffExpression.setAttribute("log2FC", String.valueOf(log2FC));
                    geneDiffExpression.setAttribute("pValue", String.valueOf(pValue));
                    geneDiffExpression.setAttribute("adjpValue", String.valueOf(adjpValue));
                    geneDiffExpression.setAttribute("condition",((String) conditions.get(filePrefix)));
                    store(geneDiffExpression);
                }
            }
        }
    }

}
