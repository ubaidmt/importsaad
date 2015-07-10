package com.ibm.ecm.extension.util.cipher;

import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

public class CipherUtil {
	
	private final static String algorithm = "AES";
	private static Cipher ecipher;
	private static Cipher dcipher;
	
	public static SecretKey generateKey() throws NoSuchAlgorithmException {
		return KeyGenerator.getInstance(algorithm).generateKey();
	}
	
	public static String getStringKey(SecretKey secretKey) {
		return Base64.encodeBase64String(secretKey.getEncoded());
	}

	public static SecretKey getSecretKey(String stringKey) {
		byte[] encodedKey = Base64.decodeBase64(stringKey);
		SecretKey originalKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, algorithm);
		return originalKey;
	}	
	
	public static String encrypt(String str, SecretKey key) {
		try 
		{
			ecipher = Cipher.getInstance(algorithm);
			ecipher.init(Cipher.ENCRYPT_MODE, key);
			
			byte[] utf8 = str.getBytes("UTF8");
			byte[] enc = ecipher.doFinal(utf8);
			enc = Base64.encodeBase64(enc);
			return new String(enc);	
		}
		catch (Exception e) {}
		return null;
	}
	
	public static String decrypt(String str, SecretKey key) {
		try
		{
			dcipher = Cipher.getInstance(algorithm);
			dcipher.init(Cipher.DECRYPT_MODE, key);
			
			byte[] dec = Base64.decodeBase64(str.getBytes());
			byte[] utf8 = dcipher.doFinal(dec);
			return new String(utf8, "UTF8");
		}
		catch (Exception e) {}
		return null;
	}	

}
