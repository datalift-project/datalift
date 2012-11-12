/////////////////////////////////////////////////////////
//  Bare Bones Browser Launch                          //
//  Version 3.1 (June 6, 2010)                         //
//  By Dem Pilafian                                    //
//  Supports:                                          //
//     Mac OS X, GNU/Linux, Unix, Windows XP/Vista/7   //
//  Example Usage:                                     //
//     String url = "http://www.centerkey.com/";       //
//     BareBonesBrowserLaunch.openUrl(url);            //
//  Public Domain Software -- Free to Use as You Like  //
/////////////////////////////////////////////////////////

package com.centerkey.utils;


import java.io.IOException;
import java.util.Arrays;


/**
 * Bare Bones Browser Launch: a platform-independent mechanism to
 * launch the user's default web browser.
 * <p>
 * This solution is appropriate when a compact lightweight method
 * to open a web page is needed.  Bare Bones is free and works on
 * Mac OS X, GNU/Linux, Unix (Solaris) and Windows XP/Vista/7.</p>
 * <p>
 * Example Usage:</p>
 * <blockquote><pre>
 *   String url = "http://www.centerkey.com/";
 *   BareBonesBrowserLaunch.openUrl(url);
 * </pre></blockquote>
 * <p>
 * Public Domain Software. Free to Use as You Like. </p>
 *
 * @author  Dem Pilafian
 * @version 3.1 (June 6, 2010)
 * @see     <a href="http://www.centerkey.com/java/browser/">Bare
 *          Bones Browser Launch</a>
 */
public class BareBonesBrowserLaunch
{
    private static final String[] BROWSERS = {
        "firefox", "google-chrome", "opera", "epiphany",
        "konqueror", "conkeror", "midori", "kazehakase", "mozilla" };

    private static final String ERROR_MSG = "Failed to launch web browser: ";

    public static void openUrl(String url) throws IOException {
        String osName = System.getProperty("os.name");
        try {
            if (osName.startsWith("Mac OS")) {
                // Mac OS X
                Class.forName("com.apple.eio.FileManager")
                     .getDeclaredMethod("openURL", new Class[] {String.class})
                     .invoke(null, new Object[] {url});
            }
            else if (osName.startsWith("Windows")) {
                // Windows (XP/Vista/7...)
                Runtime.getRuntime().exec(
                                "rundll32 url.dll,FileProtocolHandler " + url);
            }
            else {
                // Assume Unix or Linux
                String browser = null;
                for (String b : BROWSERS) {
                    String[] cmd = new String[] { "which", b };
                    if (Runtime.getRuntime().exec(cmd)
                                            .getInputStream().read() != -1) {
                        browser = b;
                        Runtime.getRuntime().exec(new String[] { b, url });
                        break;
                    }
                }
                if (browser == null) {
                    throw new Exception("No web browser found (of: "
                                        + Arrays.toString(BROWSERS) + ")");
                }
            }
        }
        catch (Exception e) {
            throw new IOException(ERROR_MSG + e.getLocalizedMessage(), e);
        }
    }
}

