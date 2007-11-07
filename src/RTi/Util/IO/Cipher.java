// ----------------------------------------------------------------------------
// Cipher.java -- Manages encryption and decryption using various 
// 	cipher packages.
// ----------------------------------------------------------------------------
// Copyright:  See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History:
//
// 2002-08-06	J. Thomas Sapienza, RTi	Initial version
// 2002-08-07	JTS, RTi		Added javadoc, support for writing
//					persistent files
// 2002-11-11	JTS, RTI		Revised some commenting.
// 2003-01-09	JTS, RTi		Revisions to accomodate work with new
//					RTi licensing scheme.
// 2003-01-13	JTS, RTi		Javadoc work
// 2004-05-27	JTS, RTi		encrypt() now returns "Invalid Cipher"
//					if the two character cipher code is
//					not supported.
// 2005-04-26	JTS, RTi		Added finalize().
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
// ----------------------------------------------------------------------------

package RTi.Util.IO;

import Blowfish.BinConverter;
import Blowfish.BlowfishCBC;

import java.io.UnsupportedEncodingException;

/**
This class interacts with the actual encryption and decryption code.  RTi
applications call this code instead of calling the encryption and 
decryption classes directly.
<p>
The most likely reason that an RTi application would directly call Cipher
code would be for checking the validity of a password.  To do this, use
code similar to the following:
<pre>
   Cipher c = new Cipher (prefix);
   // where prefix is the two-character prefix that specifies the
   // cipher package and the encryption seeds to use

   if (c.validateString(unencryptedPassword, encryptedPassword)) {
      // the encrypted password was validated
   }
</pre>
*/
public class Cipher {

/**
The Blowfish cipher package.
*/
public static final int BLOWFISH = 0;

/**
the object that does the actual Blowfish encryption/decryption
*/
private BlowfishCBC __bfcbc;

/**
The cipher package being used to encrypt and decrypt data.
*/
private int __cipherPackage;

/**
The two-character cipher selection prefix being used in this instance
of Cipher.
*/
private String __prefix;

/**
Constructor.  Sets up a cipher object to encrypt and decrypt using the
Cipher Package specified with the two-character prefix.

@param prefix two-character code that specifies the cipher package and the
seeds and passwords to use.
*/
public Cipher(String prefix) {
	__prefix = prefix;

	// At this point in development (2003-01-21), saving the __cipherPackage
	// to check it later in encrypt() and decrypt() is unnecessary as
	// RTi is only using a single encryption type (Blowfish). In the future
	// there may be more, so the check is left in.
	__cipherPackage = RTiCryptoData.lookupCipherPackage(prefix);
}

/**
Decrypts a hex-value string.
<p>
With the Blowfish cipher, the length of the encrypted strings is not 
directly related to the length of the string from which it was encrypted.  
Blowfish operates on 8 bytes at a time, and so all of the strings encrypted
with Blowfish will have lengths that are multiples of 8.<p>
The exact length of the string generated by blowfish can be calculated 
by:<br>
&nbsp;&nbsp;- take the length of the original string and pad it out with 
    spaces until it is 8, 16, 24 ... etc characters long.  
    (thus, the string <pre>"Hello"</pre> will be padded to <pre>"Hello   "
    </pre>, and the string
    <pre>"Colorado State"</pre> will be padded to <pre>"Colorado State  "</pre>.
    The string <pre>"Colorado"</pre> will not be padded)<br>
&nbsp;&nbsp;- multiply the new, multiple-of-8 string's length by 2<br>
&nbsp;&nbsp;- add 2 to this number.<p>
So the encrypted string's length can be fairly closely configured through
trimming the input string to a desired length.
@param val a hex-value string representing the encrypted string.  This string
is a way of representing a byte array of values, and each element of the
byte array is represented by a 2-character string that is the hex value
for that element.  Thus, byte[]=[09][16][255] becomes String="090FFF".
<b>Note:</b>The first two characters of the string should be the prefix 
that tells which passwords and seeds to use for decryption.
@return a string decrypted from the hex-value string passed in
*/
public String decrypt(String val) {
	switch (__cipherPackage) {
		case BLOWFISH:
			if (val.length() < 2) {
				return null;
			}
			String prefix = val.substring(0, 2);
			val = val.substring(2, val.length());
			// to be used for holding the decrypted string
			byte[] messBuff;

			// if the CBC isn't rebuilt each time, it will continue
			// encryption based on the last 8-byte segment 
			// encrypted.  While putting the CBC back to start 
			// every time violates the purpose of using CBC 
			// encryption, CBC is still used because
			// it was easier to get working than ECB encryption
			rebuildBlowfishCBCIV(prefix);

			// this next section determines how large to make the 
			// byte array for holding the decrypted string.  This 
			// is an issue because the array needs to be properly 
			// sized or else the decrypted string could end up 
			// with extra characters (from memory) tacked on the 
			// end.  There is some redundancy here (two calls to 
			// binHexToBytes) so that the exact size can be 
			// determined.

			// first find out how long the encrypted string is
			int length = val.length();
		
			// create a temporary byte array to hold the 
			// equivalent bytes for this hex-value string
			byte[] temp = new byte[length];

			// decode the hex-value string into the byte array.  
			// The byte array can at this point be thrown away.  
			// The important part is the return value from 
			// BinConverter.binHexToBytes, which tells how many 
			// characters were processed.
			int size = BinConverter.binHexToBytes(val, temp, 
				0, 0, length);	
		
			// make a new byte array that holds exactly how 
			// many characters were processed in the call 
			// to binHexToBytes
			byte[] bval = new byte[size];

			// now process the hex-value string into this new 
			// byte array and have no extra space left over that 
			// can screw up the decryption.
			length = BinConverter.binHexToBytes(val, bval, 
				0, 0, size);
	
			// the next section is used to align the byte array 
			// representing the string to be decrypted to an 
			// 8-byte boundary.  Blowfish works by 
			// encrypting/decrypting 8-byte segments, so if the 
			// string to be decrypted was not an even multiple of 
			// 8 bytes, it will be padded out with zeroes to fill 
			// the extra bytes necessary for Blowfish.	
			int rest = length % 8;
			if (rest > 0) {
				// this is the section for padding out the 
				// byte array to 8 bytes	
				messBuff = new byte[length + (8 - rest)];
				System.arraycopy(bval, 0, messBuff, 0, length);
	
				for (int nI = length; nI < messBuff.length ; 
					nI++) {
					messBuff[nI] = 0x20;
				}
			} else {
				// this is if the byte array is already a 
				// multiple of 8  bytes	
				messBuff = new byte[length];
				System.arraycopy(bval, 0, messBuff, 0, length);
			}
			// end padding section
	
			// used for getting the current CBC Cipher seed 
			//byte[] showIV = new byte[BlowfishCBC.BLOCKSIZE];
			__bfcbc.setCBCIV(RTiCryptoData.getBlowfishCBCIV(
				prefix));

			// decrypt the message
			__bfcbc.decrypt(messBuff);

			// go through the byte array returned and turn each 
			// element into a character and append it to the string.
			String s = new String("");
			for (int i = 0; i < messBuff.length; i++) {
				s = s.concat("" + (char)messBuff[i]);
			}
			
			return s;
	}
	return val;
}

/**
Encrypts a string and returns the Hex-value string representing the
encryption.  
<p>
With the Blowfish cipher, the length of the encrypted strings is not 
directly related to the length of the string from which it was encrypted.  
Blowfish operates on 8 bytes at a time, and so all of the strings encrypted
with Blowfish will have lengths that are multiples of 8.<p>
The exact length of the string generated by blowfish can be calculated 
by:<br>
&nbsp;&nbsp;- take the length of the original string and pad it out with 
    spaces until it is 8, 16, 24 ... etc characters long.  
    (thus, the string <pre>"Hello"</pre> will be padded to <pre>"Hello   "
    </pre>, and the string
    <pre>"Colorado State"</pre> will be padded to <pre>"Colorado State  "</pre>.
    The string <pre>"Colorado"</pre> will not be padded)<br>
&nbsp;&nbsp;- multiply the new, multiple-of-8 string's length by 2<br>
&nbsp;&nbsp;- add 2 to this number.<p>
So the encrypted string's length can be fairly closely configured through
trimming the input string to a desired length. 
@param val the string to be encrypted
@return a hex-value string representing the encrypted string.  This string
is a way of representing a byte array of values, and each element of the
byte array is represented by a 2-character string that is the hex value
for that element.  Thus, byte[]=[09][16][255] becomes String="090FFF"
Returns "Invalid Cipher" if the 2 character cipher code isn't supported.
*/
public String encrypt(String val)
throws UnsupportedEncodingException {
	switch (__cipherPackage) {
		case BLOWFISH:
			// will be used for holding the encrypted byte array
			byte[] messBuff;

			// if the CBC isn't rebuilt each time, it will continue
			// encryption based on the last 8-byte segment 
			// encrypted.  While putting the CBC back to start 
			// every time violates the purpose of using CBC 
			// encryption, CBC is still used because
			// it was easier to get working than ECB encryption
			rebuildBlowfishCBCIV(__prefix);

			// determine the length of the string that will 
			// be encrypted
			int length = val.length();
			// turn the string into a byte array based on the 
			// eight-bit UCS Transformation Format
			byte[] bval = val.getBytes("UTF8");

			// the next section is used to align the byte array 
			// representing the string to be encrypted to an 
			// 8-byte boundary.  Blowfish works by 
			// encrypting/decrypting 8-byte segments, so if the 
			// string to be encrypted was not an even multiple 
			// of 8 bytes, it will be padded out with zeroes 
			// to fill the extra bytes necessary for Blowfish.
			int rest = length % 8;
			if (rest > 0) {
				// this is the section for padding out the 
				// byte array to 8 bytes
				messBuff = new byte[length + (8 - rest)];
				System.arraycopy(bval, 0, messBuff, 0, length);
		
				for (int nI = length; nI < messBuff.length ; 
					nI++) {
					messBuff[nI] = 0x20;
				}
			} else {
				// this is if the byte array is already a 
				// multiple of 8 bytes
				messBuff = new byte[length];
				System.arraycopy(bval, 0, messBuff, 0, length);
			}
			// end the padding section

			// used for getting the current CBC Cipher seed 
			byte[] showIV = new byte[BlowfishCBC.BLOCKSIZE];
			__bfcbc.getCBCIV(showIV);

			// finally encrypt the byte array (gets encrypted 
			// into itself)
			__bfcbc.encrypt(messBuff);

			// use the BinConverter class to change the 
			// byte array into the hex-string
			return (__prefix + 
				BinConverter.bytesToBinHex(messBuff));
	}
	return "Invalid Cipher";
}

/**
Cleans up member variables.
*/
public void finalize()
throws Throwable {
	__bfcbc = null;
	__prefix = null;
	super.finalize();
}

/**
Recreate the CBC cipher object with the password and starting key.
the cbc is reset so that encryption and decryption are not order-dependent.
*/
private void rebuildBlowfishCBCIV(String prefix) {
	__bfcbc = new BlowfishCBC(RTiCryptoData.getBlowfishPassword(prefix),
		RTiCryptoData.getBlowfishCBCIV(prefix));
}

/**
Checks that an encrypted String and nonencrypted String are the same.  It
does this by encrypting the nonencrypted String and seeing if the result is
equal to the encrypted String.
@param u the unencrypted string to check.
@param e the encrypted string to check.
@return true if the two match, false if not.
*/
public boolean validateString(String u, String e)
throws Exception {
	String r = encrypt(u);

	if (r.equals(e)) {
		return true;
	}
	return false;
}

}
