package com.tek.systems;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
	 

	@Path("/")
	public class RESTService {
		
		private static final Logger LOG = LoggerFactory.getLogger(RESTService.class);
		private static Map<Integer, Map<String,String>> memoryInCache = new ConcurrentHashMap<Integer, Map<String,String>>();
		private static final String PARTNERID="partner_id";
		private static final String DURATION="duration";
		private static final String AD_CONTENT="ad_content";
		
		//ad campaign using HTTP POST
		@POST
		@Path("/ad")
		@Consumes(MediaType.APPLICATION_JSON)
		public Response addCompaign(InputStream incomingData) {
			StringBuilder crunchifyBuilder = new StringBuilder();
			Map<String,String> map=new HashMap<String, String>();
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(incomingData));
				String line = null;
				while ((line = in.readLine()) != null) {
					crunchifyBuilder.append(line);
				}
				JSONObject json = new JSONObject(crunchifyBuilder.toString());
				if(memoryInCache.size()!=0 && memoryInCache.containsKey(json.getInt("partner_id"))){
					return Response.status(400).entity("Partner Id should be unique").build();	
				}
				
				else{
				long duration=Long.valueOf(json.getString(DURATION));
				long seconds = (new Date().getTime())/1000+duration;
				map.put(PARTNERID, json.getString(PARTNERID));
				map.put(DURATION, String.valueOf(seconds));
				map.put(AD_CONTENT, json.getString(AD_CONTENT));
				memoryInCache.put(json.getInt(PARTNERID), map);
				}
			} 
			catch (NumberFormatException nfe) {
				LOG.error("Exception occured is :"+nfe.getLocalizedMessage());
				return Response.status(400).entity("Invalid duration units").build();	
			}
			catch(JSONException je){
				LOG.error("Exception occured is :"+je.getLocalizedMessage());
			}
			catch (Exception e) {
				LOG.error("Exception occured is :"+e.getLocalizedMessage());
			}
			LOG.info("Data Received: " + crunchifyBuilder.toString());
	 
			// return HTTP response 200 in case of success
			return Response.status(200).entity(crunchifyBuilder.toString()).build();
		}
	 
	
		//fetch ad campaign for a partner
		@GET
		@Path("/ad/{partnerId}")
		@Produces(MediaType.TEXT_PLAIN)
		public Response getCampaign(@PathParam("partnerId") String partnerId) {
			JSONObject json = null;
			Map<String, String> map = null;
			long currentTimeInSeconds = (new Date().getTime())/1000;
			long adComapignDurationTime = 0;
			try{
				int id=Integer.parseInt(partnerId);
				if(memoryInCache.containsKey(id)){
					map=memoryInCache.get(id);
					json=convertMapToJson(map);
					adComapignDurationTime=Long.valueOf(json.getString(DURATION));
					if(currentTimeInSeconds>adComapignDurationTime){
						return Response.status(400).entity("no active ad campaigns exist for this partnerId").build();
					}
				}else{
					
					return Response.status(400).entity("Invalid Partner Id").build();
				}
			}
			catch(JSONException je){
				LOG.error("Exception occured is :"+je.getLocalizedMessage());
			}
			catch (Exception e) {
				LOG.error("Exception occured is :"+e.getLocalizedMessage());
			}
			// return HTTP response 200 in case of success
			return Response.status(200).entity("Campaign saved successfully.").build();
		}
		
		
		//all the ad campaigns
		@GET
		@Path("/ad/all")
		@Produces(MediaType.TEXT_PLAIN)
		public Response getAllCampaigns() {
			List<JSONObject> list=new ArrayList<JSONObject>();
			JSONObject json = null;
			try{
				for(Map<String,String> map:memoryInCache.values()){
					json=convertMapToJson(map);
					list.add(json);
				}
			}
			catch(JSONException je){
				LOG.error("Exception occured is :"+je.getLocalizedMessage());
			}
			catch (Exception e) {
				LOG.error("Exception occured is :"+e.getLocalizedMessage());
			}
			return Response.status(200).entity(list.toString()).build();
		}
		
		private JSONObject convertMapToJson(Map<String,String> map) throws JSONException{
			
			JSONObject json=new JSONObject();
			json.put(PARTNERID, map.get(PARTNERID));
			json.put(DURATION, map.get(DURATION));
			json.put(AD_CONTENT, map.get(AD_CONTENT));
			
			return json;
		}
		
}
