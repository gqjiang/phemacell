package org.projectphema.hqmf.i2b2;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.ClientResponse;

public class VSAC2i2b2Ontology {

	private String vsacAuthURL = "https://vsac.nlm.nih.gov/vsac/ws";
	private String vsacBaseURL ="https://vsac.nlm.nih.gov/vsac/svs";
	
	private String TGT = "TGT-134523-nILS1S1YmOXPiteEFfGEBm5juYuSgZMmvGOY3siFreGtgeV0o1-cas";
	
	private StringBuilder header;
	
	javax.xml.transform.TransformerFactory tFactory;
	public Transformer hqmf, i2b2, ihqmf, i2b2plus;
	public Client httpClient = null;
	public WebResource getTGTicket = null, getServiceTicket = null, getValueSet = null, addChild = null;
	
	// create an empty Model
	private Model model = ModelFactory.createDefaultModel();
	
	public String getTicketGrantingTicket(){
		StringBuffer sb = new StringBuffer();
		httpClient = Client.create();
		getTGTicket = httpClient.resource(vsacAuthURL + "/Ticket");
		
		String input = "";
		
		try{
		       // POST method
	        ClientResponse response = getTGTicket.accept(MediaType.TEXT_XML)
	                .type(MediaType.TEXT_XML).post(ClientResponse.class, input);

	        // check response status code
	        if (response.getStatus() != 200) {
	            throw new RuntimeException("Failed : HTTP error code : "
	                    + response.getStatus());
	        }

	        // display response
	        String output = response.getEntity(String.class);
	        System.out.println("Output from Server .... ");
	        System.out.println(output);		
	        sb.append(output);
			
		} catch(Exception e){
			e.printStackTrace();
		}
		
		
		
	    return sb.toString();
	}
	
	public String getServiceTicket(){
		StringBuffer sb = new StringBuffer();
		httpClient = Client.create();
		getServiceTicket = httpClient.resource(vsacAuthURL + "/Ticket/" + TGT );
		
		String input = "service=http://umlsks.nlm.nih.gov";
		
		try{
		       // POST method
	        ClientResponse response1 = getServiceTicket.accept(MediaType.TEXT_XML)
	                .type(MediaType.TEXT_XML).post(ClientResponse.class, input);

	        // check response status code
	        if (response1.getStatus() != 200) {
	            throw new RuntimeException("Failed : HTTP error code : "
	                    + response1.getStatus());
	        }

	        // display response
	        String output = response1.getEntity(String.class);
	        System.out.println("Output from Server .... ");
	        System.out.println(output);		
	        sb.append(output);
			
		} catch(Exception e){
			e.printStackTrace();
		}
		
		
		
	    return sb.toString();
	}	
	
	
	public String getValueSetByOID(String oid){
		StringBuffer sb = new StringBuffer();
		httpClient = Client.create();

		String input = "id=" + oid + "&ticket=" + getServiceTicket();
		
		String uri = vsacBaseURL + "/RetrieveValueSet?" + input;
		
		System.out.println(uri);
				
		
		try{
		       // GET method
	        ClientResponse response1 = getResponse(httpClient, uri, MediaType.APPLICATION_XML);

	        // check response status code
	        if (response1.getStatus() != 200) {
	            throw new RuntimeException("Failed : HTTP error code : "
	                    + response1.getStatus());
	        }

	        // display response
	        String output = response1.getEntity(String.class);
	        System.out.println("Output from Server .... ");
	        System.out.println(output);		
	        sb.append(output);
			
		} catch(Exception e){
			e.printStackTrace();
		}
		
		
		
	    return sb.toString();
	}
	
	public String convertValueSetFromXMLToTurtle(String oid){
		StringBuffer sb = new StringBuffer();
		String vsContent = getValueSetByOID(oid);

		
		Transformer rdf = null;
    	//Processors p = null;
		//p = Processors.getInstance(); 
		MyProps pr = MyProps.getInstance();
		//ByteArrayOutputStream result = new ByteArrayOutputStream();
		StreamSource source = new StreamSource(pr.xslLoc+"/vsac2i2b2.xsl");
		javax.xml.transform.TransformerFactory transFactory;
		ByteArrayOutputStream ttlresult = new ByteArrayOutputStream();
		StreamSource ttlsource;
		
		try{
	        ttlsource = new StreamSource(new ByteArrayInputStream(vsContent.getBytes()));
			transFactory = javax.xml.transform.TransformerFactory.newInstance();			
		    rdf = transFactory.newTransformer(source);	
		    rdf.transform(ttlsource, new StreamResult(ttlresult));
		    //String vsInRDF = vsacOnt.convertTurtleInRDF(i2b2result.toString());			
			//System.out.println(vsInRDF);
		    System.out.println(ttlresult.toString());
		    
		    sb.append(ttlresult.toString());			
			
		}catch(TransformerException e) { e.printStackTrace(); }  
			
		return sb.toString();
	}
	
	/* This is a utility method to get the ClientResponse from a url */

	private ClientResponse getResponse(Client jerseyClient, String url,
			String mediaType) {
		ClientResponse response = null;
		try {

			/** HTTP GET request **/
			response = jerseyClient.resource(url).accept(mediaType)
					.get(ClientResponse.class);

		} catch (Exception e) {
			e.printStackTrace();
			;
		}
		return response;
	}
	
	public List<String> getConcepts(String ttl){
		
		List<String> concepts = new ArrayList<String>();
        InputStream is = new ByteArrayInputStream(ttl.getBytes());
        model.read(is, null,"TURTLE");

        String queryString = this.getConceptsQuery();
        com.hp.hpl.jena.query.Query query = QueryFactory.create(queryString) ;
        QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
        
       try{  
        ResultSet results = qexec.execSelect();
      	 
        for ( ; results.hasNext() ; )
        {
          QuerySolution soln = results.nextSolution() ;
          Literal code = soln.getLiteral("code") ;   // Get a result variable - must be a literal
          Literal displayName = soln.getLiteral("displayName") ;   // Get a result variable - must be a literal
          Literal codeSystem = soln.getLiteral("codeSystem") ;   // Get a result variable - must be a literal
          Literal codeSystemName = soln.getLiteral("codeSystemName") ;   // Get a result variable - must be a literal
          Literal codeSystemVersion = soln.getLiteral("codeSystemVersion") ;   // Get a result variable - must be a literal
          
         
          concepts.add(code.getString() + "|" + displayName.getString() + "|" + codeSystem.getString() + "|" + codeSystemName.getString() + "|" + codeSystemVersion.getString());
        
        }
      } finally { qexec.close() ; 	
      
      }
		return concepts;
	}

	
	public String getValueset(String ttl){
		
		StringBuffer sb = new StringBuffer();
        InputStream is = new ByteArrayInputStream(ttl.getBytes());
        model.read(is, null,"TURTLE");
		
        String queryString = this.getValueSetQuery();
        com.hp.hpl.jena.query.Query query = QueryFactory.create(queryString) ;
        QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
        
       try{  
        ResultSet results = qexec.execSelect();
      	 
        for ( ; results.hasNext() ; )
        {
          QuerySolution soln = results.nextSolution() ;
          Literal oid = soln.getLiteral("oid") ;   // Get a result variable - must be a literal
          Literal displayName = soln.getLiteral("displayName") ;   // Get a result variable - must be a literal
          Literal version = soln.getLiteral("version") ;   // Get a result variable - must be a literal
          
         
          sb.append(oid.getString() + "|" + displayName.getString() + "|" + version.getString());
        
        }
      } finally { qexec.close() ; 	
      
      }
		return sb.toString();
	}

	public String convertTurtleInRDF(String ttlContent){
		

		//List<String> versions = new ArrayList<String>();		 
	     StringBuffer sb = new StringBuffer();
		 	 
	     
		try {
         
            InputStream is = new ByteArrayInputStream(ttlContent.getBytes());
            model.read(is, null,"TURTLE");
			//model1.write(System.out, "TURTLE");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			//model.write(out, "RDF/XML-ABBREV");
			model.write(out, "RDF/XML-ABBREV");
			
			sb.append(new String(out.toByteArray()));

		}catch(Exception e){
			e.printStackTrace();
		}


		return sb.toString();
	}	
	
	public void generateVSACOntologyInOntologyCell(String oid){
		
		List<String> requests = new ArrayList<String>();

		String vsTurtle = convertValueSetFromXMLToTurtle(oid);
		String vsContent = getValueset(vsTurtle);
		List<String> concepts = getConcepts(vsTurtle);
		
		String[] valuesetItems = vsContent.split("\\|");
		String vsName = valuesetItems[1];
		String vsDimCode = this.getBaseCodeForValueSet(concepts);
		String vsBaseCode = "OID:" + oid;
		//String vsDimCode = "'ICD9:410','ICD9:410.0','ICD9:410.00','ICD9:410.01','ICD9:410.02','ICD9:410.1','ICD9:410.10','ICD9:410.11','ICD9:410.12','ICD9:410.2','ICD9:410.20','ICD9:410.21','ICD9:410.22','ICD9:410.3','ICD9:410.30','ICD9:410.31','ICD9:410.32','ICD9:410.4','ICD9:410.40','ICD9:410.41','ICD9:410.42','ICD9:410.5','ICD9:410.50','ICD9:410.51','ICD9:410.52','ICD9:410.6','ICD9:410.60','ICD9:410.61','ICD9:410.62','ICD9:410.7','ICD9:410.70','ICD9:410.71'";
		
    	String requestVS = generateRequestForAddChildToOntologyCell("i2b2", "Demo", 
    			"i2b2demo", "demouser", true, 
    			"\\\\CUST\\Custom Metadata\\VSAC Valueset\\" + vsBaseCode + "\\" ,
    			"addChild", 2, vsName + "value set", vsBaseCode, "IN", 
    			vsDimCode,
    			"Test Ontology Loading", "Custom Metadata \\ VSAC Valueset \\ " + vsName + " value set");
		
		
		Transformer rdf = null;
    	Processors p = null;
		p = Processors.getInstance(); 
		MyProps pr = MyProps.getInstance();
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		StreamSource source = new StreamSource(pr.xslLoc+"/vsac2i2b2.xsl");
		javax.xml.transform.TransformerFactory transFactory;
		ByteArrayOutputStream i2b2result = new ByteArrayOutputStream();
		StreamSource i2b2source;
		
		try{
			i2b2source = new StreamSource(new ByteArrayInputStream(vsContent.getBytes()));
			transFactory = javax.xml.transform.TransformerFactory.newInstance();			
		    rdf = transFactory.newTransformer(source);	
		    //rdf.transform(i2b2source, new StreamResult(i2b2result));
		    //String vsInRDF = vsacOnt.convertTurtleInRDF(i2b2result.toString());
			
			//System.out.println(vsInRDF);

			//getChildren
/*	    	String request = generateRequestForAddChildToOntologyCell("i2b2", "Demo", 
	    			"i2b2demo", "demouser", true, 
	    			"\\\\CUST\\Custom Metadata\\VSAC Valueset\\Statin RXNORM value set\\Lovastatin\\" ,
	    			"addChild", 3, "Lovastatin", "RXNORM:197903", "IN", 
	    			"\\Custom Metadata\\VSAC Valueset\\Statin RXNORM value set\\Lovastatin\\",
	    			"Test Ontology Loading", "Custom Metadata \\ VSAC Valueset \\ Statin RXNORM value set \\ Lovastatin");
*/

            System.out.println(requestVS);
    		httpClient = Client.create();
    		addChild = httpClient.resource(pr.ontLoc+"/addChild");

            ClientResponse response = addChild.accept(MediaType.APPLICATION_XML,MediaType.TEXT_XML).type(MediaType.TEXT_XML).post(ClientResponse.class, requestVS);
        	String xmlResponse = response.getEntity(String.class);   
        	System.out.println(xmlResponse);
			
			
		}catch(TransformerException e) { e.printStackTrace(); }  
		
			
	}
	
	
	public void generateVSACOntologyInOntologyCellByFile(String fileName){
		
		List<String> requests = new ArrayList<String>();


		String vsDimCode = this.getValueSetBaseCodeFromFile(fileName);

		String vsName = this.valueSetName;
		vsName = vsName.replaceAll(",", "");
		vsName = vsName.replaceAll("\\.", "");
		vsName = vsName + " " + this.valueSetCodeSystem;
		String vsBaseCode = "OID:" + this.valueSetOid;
		//String vsDimCode = "'ICD9:410','ICD9:410.0','ICD9:410.00','ICD9:410.01','ICD9:410.02','ICD9:410.1','ICD9:410.10','ICD9:410.11','ICD9:410.12','ICD9:410.2','ICD9:410.20','ICD9:410.21','ICD9:410.22','ICD9:410.3','ICD9:410.30','ICD9:410.31','ICD9:410.32','ICD9:410.4','ICD9:410.40','ICD9:410.41','ICD9:410.42','ICD9:410.5','ICD9:410.50','ICD9:410.51','ICD9:410.52','ICD9:410.6','ICD9:410.60','ICD9:410.61','ICD9:410.62','ICD9:410.7','ICD9:410.70','ICD9:410.71'";
		
    	String requestVS = generateRequestForAddChildToOntologyCell("i2b2", "Demo", 
    			"i2b2demo", "demouser", true, 
    			"\\\\CUST\\Custom Metadata\\VSAC Valueset\\" + vsBaseCode + "\\" ,
    			"addChild", 2, vsName + " value set", vsBaseCode, "IN", 
    			vsDimCode,
    			"T2DM Valueset Loading", "Custom Metadata \\ VSAC Valueset \\ " + vsName + " value set");
		
		
		Transformer rdf = null;
    	Processors p = null;
		p = Processors.getInstance(); 
		MyProps pr = MyProps.getInstance();
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		StreamSource source = new StreamSource(pr.xslLoc+"/vsac2i2b2.xsl");
		javax.xml.transform.TransformerFactory transFactory;
		ByteArrayOutputStream i2b2result = new ByteArrayOutputStream();
		StreamSource i2b2source;
		
		//try{
			//i2b2source = new StreamSource(new ByteArrayInputStream(vsContent.getBytes()));
			//transFactory = javax.xml.transform.TransformerFactory.newInstance();			
		   // rdf = transFactory.newTransformer(source);	
		    //rdf.transform(i2b2source, new StreamResult(i2b2result));
		    //String vsInRDF = vsacOnt.convertTurtleInRDF(i2b2result.toString());
			
			//System.out.println(vsInRDF);

			//getChildren
/*	    	String request = generateRequestForAddChildToOntologyCell("i2b2", "Demo", 
	    			"i2b2demo", "demouser", true, 
	    			"\\\\CUST\\Custom Metadata\\VSAC Valueset\\Statin RXNORM value set\\Lovastatin\\" ,
	    			"addChild", 3, "Lovastatin", "RXNORM:197903", "IN", 
	    			"\\Custom Metadata\\VSAC Valueset\\Statin RXNORM value set\\Lovastatin\\",
	    			"Test Ontology Loading", "Custom Metadata \\ VSAC Valueset \\ Statin RXNORM value set \\ Lovastatin");
*/

            System.out.println(requestVS);
/*
            httpClient = Client.create();
    		addChild = httpClient.resource(pr.ontLoc+"/addChild");

            ClientResponse response = addChild.accept(MediaType.APPLICATION_XML,MediaType.TEXT_XML).type(MediaType.TEXT_XML).post(ClientResponse.class, requestVS);
        	String xmlResponse = response.getEntity(String.class);   
        	System.out.println(xmlResponse);
*/			
			
		//}catch(TransformerException e) { e.printStackTrace(); }  
		
			
	}	
	
	private String valueSetName = "";
	private String valueSetOid = "";
	private String valueSetCodeSystem = "";
	
	public String getValueSetBaseCodeFromFile(String fileName){
		StringBuffer sb = new StringBuffer();
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(fileName));
			String line = br.readLine();
			String[] vsItems = line.split("\t");
			valueSetName = vsItems[3];
			valueSetOid = vsItems[1];
			valueSetCodeSystem = vsItems[4];
			while(line != null){
				String[] conceptItems = line.split("\t");
				sb.append("'" + conceptItems[8] + "',");
				line = br.readLine();
				
			}
			String codes = sb.toString();
			codes = codes.substring(0, codes.length()-1);
			
			sb = new StringBuffer(codes);
			
			br.close();
		}catch(IOException ie){
			ie.printStackTrace();
		}
		
		
		return sb.toString();
		
	}
	
	public String getBaseCodeForValueSet(List<String> concepts){
		StringBuffer sb = new StringBuffer();
		
		int size = concepts.size();
		
		int index = 0;
		
		for(String concept : concepts){
			String[] conceptItems = concept.split("\\|");
			String code = conceptItems[0];
			if(code.indexOf("ICD9") >= 0){
				code = code.replaceAll("ICD9CM", "ICD9");
				sb.append( "'" + code + "'");	
				
				sb.append(",");
				
			}
						
		}
		
		String codes = sb.toString();
		codes = codes.substring(0, codes.length()-1);
		
		sb = new StringBuffer(codes);
		
		
		System.out.println(sb.toString());
		
		
		return sb.toString();
	}
	
	public String generateRequestForAddChildToOntologyCell(String user, String project, 
			String domain, String password, boolean isToken, String key, String requestType,
			int level, String name, String basecode, String operator, String dimcode,
			String comment, String tooltip) {
		header = new StringBuilder();
		
		header.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		header.append("<ns4:request xmlns:ns2='http://www.i2b2.org/xsd/hive/plugin/' xmlns:ns4='http://www.i2b2.org/xsd/hive/msg/1.1/' xmlns:ns3='http://www.i2b2.org/xsd/cell/crc/psm/1.1/' xmlns:ns5='http://www.i2b2.org/xsd/hive/msg/result/1.1/' xmlns:ns6='http://www.i2b2.org/xsd/cell/ont/1.1/' xmlns:ns7='http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/'>");
		header.append("<message_header><i2b2_version_compatible>1.1</i2b2_version_compatible><sending_application><application_name>i2b2 Ontology</application_name><application_version>1.7</application_version></sending_application><sending_facility><facility_name>i2b2 Hive</facility_name></sending_facility><receiving_application><application_name>Ontology Cell</application_name><application_version>1.7</application_version></receiving_application><receiving_facility><facility_name>i2b2 Hive</facility_name></receiving_facility>");			

		StringBuilder request = new StringBuilder(header);
		request.append("<datetime_of_message>");
		request.append(new Date());
		request.append("</datetime_of_message>");
		request.append("<security>");
		request.append("<domain>");
		request.append(domain);
		request.append("</domain><username>");
		request.append(user);
		request.append("</username><password token_ms_timeout='1800000' is_token=");
		if (isToken) request.append("'true'>"); else request.append("'false'>");
		request.append(password);
		request.append("</password></security><project_id>");
		request.append(project);
		request.append("</project_id></message_header>");
		if (requestType.equals("addChild")) {
			request.append("<message_body><ns6:add_child>");
			request.append("<level>");
			request.append(level);
			request.append("</level>");
			request.append("<key>");
			request.append(key);
			request.append("</key>");
			request.append("<name>");
			request.append(name);
			request.append("</name>");
			request.append("<synonym_cd>N</synonym_cd><visualattributes>LAE</visualattributes><totalnum xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:nil='true' />");
			request.append("<basecode>");
			request.append(basecode);
			request.append("</basecode>");
			request.append("<facttablecolumn>concept_cd</facttablecolumn><tablename>concept_dimension</tablename><columnname>concept_cd</columnname><columndatatype>T</columndatatype>");
			request.append("<operator>");
			request.append(operator);
			request.append("</operator>");
			request.append("<dimcode>");
			request.append(dimcode);
			request.append("</dimcode>");
			request.append("<comment>");
			request.append(comment);
			request.append("</comment>");
			request.append("<tooltip>");
			request.append(tooltip);
			request.append("</tooltip>");
			request.append("<sourcesystem_cd>i2b2_manualentry</sourcesystem_cd><valuetype_cd /></ns6:add_child></message_body>");
			
		} 
		request.append("</ns4:request>");
		//System.out.println(request.toString());
		return request.toString();
	}	
	
	
	private String getConceptsQuery(){
		
		String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
        "PREFIX svs: <urn:ihe:iti:svs:2008> " +
        "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
        "SELECT DISTINCT ?code ?displayName ?codeSystem ?codeSystemName ?codeSystemVersion " +
        "  WHERE { " +        
        "     ?uri  rdf:type svs:Concept . " + 
        "     ?uri  rdfs:label ?displayName . " + 
        "     ?uri  skos:notation ?code . " + 
        "     ?uri  svs:codeSystem ?codeSystem . " + 
        "     ?uri  svs:codeSystemName ?codeSystemName . " + 
        "     ?uri  svs:codeSystemVersion ?codeSystemVersion . " +         
        "  }    " ;
		
		return query;
	}	

	
	private String getValueSetQuery(){
		
		String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
        "PREFIX svs: <urn:ihe:iti:svs:2008> " +
        "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
        "SELECT DISTINCT ?oid ?displayName ?version " +
        "  WHERE { " +        
        "     ?uri  rdf:type svs:ValueSet . " + 
        "     ?uri  rdfs:label ?displayName . " + 
        "     ?uri  skos:notation ?oid . " + 
        "     ?uri  svs:version ?version . " +       
        "  }    " ;
		
		return query;
	}					
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String vsacoid = "2.16.840.1.113883.3.666.5.3011";
		//String vsacoid = "2.16.840.1.113762.1.4.1053.4";
		
		VSAC2i2b2Ontology vsacOnt = new VSAC2i2b2Ontology();
		//vsacOnt.getTicketGrantingTicket();
		//vsacOnt.getServiceTicket();
		//String vsContent = vsacOnt.getValueSetByOID(vsacoid);
		
		//String fileName = "/Users/m005994/Documents/i2b2-webclient/i2b2-T2DM/valueset/t2dm-icd9-valueset.txt";
		
		
		try{
		
			//String vsTurtle = vsacOnt.convertValueSetFromXMLToTurtle(vsacoid);
			//String vsContent = vsacOnt.getValueset(vsTurtle);
			//List<String> concepts = vsacOnt.getConcepts(vsTurtle);
			
			//System.out.println(vsContent + "|" + concepts.size());
			
			vsacOnt.generateVSACOntologyInOntologyCell(vsacoid);
			
			//vsacOnt.generateVSACOntologyInOntologyCellByFile(fileName);
			
		}catch(Exception e) { e.printStackTrace(); }  
		
		
	}

}
