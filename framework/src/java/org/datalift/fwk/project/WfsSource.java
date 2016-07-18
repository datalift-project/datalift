package org.datalift.fwk.project;

public interface WfsSource extends ServiceSource {

	
	/**
	 * Supported server types.
	 
	public enum serverType {
		auto("auto"), geoserver("geoserver"),mapserver("mapserver");

		protected final String value;

		serverType(String s) {
			this.value = s;
		}

		public String getValue() {
			return value;
		}
	}*/

	//-------------------------------------------------------------------------
		// Methods
		//-------------------------------------------------------------------------
	
	
	
	public boolean isCompliantInspire();
	
	public void setCompliantInspire();
	
	
}
