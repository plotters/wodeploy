package com.webobjects.monitor.application;

/*
 � Copyright 2006- 2007 Apple Computer, Inc. All rights reserved.

 IMPORTANT:  This Apple software is supplied to you by Apple Computer, Inc. (�Apple�) in consideration of your agreement to the following terms, and your use, installation, modification or redistribution of this Apple software constitutes acceptance of these terms.  If you do not agree with these terms, please do not use, install, modify or redistribute this Apple software.

 In consideration of your agreement to abide by the following terms, and subject to these terms, Apple grants you a personal, non-exclusive license, under Apple�s copyrights in this original Apple software (the �Apple Software�), to use, reproduce, modify and redistribute the Apple Software, with or without modifications, in source and/or binary forms; provided that if you redistribute the Apple Software in its entirety and without modifications, you must retain this notice and the following text and disclaimers in all such redistributions of the Apple Software.  Neither the name, trademarks, service marks or logos of Apple Computer, Inc. may be used to endorse or promote products derived from the Apple Software without specific prior written permission from Apple.  Except as expressly stated in this notice, no other rights or licenses, express or implied, are granted by Apple herein, including but not limited to any patent rights that may be infringed by your derivative works or by other works in which the Apple Software may be incorporated.

 The Apple Software is provided by Apple on an "AS IS" basis.  APPLE MAKES NO WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION THE IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, REGARDING THE APPLE SOFTWARE OR ITS USE AND OPERATION ALONE OR IN COMBINATION WITH YOUR PRODUCTS. 

 IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION, MODIFICATION AND/OR DISTRIBUTION OF THE APPLE SOFTWARE, HOWEVER CAUSED AND WHETHER UNDER THEORY OF CONTRACT, TORT (INCLUDING NEGLIGENCE), STRICT LIABILITY OR OTHERWISE, EVEN IF APPLE HAS BEEN  ADVISED OF THE POSSIBILITY OF 
 SUCH DAMAGE.
 */
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.monitor._private.MApplication;
import com.webobjects.monitor._private.String_Extensions;

/**
 * List of applications.
 */

public class ApplicationsPage extends MonitorComponent {

	private static final long serialVersionUID = -2523319756655905750L;

	/**
	 * UI: Application currently being iterated over.
	 */
	public MApplication currentApplication;

	/**
	 * UI: New application name
	 */
	public String newApplicationName;

	public ApplicationsPage( WOContext aWocontext ) {
		super( aWocontext );
		handler().updateForPage( name() );
	}

	/**
	 * @return All applications currently being
	 */
	public NSArray<MApplication> applications() {
		NSMutableArray<MApplication> result = new NSMutableArray<MApplication>();
		result.addObjectsFromArray( mySession().siteConfig().applicationArray() );
		EOSortOrdering order = new EOSortOrdering( "name", EOSortOrdering.CompareAscending );
		EOSortOrdering.sortArrayUsingKeyOrderArray( result, new NSArray( order ) );
		return result;
	}

	/**
	 * @return an URL to the application.
	 */
	public String hrefToApp() {
		String aURL = siteConfig().woAdaptor();
		if( aURL != null ) {
			aURL = aURL + "/" + currentApplication.name();
		}
		return aURL;
	}

	/**
	 * Inspect the current application.
	 */
	public WOComponent appDetailsClicked() {
		return AppDetailPage.create( context(), currentApplication );
	}

	/**
	 * Add an application 
	 */
	public WOComponent addApplicationClicked() {
		if( String_Extensions.isValidXMLString( newApplicationName ) ) {
			handler().startReading();
			try {
				if( siteConfig().applicationWithName( newApplicationName ) == null ) {
					MApplication newApplication = new MApplication( newApplicationName, siteConfig() );
					siteConfig().addApplication_M( newApplication );

					if( siteConfig().hostArray().count() != 0 ) {
						handler().sendAddApplicationToWotaskds( newApplication, siteConfig().hostArray() );
					}

					AppConfigurePage aPage = (AppConfigurePage)AppConfigurePage.create( context(), newApplication );
					aPage.isNewInstanceSectionVisible = true;

					// endReading in the finally block below
					return aPage;
				}
			}
			finally {
				handler().endReading();
			}
		}
		newApplicationName = null;
		return pageWithName( ApplicationsPage.class );
	}

	/**
	 * Delete an application. 
	 */
	public WOComponent deleteClicked() {

		final MApplication application = currentApplication;

		return ConfirmationPage.create( context(), new ConfirmationPage.Delegate() {

			public WOComponent cancel() {
				return pageWithName( ApplicationsPage.class );
			}

			public WOComponent confirm() {
				handler().startWriting();
				try {
					siteConfig().removeApplication_M( application );

					if( siteConfig().hostArray().count() != 0 ) {
						handler().sendRemoveApplicationToWotaskds( application, siteConfig().hostArray() );
					}
				}
				finally {
					handler().endWriting();
				}

				return pageWithName( ApplicationsPage.class );
			}

			public String explaination() {
				return "Selecting 'Yes' will shutdown any running instances of this application, delete all instance configurations, and remove this application from the Application page.";
			}

			public int pageType() {
				return APP_PAGE;
			}

			public String question() {
				return "Are you sure you want to delete the <I>" + application.name() + "</I> Application?";
			}

		} );
	}

	/**
	 * Bounces an application.
	 */
	public WOComponent bounceClicked() {
		AppDetailPage page = AppDetailPage.create( context(), currentApplication );
		page = (AppDetailPage)page.bounceClicked();
		return page;
	}

	public WOComponent configureClicked() {
		AppConfigurePage aPage = (AppConfigurePage)AppConfigurePage.create( context(), currentApplication );
		aPage.isNewInstanceSectionVisible = true;
		return aPage;
	}

	public boolean logoutRequired() {
		if( siteConfig() == null ) {
			return false;
		}
		else {
			return (mySession().isLoggedIn() && siteConfig().isPasswordRequired());
		}
	}

	public WOComponent logoutClicked() {
		mySession().setIsLoggedIn( false );
		return pageWithName( "Main" );
	}

	public boolean currentApplicationIsNotRunning() {
		return !currentApplication.isRunning();
	}
}