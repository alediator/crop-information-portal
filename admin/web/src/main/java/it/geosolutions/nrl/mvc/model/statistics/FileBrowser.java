package it.geosolutions.nrl.mvc.model.statistics;

import it.geosolutions.nrl.model.JSPFile;
import it.geosolutions.nrl.utils.FileNameComparator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * provides files and directories from a path
 * @author Lorenzo Natali
 * @author Lorenzo Pini
 *
 */
@XmlRootElement
public class FileBrowser {
	private String baseDir="";
	private String regex;
	private Boolean scanDiretories;
	
	
	/**
	 * Utility to provide a list of JSPFile inside the basedir
	 * @param scanDirectories
	 * @return
	 */
	public List<JSPFile> getFiles(){
		File dir = new File(baseDir);
		if( !dir.isDirectory() ) return null;
		File[] children = dir.listFiles();
		List<JSPFile> dirList = new LinkedList<JSPFile>();
		
		if(children == null) {
			return dirList;
		}
		
		List<JSPFile> fileList = new LinkedList<JSPFile>();
		for (int i=0;i<children.length;i++){
			String name = children[i].getName();
			if(regex!=null){
				Pattern pattern = Pattern.compile(regex);
				Matcher match = pattern.matcher(name);
				if(match.matches() && (scanDiretories || !children[i].isDirectory())){
					if(children[i].isDirectory()) {
						dirList.add(new JSPFile(children[i].getPath()));
					}else {
						fileList.add(new JSPFile(children[i].getPath()));						
					}
				}
			}else{
				if((scanDiretories || !children[i].isDirectory())){
					if(children[i].isDirectory()) {
						dirList.add(new JSPFile(children[i].getPath()));
					}else {
						fileList.add(new JSPFile(children[i].getPath()));						
					}

				}
			}
		}
		//sort directories and files
		Collections.sort(dirList, new FileNameComparator());
		Collections.sort(fileList, new FileNameComparator());
		//merge the two sorted lists 
		dirList.addAll(fileList);
		//Sort files by name
		
		return dirList;
	}
	
	//############################################
	// SETTERS AND GETTERS 
	//############################################

	@XmlElement
	public String getBaseDir() {
		return baseDir;
	}
	
	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
		
	}

	/**
	 * @return the scanDiretories
	 */
	public Boolean getScanDiretories() {
		return scanDiretories;
	}

	/**
	 * @param scanDiretories the scanDiretories to set
	 */
	public void setScanDiretories(Boolean scanDiretories) {
		this.scanDiretories = scanDiretories;
	}
	@XmlElement
    public String getRegex() {
        return regex;
    }
    
    public void setRegex(String regex) {
        this.regex = regex;
    }
    
}
