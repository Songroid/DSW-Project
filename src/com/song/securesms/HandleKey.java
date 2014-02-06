package com.song.securesms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;

/**
 * Helper class for assistance method
 */
public class HandleKey {

    private Context context;
    private int carrier;
    private int slots;

    public HandleKey(Context context, Integer carrier) {
        this.context = context;
        this.carrier = carrier;
    }

    public HashMap<String,KeySpec> rsaKeyGen() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, NoSuchProviderException{
		/* 
		 * create an instance of KeyPairGenerator suitable for generating RSA keys
    	 * initialize the generator, telling it the bit length of the modulus
    	 * returns a KeyPair object
    	 * pull out the public and private keys
    	 */
    	KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");

        /*
         * use different key for each carrier
         */
        switch (carrier){
            case 1:
            case 2:
            case 3:
                kpg.initialize(512);
                break;
        }
    	KeyPair kp = kpg.genKeyPair();
    	HashMap<String,KeySpec> pubpri = new HashMap<String,KeySpec>();
    	
    	/* 
    	 * translate between Keys and their corresponding specification 
    	 * save them in the root dir
    	 */
    	KeyFactory fact = KeyFactory.getInstance("RSA");
    	RSAPublicKeySpec pub = fact.getKeySpec(kp.getPublic(),
    			  RSAPublicKeySpec.class);
    	RSAPrivateKeySpec priv = fact.getKeySpec(kp.getPrivate(),
    			  RSAPrivateKeySpec.class);
    	this.saveToFile("public.key", pub.getModulus(),
    			  pub.getPublicExponent());
    	/*this.saveToFile("private.key", priv.getModulus(),
    			  priv.getPrivateExponent());*/
    	
    	pubpri.put("private", priv);
    	pubpri.put("public", pub);
    	
    	return pubpri;
	}

    // general file saving function
	public void saveToFile(String fileName, BigInteger mod, BigInteger exp) throws IOException {
	  	File file = new File(context.getFilesDir(), fileName);
	  	ObjectOutputStream oout = new ObjectOutputStream(
	  		    new BufferedOutputStream(new FileOutputStream(file)));
	  	try {
	  		// change 
	  		oout.writeObject(mod);
	  	    oout.writeObject(exp);
	  	} catch (Exception e) {
	  		
	  	} finally {
	  		oout.close();
	  	}
	}
	
    public PrivateKey readAESFromFile(String keyFileName, String passphrase) throws IOException,BadPaddingException {
    	String priEncryMod = "";
    	String priEncryExp = "";
    	byte[] primod;
    	byte[] priexp;
    	// fixed path in the phone
        File file = new File(context.getFilesDir(), keyFileName);
    	// read AES key from storage
    	FileInputStream in = new FileInputStream(file);
    	ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
    	try {
    		// AES HexString
    		priEncryMod = (String) oin.readObject();
    		priEncryExp = (String) oin.readObject();
    		byte[] bytepass = passphrase.getBytes("UTF-8");
    		Cipher c = Cipher.getInstance("AES");
    		SecretKeySpec keyAESspec = new SecretKeySpec(bytepass, "AES");
    		c.init(Cipher.DECRYPT_MODE, keyAESspec);
    		// passphrase + AES HexString --(decryption)-> private key (byte[])
    		primod = c.doFinal(hexToBytes(priEncryMod));
    		priexp = c.doFinal(hexToBytes(priEncryExp));
    		
    		// read separate components, mod and exp
    		BigInteger m = new BigInteger(new String(primod));
    		BigInteger e = new BigInteger(new String(priexp));
    		RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(m, e);
    		KeyFactory fact = KeyFactory.getInstance("RSA");
    		PrivateKey priKey = fact.generatePrivate(keySpec);
    		return priKey;
    	} catch (NoSuchAlgorithmException e1) {
    		e1.printStackTrace();
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (InvalidKeyException e1) {
			e1.printStackTrace();
		} catch (NoSuchPaddingException e1) {
			e1.printStackTrace();
		} catch (IllegalBlockSizeException e1) {
			e1.printStackTrace();
		} catch (InvalidKeySpecException e1) {
			e1.printStackTrace();
		} finally {
    		oin.close();
    	}
		return null;
    }
    
    public byte[] rsaDecrypt(byte[] data,String passphrase, Integer carrier) throws IOException, NoSuchAlgorithmException,
    				NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
    	String messageBody = asHex(data);
    	String result = "";

    	int index=0;
    	PrivateKey priKey = readAESFromFile("aesTransPriv.key",passphrase);
    	Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    	cipher.init(Cipher.DECRYPT_MODE, priKey);

        switch (carrier){
            /** VZW & ATT**/
            case 1:
            case 2:
                slots = (int)Math.ceil(((double)messageBody.length())/128);
                String fullVZWMsgArray[] = new String[slots];
                for(int i=0;i<slots;i++){
                    fullVZWMsgArray[i]=messageBody.substring(index,index+128);
                    index+=128;
                    result+=asHex(cipher.doFinal(hexToBytes(fullVZWMsgArray[i])));
                }
                break;
        }
        byte[] cipherData = hexToBytes(result);
		
		return cipherData;
    }
    
    public byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];
        for(int i=0;i<len;i+=2)
        {
            data[i/2]=(byte) ((Character.digit(s.charAt(i),16)<<4)+Character.digit(s.charAt(i+1),16));
        }
        return data;
    }
    
    // byte[] to String
  	public String asHex (byte buff[]) {
          StringBuffer strbuff = new StringBuffer(buff.length * 2);
          for (int i = 0; i < buff.length; i++)
          {
              if (((int) buff[i] & 0xff) < 0x10)
                  strbuff.append("0");
              strbuff.append(Long.toString((int) buff[i] & 0xff, 16));
          }
              return strbuff.toString();
      }

    // add space after "(num)." in EW and FU message
    // regex: \\d+\\.
    public String addSpaceInEWorFU(String s){
        return s.replaceAll("\\d+\\.", "$0 ");
    }
    
}
