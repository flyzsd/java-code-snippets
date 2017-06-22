package sandbox.java.security.aes;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/*
 * By default Java only support AES with 128 bits key length. You need to install Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files in order to support 256 bits.
 * 
 */
public class AESDemo {

	public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
		KeyGenerator KeyGen = KeyGenerator.getInstance("AES");
		KeyGen.init(256);
		SecretKey secretKey = KeyGen.generateKey();
		System.out.println(secretKey.getAlgorithm());
		SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(),"AES");
		System.out.println(secret.getAlgorithm());
		
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        
        byte[] byteText = "Hello World!".getBytes(StandardCharsets.UTF_8);
        cipher.init(Cipher.ENCRYPT_MODE, secret, ivParameterSpec);
        byte[] byteCipherText = cipher.doFinal(byteText);

        cipher.init(Cipher.DECRYPT_MODE, secret, ivParameterSpec);
        byte[] bytePlainText = cipher.doFinal(byteCipherText);
        System.out.println(new String(bytePlainText, StandardCharsets.UTF_8));
	}

}