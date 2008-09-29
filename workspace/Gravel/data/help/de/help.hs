<?xml version='1.0' encoding='ISO-8859-1' ?>
<!DOCTYPE helpset
  PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 1.0//EN"
         "http://java.sun.com/products/javahelp/helpset_1_0.dtd">

<?TestTarget this is data for the test target ?>

<helpset version="1.0">

  <!-- title -->
  <title>Gravel - Hilfe</title>

  <!-- maps -->
  <maps>
     <homeID>intro</homeID>
     <mapref location="content.jhm"/>
  </maps>

  <!-- views -->
  <view>
    <name>TOC</name>
    <label>Inhaltsverzeichnis</label>
    <type>javax.help.TOCView</type>
    <data>toc.xml</data>
  </view>

  <view>
    <name>Index</name>
    <label>alphabetischer Index</label>
    <type>javax.help.IndexView</type>
    <data>index.xml</data>
  </view>

<!--	<view>
	  <name>Suche</name>
	  <label>Search</label>
	  <type>javax.help.SearchView</type>
	  <data engine="com.sun.java.help.search.DefaultSearchEngine">
	    MasterSearchIndex
	  </data>
	</view> -->
	
	<presentation default="true">
	    <name>Main_Window</name>
	    <size width="800" height="480" />
	    <location x="0" y="0" />
	    <title>Gravel - Hilfe</title>
			<toolbar> 
					<helpaction>javax.help.BackAction</helpaction>
					<helpaction>javax.help.ForwardAction</helpaction>
					<helpaction>javax.help.HomeAction</helpaction>
					<helpaction>javax.help.SeparatorAction</helpaction>
					<helpaction>javax.help.PrintAction</helpaction>
					<helpaction>javax.help.PrintSetupAction</helpaction>			
			</toolbar>
	</presentation>	

</helpset>
