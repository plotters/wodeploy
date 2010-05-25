package com.webobjects.monitor.application;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSLog;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation._NSThreadsafeMutableArray;
import com.webobjects.foundation._NSThreadsafeMutableDictionary;
import com.webobjects.monitor._private.MSiteConfig;
import com.webobjects.monitor.application.WOTaskdHandler.ErrorCollector;

import er.extensions.appserver.ERXSession;

public class Session extends ERXSession implements ErrorCollector {
    private static final long serialVersionUID = 8067267944038698356L;

    public boolean _isLoggedIn;

    public Session() {
        super();
        _isLoggedIn = false;
        return;
    }

    public boolean isLoggedIn() {
        return _isLoggedIn;
    }

    public void setIsLoggedIn(boolean aBOOL) {
        _isLoggedIn = aBOOL;
    }

    protected MSiteConfig siteConfig() {
        return WOTaskdHandler.siteConfig();
    }

    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
        // Check to make sure they have logged in if it is required
        MSiteConfig aMonitorConfig = siteConfig();

        if ((aMonitorConfig == null) || (aMonitorConfig.isPasswordRequired())) {
            if (_isLoggedIn) {
                super.appendToResponse(aResponse, aContext);
            } else {
                if (aContext.page().getClass().getName().equals(Main.class.getName())) {
                    // needs to login on Main page.
                    super.appendToResponse(aResponse, aContext);
                } else {
                    NSLog.err.appendln("Tried to access " + (aContext.page()) + " while not logged in.");
                }
            }
        } else {
            super.appendToResponse(aResponse, aContext);
        }
    }

    /** ******** Error/Informational Messages ********* */
    private _NSThreadsafeMutableArray errorMessageArray = new _NSThreadsafeMutableArray(new NSMutableArray<Object>());

    public void addErrorIfAbsent(String message) {
        errorMessageArray.addObjectIfAbsent(message);
    }

    public String message() {
        String _message = null;
        if (siteConfig() != null) {
            NSArray globalArray = siteConfig().globalErrorDictionary.allValues();
            if ((globalArray != null) && (globalArray.count() > 0)) {
                addObjectsFromArrayIfAbsentToErrorMessageArray(globalArray);
                siteConfig().globalErrorDictionary = new _NSThreadsafeMutableDictionary(
                        new NSMutableDictionary<Object, Object>());
            }
        }
        if (NSLog.debugLoggingAllowedForLevelAndGroups(NSLog.DebugLevelInformational, NSLog.DebugGroupDeployment))
            NSLog.debug.appendln("message(): " + errorMessageArray.array());
        if ((errorMessageArray != null) && (errorMessageArray.count() > 0)) {
            _message = errorMessageArray.componentsJoinedByString(", ");
            errorMessageArray = new _NSThreadsafeMutableArray(new NSMutableArray<Object>());
        }
        return _message;
    }

    public void addObjectsFromArrayIfAbsentToErrorMessageArray(NSArray<String> anArray) {
        if (anArray != null && anArray.count() > 0) {
            int arrayCount = anArray.count();
            for (int i = 0; i < arrayCount; i++) {
                addErrorIfAbsent(anArray.objectAtIndex(i));
            }
        }
    }
}
