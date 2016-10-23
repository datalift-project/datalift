package org.datalift.ows.model;

import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
/**
 * A class containing the metadata of a SOS observation 
 * @author Hanane Eljabiri
 *
 */
public class ObservationMetaData {
	private String identifier;
	private String name;
	private String description;
	private XMLGregorianCalendar phonomenonTimeBegin;
	private XMLGregorianCalendar phonomenonTimeEnd;
	private List<String> vailableFormat;
	
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public XMLGregorianCalendar getPhonomenonTimeBegin() {
		return phonomenonTimeBegin;
	}
	public void setPhonomenonTimeBegin(XMLGregorianCalendar phonomenonTimeBegion) {
		this.phonomenonTimeBegin = phonomenonTimeBegion;
	}
	public XMLGregorianCalendar getPhonomenonTimeEnd() {
		return phonomenonTimeEnd;
	}
	public void setPhonomenonTimeEnd(XMLGregorianCalendar phonomenonTimeEnd) {
		this.phonomenonTimeEnd = phonomenonTimeEnd;
	}
	public List<String> getVailableFormat() {
		return vailableFormat;		
	}
	public void setVailableFormat(List<String> vailableFormat) {
		this.vailableFormat = vailableFormat;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
