package com.sctek.smartglasses.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class XmlContentHandler extends DefaultHandler {
	
	private final static String TAG = "XmlContentHandler";
	private final static String IMAGE_PREFIX = "http://%s/data/GlassData/photos/";
	private final static String VEDIO_PREFIX = "http://%s/data/GlassData/videos/";
	
	private String nodeName;
	private ArrayList<MediaData> mediaList;
	private String urlPrefix;
	private String ip;
	
	public XmlContentHandler(String ip) {
		
		this.ip = ip;
		
		mediaList = new ArrayList<MediaData>();
		urlPrefix = String.format(IMAGE_PREFIX, ip);
		
	}
	
	@Override
	public void startDocument() throws SAXException {
		// TODO Auto-generated method stub
		super.startDocument();
		
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		// TODO Auto-generated method stub
		nodeName = localName;
		if("videos".equals(nodeName))
			urlPrefix = String.format(VEDIO_PREFIX, ip);
		super.startElement(uri, localName, qName, attributes);
	}
	
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		// TODO Auto-generated method stub
		super.endElement(uri, localName, qName);
	}
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		// TODO Auto-generated method stub
		
		if("name".equals(nodeName)) {
			String name = new String(ch, start, length);
			Log.e(TAG, name);
			if(!"\n".equals(name)) {
				MediaData md = new MediaData();
				
				md.setName(name);
				md.setUrl(urlPrefix + name); 
				mediaList.add(md);
			}
			
		}
		super.characters(ch, start, length);
	}
	
	@Override
	public void endDocument() throws SAXException {
		// TODO Auto-generated method stub
		super.endDocument();
	}
	
	public ArrayList<MediaData> getMedias() {
		Log.e(TAG, "========================media count:" + mediaList.size());
		return mediaList;
	}

}
