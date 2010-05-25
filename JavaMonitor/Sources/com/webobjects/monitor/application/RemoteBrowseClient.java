package com.webobjects.monitor.application;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOHTTPConnection;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.appserver.xml.WOXMLException;
import com.webobjects.appserver.xml._JavaMonitorDecoder;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSLog;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.monitor._private.MHost;
import com.webobjects.monitor._private.MObject;
import com.webobjects.monitor._private.MonitorException;

public class RemoteBrowseClient extends MonitorComponent {
    private static final long serialVersionUID = 3929193699509459110L;

    static private byte[] evilHack = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>".getBytes();

    public RemoteBrowseClient(WOContext aWocontext) {
        super(aWocontext);
    }

    static public NSDictionary _getFileListOutOfResponse(WOResponse aResponse, String thePath) throws MonitorException {
        NSData responseContent = aResponse.content();
        NSArray anArray = NSArray.EmptyArray;
        if (responseContent != null) {
            byte[] responseContentBytes = responseContent.bytes();
            String responseContentString = new String(responseContentBytes);
            if (responseContentString.startsWith("ERROR")) {
                throw new MonitorException("Path " + thePath + " does not exist");
            } else {
                _JavaMonitorDecoder aDecoder = new _JavaMonitorDecoder();
                try {
                    byte[] evilHackCombined = new byte[responseContentBytes.length + evilHack.length];
                    // System.arraycopy(src, src_pos, dst, dst_pos, length);
                    System.arraycopy(evilHack, 0, evilHackCombined, 0, evilHack.length);
                    System.arraycopy(responseContentBytes, 0, evilHackCombined, evilHack.length,
                            responseContentBytes.length);
                    anArray = (NSArray) aDecoder.decodeRootObject(new NSData(evilHackCombined));
                } catch (WOXMLException wxe) {
                    NSLog.err.appendln("RemoteBrowseClient _getFileListOutOfResponse Error decoding response: "
                            + responseContentString);
                    throw new MonitorException("Host returned bad response for path " + thePath);
                }
            }
        } else {
            NSLog.err.appendln("RemoteBrowseClient _getFileListOutOfResponse Error decoding null response");
            throw new MonitorException("Host returned null response for path " + thePath);
        }

        String isRoots = (String) aResponse.headerForKey("isRoots");
        String filepath = (String) aResponse.headerForKey("filepath");

        NSMutableDictionary aDict = new NSMutableDictionary();
        aDict.takeValueForKey(isRoots, "isRoots");
        aDict.takeValueForKey(filepath, "filepath");
        aDict.takeValueForKey(anArray, "fileArray");

        return aDict;
    }

    private static String getPathString = "/cgi-bin/WebObjects/wotaskd.woa/wa/RemoteBrowse/getPath";

    static public NSDictionary fileListForStartingPathHost(String aString, MHost aHost, boolean showFiles)
            throws MonitorException {
        if (NSLog.debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelDetailed, NSLog.DebugGroupDeployment))
            NSLog.debug.appendln("!@#$!@#$ fileListForStartingPathHost creates a WOHTTPConnection");
        NSDictionary aFileListDictionary = null;
        try {
            Application theApplication = (Application) WOApplication.application();
            WOHTTPConnection anHTTPConnection = new WOHTTPConnection(aHost.name(), theApplication
                    .lifebeatDestinationPort());
            NSMutableDictionary<String, NSMutableArray<String>> aHeadersDict = (NSMutableDictionary<String, NSMutableArray<String>>) WOTaskdHandler
                    .siteConfig().passwordDictionary().mutableClone();
            WORequest aRequest = null;
            WOResponse aResponse = null;
            boolean requestSucceeded = false;
            aHeadersDict.setObjectForKey(new NSMutableArray<String>(aString != null && aString.length() > 0 ? aString
                    : "/Library/WebObjects"), "filepath");
            if (showFiles) {
                aHeadersDict.setObjectForKey(new NSMutableArray<String>("YES"), "showFiles");
            }

            aRequest = new WORequest(MObject._GET, RemoteBrowseClient.getPathString, MObject._HTTP1, aHeadersDict,
                    null, null);
            anHTTPConnection.setReceiveTimeout(5000);

            requestSucceeded = anHTTPConnection.sendRequest(aRequest);

            if (requestSucceeded) {
                aResponse = anHTTPConnection.readResponse();
            }

            if ((aResponse == null) || (!requestSucceeded) || (aResponse.status() != 200)) {
                throw new MonitorException("Error requesting directory listing for " + aString + " from "
                        + aHost.name());
            } else {
                try {
                    aFileListDictionary = _getFileListOutOfResponse(aResponse, aString);
                } catch (MonitorException me) {
                    if (NSLog
                            .debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelCritical, NSLog.DebugGroupDeployment))
                        NSLog.debug.appendln("caught exception: " + me);
                    throw me;
                }
            }
            aHost.isAvailable = true;
        } catch (MonitorException me) {
            aHost.isAvailable = true;
            throw me;
        } catch (Exception localException) {
            aHost.isAvailable = false;
            NSLog.err.appendln("Exception requesting directory listing: ");
            localException.printStackTrace();
            throw new MonitorException("Exception requesting directory listing for " + aString + " from "
                    + aHost.name() + ": " + localException.toString());
        }
        return aFileListDictionary;
    }
}
