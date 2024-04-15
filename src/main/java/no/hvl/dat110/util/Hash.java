package no.hvl.dat110.util;

/**
 * exercise/demo purpose in dat110
 * @author tdoy
 *
 */

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash { 
	
	
	public static BigInteger hashOf(String entity) {

		BigInteger hashInt = null;

		try {
			// Create MD5 digest instance
			MessageDigest md = MessageDigest.getInstance("MD5");

			// Compute hash of the input 'entity'
			byte[] hashBytes = md.digest(entity.getBytes());

			// Convert hash into hex format
			StringBuilder hexString = new StringBuilder();
			for (byte b : hashBytes) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}

			// Convert hex into BigInteger
			hashInt = new BigInteger(hexString.toString(), 16);

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return hashInt;
	}

	
	public static BigInteger addressSize() {

		int bitSize = bitSize();

		return BigInteger.valueOf(2).pow(bitSize);
	}
	
	public static int bitSize() {
		
		int digestlen = 16;
		

		
		return digestlen*8;
	}
	
	public static String toHex(byte[] digest) {
		StringBuilder strbuilder = new StringBuilder();
		for(byte b : digest) {
			strbuilder.append(String.format("%02x", b&0xff));
		}
		return strbuilder.toString();
	}

}
