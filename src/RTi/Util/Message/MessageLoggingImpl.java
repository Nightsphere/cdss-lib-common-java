/**
 *
 * Created on February 19, 2007, 10:50 AM
 *
 */

package RTi.Util.Message;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

/**
 *
 * @author iws
 */
public class MessageLoggingImpl extends MessageImpl {
    
    private Logger status = Logger.getLogger("RTi.Util.Message.status");
    private Logger warning = Logger.getLogger("RTi.Util.Message.warning");
    private Logger debug = Logger.getLogger("RTi.Util.Message.debug");
    
    public static void install() {
        System.setProperty("RTi.Util.MessageImpl",MessageLoggingImpl.class.getName());
    }
    
    public MessageLoggingImpl() {
        status.setLevel(Level.FINE);
        warning.setLevel(Level.ALL);
        debug.setLevel(Level.OFF);
    }
    
    protected void flushOutputFiles(int flag) {
        // do nothing
    }
    
    protected void printWarning(int l, String routine, Throwable e) {
        log(warning,l,routine,e);
    }
    
    protected void printDebug(int l, String routine, Throwable e) {
        log(debug,l,routine,e);
    }
    
    protected void printWarning(int l, String routine, String message, JFrame top_level) {
        printWarning(l,routine,message);
    }
    
    protected void printWarning(int l, String routine, String message) {
        log(warning,l,routine,message);
    }
    
    protected void printStatus(int l, String routine, String message) {
        log(status,l,routine,message);
    }
    
    protected void printWarning(int l, String tag, String routine, String message) {
        log(warning,l,routine,message);
    }
    
    protected void printDebug(int l, String routine, String message) {
        log(debug,l,routine,message);
    }
    
    private Level calcLevel(int level) {
        Level l;
        if (level <= 1) {
            l = Level.FINE;
        } else if (level <= 10) {
            l = Level.FINER;
        } else {
            l = Level.FINEST;
        }
        return l;
    }
    
    
    protected void initStreams() {
        
    }
    
    private void log(Logger logger,int level,String routine, String message) {
        Level l = calcLevel(level);
        if (logger.isLoggable(l)) {
            int idx = routine.indexOf('.');
            String clazz = null;
            String method = null;
            if (idx >= 0) {
                clazz = routine.substring(0,idx);
                method = routine.substring(idx +1 ,routine.length());
            } else {
                clazz = routine;
            }
            logger.logp(l,clazz,method,message);
        }
    }
    
    private void log(Logger logger,int level,String routine, Throwable e) {
        Level l = calcLevel(level);
        if (logger.isLoggable(l)) {
            int idx = routine.indexOf('.');
            String clazz = null;
            String method = null;
            if (idx >= 0) {
                clazz = routine.substring(0,idx);
                method = routine.substring(idx +1 ,routine.length());
            } else {
                clazz = routine;
            }
            logger.logp(l,clazz,method,e.getMessage(),e);
        }
    }

    
}