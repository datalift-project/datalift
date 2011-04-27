package org.datalift.core.project;

import au.com.bytecode.opencsv.CSVReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import javax.persistence.Entity;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;


@Entity
@RdfsClass("datalift:csvSource")
public class CsvSource extends FileSource implements Iterable<String[]>
{
    private List<String[]> grid;

    private Collection<String> header = new ArrayList<String>();

    @RdfProperty("datalift:separator")
    private String separator;
    @RdfProperty("datalift:titleRow")
    private boolean titleRow = false;

    public boolean hasTitleRow() {
        return titleRow;
    }

    public void setTitleRow(boolean titleRow) {
        this.titleRow = titleRow;
    }

    public void init(String storagePath) {
    	try {
			super.init(storagePath);
		} 
    	catch (FileNotFoundException e1) {
			throw new RuntimeException();
		}
        if (getUrl() != null)
        {
            CSVReader reader = null;
            try {
                reader = new CSVReader(this.getReader(), 
                		Separator.valueOf(separator).getValue());
                grid = reader.readAll();
            }
            catch (IOException e) {
                throw new RuntimeException();
            }
            Iterator<String[]> it = grid.iterator();
            if (!titleRow && it.hasNext()) {
                Iterator<String[]> bis = grid.iterator();
                String firstRow[] = (String[])bis.next();
                for (int i = 0; i < firstRow.length; i++) {
                    header.add(getRowName(i));
                }
            } 
            else if (it.hasNext()) {
                String firstRow[] = (String[])it.next();
                for (int i = 0; i < firstRow.length; i++) {
                    header.add(firstRow[i]);
                }
            }
        }
    }

    public static String getRowName(int n) {
        String s = "";
        for (; n >= 0; n = n / 26 - 1)
        {
            s = (new StringBuilder()).append((char)(n % 26 + 65)).append(s).toString();
        }
        return s;
    }

    public Iterator<String[]> iterator() {
    	Iterator<String[]> i = Collections.unmodifiableList(grid).iterator();
    	if ((this.titleRow) && (i.hasNext())) {
    		// Skip title row.
    		i.next();
    	}
    	return i;
    }

    public int getColumnsSize() {
    	Iterator<String[]> it = grid.iterator();
        if (it.hasNext())
        {
            return ((String[])it.next()).length;
        } else
        {
            return 0;
        }
    }

    public Collection<String> getColumnsHeader() {
        return header;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getSeparator() {
        return separator;
    }
    
    public enum Separator {
        comma(','), semicolon(';'), tab('\t');

        protected final char value;

        Separator(char s) {
            this.value = s;
        }

        public char getValue() {
            return value;
        }
    }
}
