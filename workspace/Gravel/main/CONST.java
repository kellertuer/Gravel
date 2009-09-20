package main;
/**
 * Contains some global Constants for example special chars of german language and the version number 
 *
 * @author Ronny Bergmann
 */
public class CONST {

	public final static String version = "0.4";
	public final static String lastchanged = "20. September 2009";
	public final static String lastchangedshort = "2009-09-20";
	
	public final static String html_ae = "&auml;";
	public final static String html_oe = "&ouml;";
	public final static String html_ue = "&uuml;";

	public final static String html_Ae = "&Auml;";
	public final static String html_Oe = "&Ouml;";
	public final static String html_Ue = "&Uuml;";

	public final static String html_sz = "&szlig;";

	public final static String html_times = "&times;";

	public final static String utf8_ae = "\u00E4";
	public final static String utf8_oe = "\u00F6";
	public final static String utf8_ue = "\u00FC";

	public final static String utf8_Ae = "\u00C4";
	public final static String utf8_Oe = "\uC3D6";
	public final static String utf8_Ue = "\uC39C";

	public final static String utf8_sz = "\uC39F";
	
	public static String encodetoHTML(String s)
	{
		String reg = s;
		reg = reg.replaceAll("&","&amp;");
		reg = reg.replaceAll("<","&lt;");
		reg = reg.replaceAll(">","&gt;");
		reg = reg.replaceAll("\n","<br>");
		return reg;
	}
}
