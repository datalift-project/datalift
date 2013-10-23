package org.datalift.interlinker.sparql;

import java.util.List;

public abstract class SparqlCursor {
	/** The index of selected item of the cursor*/
	private int position=-1;
	protected List<String[]> content;
	protected String[] columns;
	
	/**
	 * Move the cursor to a specific position
	 * @param newPosition the new position
	 */
	public void moveToPosition(int newPosition){
		if(newPosition<0){
			throw new IllegalArgumentException();
		}
		this.position = newPosition;
	}
	
	public void moveToNextPosition(){
		if(this.position>=content.size()){
			throw new ArrayIndexOutOfBoundsException();
		}
		this.position++;
	}
	
	public void moveToFirstPosition(){
		if(content==null){
			throw new IllegalStateException();
		}
		this.position = 0;
	}
	
	
	public void moveToLastPosition(){
		if(content==null){
			throw new IllegalStateException();
		}
		this.position = content.size()-1;
	}
	
	/**
	 * Get the current position of the cursor
	 * @return cursor's position
	 */
	public int getCurrentPosition(){
		return position;
	}
	
	public String getValue(String columnName){
		int colIndex = getColumnIndex(columnName);
		return getValue(colIndex);
	}
	
	public String getValue(int columnIndex){
		if(columnIndex<0){
			throw new IllegalArgumentException();
		}
		return content.get(position)[columnIndex];
	}
	
	public int getColumnIndex(String columnName){
		for(int i=0;i<columns.length;i++){
			if(columns[i].equalsIgnoreCase(columnName)){
				return i;
			}
		}
		return -1;
	}
	
	
	
	/**
	 * Returns the numbers of rows in the cursor.
	 */
	public int getCount(){
		if(content==null){
			throw new IllegalStateException();
		}
		return content.size();
	}
	
}
